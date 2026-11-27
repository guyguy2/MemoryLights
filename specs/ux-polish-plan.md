# Memory Lights — UX, Visual Craft & Automated Guard Plan

**Source:** `android-ux-audit` against `android-ux-design` standards (2026-11-27)
**Status:** Planned (not started — execute on a clean feature branch off `main`, e.g. `ux/wave-20-craft-and-guards`)
**Verification baseline:** `local-ci.sh --app memorylights` + `local-emulator-test.sh --app memorylights` on `Pixel_10` (`emulator-5558`)

---

## Wave A — P0 Accessibility & Contrast Fixes (Ship First)

1. **Fix Total Score purple WCAG failure (`2.28:1` Dark / `2.67:1` AMOLED)**
   - **File:** `app/src/main/java/com/happypuppy/memorylights/ui/screens/StatisticsScreen.kt` (lines 112–117)
   - **Problem:** `Color(0xFF9C27B0)` on `surfaceContainerHigh` (`#2B2930` Dark / `#1D1D1D` AMOLED) fails both WCAG `4.5:1` text and `3.0:1` non-text minimums.
   - **Fix:** Move stat accent colors into a `@Immutable` `GameAccentColors` palette in `ui/theme/` (provided via `LocalGameAccentColors`) and lighten Total Score purple (e.g., `MaterialTheme.colorScheme.primary` `#D0BCFF` or `#CE93D8`, `>= 6.5:1`).
2. **Replace `Color.Gray` (`#888888`) with `MaterialTheme.colorScheme.onSurfaceVariant`**
   - **Files:** `SettingsScreen.kt` (lines 202, 269, 301, 473, 479, 536–538, 553, 607, 626), `StatisticsScreen.kt` (lines 165, 318, 326, 347–348), plus any remaining `Color.Gray` usages across `ui/screens/` and `ui/components/`.
   - **Problem:** `#888888` on Dark `surfaceContainerHigh` (`#2B2930`) is `4.05:1` (fails WCAG `4.5:1` body text).
   - **Fix:** Use `MaterialTheme.colorScheme.onSurfaceVariant` (`#CAC4D0`, **`8.42:1`**).
3. **Move hardcoded TalkBack achievement strings to `res/values/strings.xml`**
   - **File:** `StatisticsScreen.kt` (lines 355–359)
   - **Problem:** `"$title unlocked. $description"` and `"$title locked. $description"` are hardcoded English literals.
   - **Fix:** Add `achievement_cd_unlocked` and `achievement_cd_locked` format resources in `res/values/strings.xml`.
4. **Remove nested clickable trap on Reset Statistics card**
   - **File:** `StatisticsScreen.kt` (lines 138–178)
   - **Problem:** Outer `SettingsCard(onClick = ...)` wraps an inner clickable `TextButton`, creating duplicate TalkBack stops and accidental trigger risk (already fixed on `SettingsScreen.kt:177`).
   - **Fix:** Remove `onClick` from the outer `SettingsCard`; let only the inner `Reset` `TextButton` trigger the dialog.

---

## Wave B — P1 Visual Craft, Typography, Motion & Responsiveness

5. **Enable tabular numerals (`fontFeatureSettings = "tnum"`) on live timers and scores**
   - **Files:** `TopHudStrip.kt` (lines 130, 440, 462, 484, 518) and `CenterDiscContent.kt` (lines 140, 162, 219).
   - **Fix:** Apply `.copy(fontFeatureSettings = "tnum")` to all ticking `M:SS.s` Blitz timers and round/score digits so digits never jitter horizontally.
6. **Respect system Reduced Motion (`ANIMATOR_DURATION_SCALE == 0`) in `ParticleEffect`**
   - **File:** `app/src/main/java/com/happypuppy/memorylights/ui/components/ParticleEffect.kt` (lines 47–76).
   - **Fix:** Read `Settings.Global.ANIMATOR_DURATION_SCALE`; when `0f`, skip particle rendering/frame loop and invoke `onComplete?.invoke()` immediately.
