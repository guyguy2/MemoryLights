package com.happypuppy.memorylights.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guard against hardcoded user-facing text, raw `Color.Gray` / `Color.DarkGray`
 * literals, and raw `.sp` font-size overrides creeping back into UI code.
 *
 * UI copy belongs in `res/values/strings.xml`, colors belong in
 * `MaterialTheme.colorScheme` / `LocalGameAccentColors`, and typography belongs
 * in `MaterialTheme.typography`.
 */
class StringLiteralGuardTest {

    @Test
    fun `no user-facing string literals in app main sources`() {
        val root = File("src/main/java")
        assertTrue("run from the app module directory, missing ${root.absolutePath}", root.isDirectory)

        val violations = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file -> findUserFacingLiterals(file.readText()).map { "${file.path}: $it" } }
            .toList()

        assertEquals(
            "Move these to res/values/strings.xml (or extend the allow-list with a reason):\n" +
                violations.joinToString("\n"),
            emptyList<String>(),
            violations
        )
    }

    @Test
    fun `no Color_Gray or raw fontSize sp overrides outside ui theme`() {
        val root = File("src/main/java/com/happypuppy/memorylights/ui")
        assertTrue("missing ${root.absolutePath}", root.isDirectory)

        val forbidden = listOf(
            Regex("""\bColor\.(Gray|DarkGray|LightGray)\b"""),
            Regex("""\bfontSize\s*=\s*\d+(?:\.\d+)?\.sp\b"""),
        )
        val hits = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && "/ui/theme/" !in it.path.replace('\\', '/') }
            .flatMap { file ->
                val text = file.readText()
                forbidden.flatMap { rx -> rx.findAll(text).map { "${file.path}: ${it.value}" } }
            }
            .toList()

        assertEquals(
            "Use MaterialTheme color/typography tokens instead of raw literals:\n" + hits.joinToString("\n"),
            emptyList<String>(),
            hits
        )
    }

    @Test
    fun `detector flags text and content description literals`() {
        val source = """
            Text("Hello")
            Text(
                "Wrapped"
            )
            Text(text = "Named")
            Icon(Icons.Default.Add, contentDescription = "Add")
            DropdownMenuItem(text = { Text("Item") })
            Modifier.semantics { onClick(label = "Do it") { true } }
            CustomAccessibilityAction("Delete") { true }
            Surface(title = "T")
            Snackbar(actionLabel = "Undo")
            label = "Tooling name"
        """.trimIndent()

        val found = findUserFacingLiterals(source)

        assertEquals(
            listOf("Hello", "Wrapped", "Named", "Add", "Item", "Do it", "Delete", "T", "Undo", "Tooling name"),
            found
        )
    }

    @Test
    fun `detector flags literals in if else expressions used as text`() {
        val source = """
            Text(if (isEmpty) "First" else "Second")
            Text(
                text = if (filtered)
                    "Third"
                else stringResource(R.string.ok)
            )
            Text(text = if (filtered) stringResource(R.string.ok) else "Fourth")
            Text(text = if (filtered) stringResource(R.string.ok) else stringResource(R.string.cancel))
        """.trimIndent()

        assertEquals(listOf("First", "Second", "Third", "Fourth"), findUserFacingLiterals(source))
    }

    @Test
    fun `detector ignores resources, symbols and the allow-list`() {
        val source = """
            Text(stringResource(R.string.app_name))
            Text("+")
            Text("")
            contentDescription = null
            label = "ctaPulse"
            Log.d("Tag", "message with words")
            val key = "Personal"
        """.trimIndent()

        assertEquals(emptyList<String>(), findUserFacingLiterals(source))
    }

    private companion object {
        /** Compose animation labels (debug tooling names, never displayed). */
        val ALLOWED_LITERALS = setOf(
            "ctaPulse",
            "ctaScalePulse",
            "hud_round_animation",
            "level_animation",
            "discRingReactiveColor",
            "discRingGlowAlpha",
            "pressAnimation",
            "litProgress",
        )

        // A literal counts only if it contains a letter: "+" or "" are symbols, not copy.
        const val LITERAL = "\"([^\"\\n]*[A-Za-z][^\"\\n]*)\""
        const val TEXT_SINK =
            """(?:\bText\(|\b(?:text|title|description|placeholder|message|actionLabel|onClickLabel|contentDescription|stateDescription|label|supportingText|hint)\s*=)"""
        val PATTERNS = listOf(
            Regex("""\bText\(\s*$LITERAL"""),
            Regex("""\b(?:text|title|description|placeholder|message|actionLabel|onClickLabel|contentDescription|stateDescription|label|supportingText|hint)\s*=\s*$LITERAL"""),
            Regex("""\bonClick\(\s*label\s*=\s*$LITERAL"""),
            Regex("""\bCustomAccessibilityAction\(\s*$LITERAL"""),
            Regex("""$TEXT_SINK\s*if\s*\([^"{}]{0,200}\)\s*$LITERAL"""),
            Regex("""$TEXT_SINK\s*if\b[^{}]{0,300}?\belse\s+$LITERAL"""),
        )

        fun findUserFacingLiterals(source: String): List<String> =
            PATTERNS.flatMap { pattern -> pattern.findAll(source).map { it.groups[1]!!.range.first to it.groupValues[1] } }
                .distinct()
                .sortedBy { it.first }
                .map { it.second }
                .filterNot { it in ALLOWED_LITERALS }
    }
}