7. **Fix AMOLED dialog tonal separation**
   - **Files:** `SettingsScreen.kt` (lines 359, 427) and `StatisticsScreen.kt` (line 226).
   - **Fix:** Replace `containerColor = MaterialTheme.colorScheme.surface` (`#000000` in AMOLED) with `MaterialTheme.colorScheme.surfaceContainerHigh` (or M3 default) so dialogs clearly float above the black canvas.
8. **Preserve `StatisticsScreen` dialog state across process death/recreation**
   - **File:** `StatisticsScreen.kt` (line 40).
   - **Fix:** Change `var showResetDialog by remember { mutableStateOf(false) }` to `rememberSaveable`.
9. **Cap Settings / Statistics / sub-screen content width on tablets & wide landscape**
   - **Files:** `SettingsScreen.kt` (lines 96–101), `StatisticsScreen.kt` (lines 63–68), `GameModesScreen.kt`, `GameplayScreen.kt`, `SoundAndHapticsScreen.kt`.
   - **Fix:** Wrap scrollable `Column`s in a centered container with `Modifier.widthIn(max = 840.dp)` (matching `SimpleDayCounter` / `android-ux-design`).

---

## Wave C — Automated UX & Accessibility Guard Tests (Permanent CI Gates)

10. **Add JVM WCAG Contrast & Brand Color Scheme tests (`app/src/test/.../ui/theme/`)**
    - `BrandColorSchemeTest` pinning every M3 role of `AmoledColorScheme` and `DarkMaterialColorScheme` (including monotonic dark surface container luminance).
    - `ThemeContrastTest` asserting `>= 4.5:1` for all text/label/button pairs and `>= 3.0:1` for large text, icons, borders, and stat accents across both AMOLED and Dark schemes.
11. **Add `StringLiteralGuardTest` (`app/src/test/.../ui/StringLiteralGuardTest.kt`)**
    - Scan `app/src/main/java` for hardcoded user-facing literals in `Text(...)`, `contentDescription = "..."`, etc. (matching `SimpleDayCounter` and `SpeechTimer_Jetpack`).
12. **Add Compose Multipreviews (`@PreviewLightDark` / `@PreviewFontScale` / `@PreviewScreenSizes`)**
    - Add realistic private previews for `SimonGameScreen` (4-tile & 6-tile), `SettingsScreen`, `StatisticsScreen`, and key dialogs so `android preview` works locally.
13. **Add Compose 1.8+ ATF Accessibility Tests (`app/src/androidTest/.../ScreenAccessibilityTest.kt`)**
    - Add `androidx.compose.ui:ui-test-junit4-accessibility`, call `rule.enableAccessibilityChecks()`, and verify `onAllNodes(isRoot()).tryPerformAccessibilityChecks()` across Game, Settings, Game Modes, Gameplay, Sound & Haptics, Statistics (empty & populated), and Reset/About dialogs on `Pixel_10` (`emulator-5558`).

---

## Wave D — P2 Polish & Existing Tracked Debt

14. **Eliminate remaining hardcoded `Color.White` / `Color(0x...)` literals and `9.sp` overrides**
    - Migrate `Color.White` → `MaterialTheme.colorScheme.onSurface` and centralize game/stat accents in `ui/theme/Color.kt`; replace `9.sp` micro-overrides in `CenterDiscContent.kt` (lines 114, 154, 172) with `TextAutoSize.StepBased` or dedicated type tokens.
15. **Clean up deprecated `themes.xml` bar attributes & `#70` `announceForAccessibility`**
    - Remove deprecated `android:statusBarColor` / `android:navigationBarColor` in `res/values/themes.xml` (lines 11–12) since `enableEdgeToEdge()` handles system bars; consider `core-splashscreen`.
    - Replace deprecated `view.announceForAccessibility(...)` calls in `SimonGameScreen.kt` (lines 222–246, tracked as `#70`) with `LiveRegionMode.Polite` semantics.
