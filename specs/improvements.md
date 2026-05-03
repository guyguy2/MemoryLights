# Memory Lights - Codebase Improvement Report

This is the canonical audit doc. It supersedes the original December 2025 best-practices write-up; closed items from that pass are listed in `specs/progress.md`. The current ranked execution roadmap (with phases and decisions) lives at `~/.claude/plans/use-as-needed-polymorphic-sunrise.md`.

---

## 1. Project Overview

Memory Lights is a single-activity Android game that recreates the classic Simon Says
color-sequence memory challenge. Players watch a growing sequence of colored button
flashes and must repeat it correctly before a 10-second timeout. The app ships two
modes: a classic 4-button layout and a harder 6-button "Memory Lights+" variant, with
four real sound packs (Standard, Funny, Electronic, Retro) and three placeholder packs
(Musical, Nature, Sci-Fi). The tech stack is Kotlin 2.1.10 + Jetpack Compose BOM
2025.06.00 + Material 3 + Koin 4.0.3 + DataStore Preferences. All persistence is
handled by DataStore (migrated from SharedPreferences). There is a single Gradle module
(`:app`); no KMP, no Room, no network layer.

---

## 2. Architecture & Module Structure

### Observations

The project uses a reasonable MVVM layering: `domain` (models, enums, constants),
`data` (managers, repository), `di`, and `ui` (screens, components, theme, viewmodels).
For an app of this scale that structure is fine.

### Issues

**Redundant dual-state machine.** Both `GameState` and `ScreenState` exist in
`SimonGameUiState`, but `GameState.Settings` overlaps with `ScreenState.Settings`.
`showSettings()` sets both `gameState = GameState.Settings` and
`screenState = ScreenState.Settings`, while `showStatistics()` sets
`gameState = GameState.Settings` and `screenState = ScreenState.Statistics`. This means
`GameState.Settings` has no meaning on its own; it is an overloaded placeholder for
"any non-game screen." The navigation `when` branch in `MemoryLightsGame` only checks
`screenState`, making `GameState.Settings` dead for navigation. The back-press handler
in `MainActivity` checks `gameState is GameState.Settings` which will accidentally
trigger when viewing statistics too.

- `app/src/main/java/.../domain/model/SimonGameState.kt` - `GameState.Settings` object
- `app/src/main/java/.../ui/MainActivity.kt:55` - back-press check
- `app/src/main/java/.../ui/viewmodels/SimonGameViewModel.kt:195-199` - `showStatistics` sets `GameState.Settings`

**Suggestion:** Remove `GameState.Settings` entirely. Drive navigation solely from
`screenState`. The ViewModel can keep a `previousScreenState: ScreenState` for the
"return from settings" logic.

**`StatisticsManager` is misnamed and misplaced.** Despite being called a "manager" and
living in `data/manager/`, it is really a repository. It owns its own `CoroutineScope`,
talks directly to DataStore, and exposes synchronous-style methods backed by
`runBlocking`. It should be renamed `StatisticsRepository` and placed alongside
`DataStoreSettingsRepository` in `data/repository/`.

**`rootProject.name = "My Application"` in `settings.gradle.kts:22`.** This is the
default placeholder name, never updated. It only affects IDE project display and some
build artifact naming.

**No convention plugins or build-logic module.** Acceptable for a single-module project.
Adding one is not warranted until a second module is introduced.

---

## 3. Dependency Injection

### Observations

Koin 4.0.3 is used cleanly with `viewModelOf`, constructor injection, and a properly
typed interface binding for `SettingsRepository`. Module startup in `SimonApp` is
minimal and correct.

### Issues

**`SettingsScreen` directly injects `SimonSoundManager` from Koin via
`koinInject<SimonSoundManager>()` at `SettingsScreen.kt:61`.** This bypasses the
ViewModel and creates a hidden coupling: the composable now depends on the DI container.
The sound preview on sound-pack selection (`soundManager.playSound(SimonButton.GREEN)`)
should be an event sent to the ViewModel, not a direct manager call from the UI. This
also means the Settings composable cannot be previewed without a live Koin context.

```kotlin
// BEFORE (SettingsScreen.kt:61, 151)
val soundManager = koinInject<SimonSoundManager>()
// ...
onSelect = {
    soundManager.setSoundPack(soundPack)
    soundManager.playSound(SimonButton.GREEN)
    onSoundPackSelected(soundPack)
}

// AFTER - add a single callback and handle it in ViewModel
onSoundPackSelected = { pack ->
    viewModel.setSoundPackAndPreview(pack) // ViewModel plays the preview sound internally
}
```

**`SimonSoundManager` is injected as `single` but holds a `Context` reference** and
registers `ComponentCallbacks2`. This is fine as long as `applicationContext` is always
used (and it is), but the `getContext()` method at `SimonSoundManager.kt:193` returns
`context.applicationContext`, which is only called from commented-out or non-existent
callers and should be removed.

---

## 4. Presentation Layer (Compose + ViewModels)

### Observations

`collectAsStateWithLifecycle` is used correctly. `SimonPanel` applies semantics
(`contentDescription`, `stateDescription`, `Role.Button`). Immutable state is
consistently used via `StateFlow` + `it.copy(...)`.

### Issues

**`SimonGameViewModel` implements `DefaultLifecycleObserver` and is cast to it in
`MainActivity`.** This tightly couples the ViewModel to the Activity lifecycle via a
`DefaultLifecycleObserver` cast rather than the standard `LifecycleEventObserver` or a
`ProcessLifecycleOwner` approach. The cast `viewModel as DefaultLifecycleObserver` is
unsafe: if the ViewModel ever stops implementing that interface, this will throw a
`ClassCastException` at runtime.

- `app/src/main/java/.../ui/MainActivity.kt:40`

```kotlin
// BEFORE
lifecycle.addObserver(viewModel as DefaultLifecycleObserver)

// AFTER - check before cast, or model the lifecycle concern differently
lifecycle.addObserver(viewModel)  // safe because ViewModel implements the interface
// ...or use ProcessLifecycleOwner to avoid any Activity dependency
```

**`SimonGameScreen` has local `localPressedButtons` state that duplicates
`activeButtonPresses` in `SimonGameUiState`.** The component tracks pressed buttons
both in ViewModel state (`activeButtonPresses`) and in a local `remember` map
(`localPressedButtons`). Only the local copy drives the `SimonPanel.userPressed`
parameter; the ViewModel copy is never read by the UI. This is dead ViewModel state.
Either remove `activeButtonPresses` from `SimonGameUiState` (and keep local state for
purely cosmetic press feedback) or remove the local state and use `uiState.activeButtonPresses`.

- `app/src/main/java/.../ui/screens/SimonGameScreen.kt:110-113` (local state)
- `app/src/main/java/.../domain/model/SimonGameUiState.kt:30` (`activeButtonPresses`)

**`SimonGameScreen` is 750+ lines.** The 4-button vs 6-button grid layout (portrait and
landscape) is copy-pasted four times with near-identical `SimonPanel` instantiation.
Extract a `SimonButtonGrid` composable that accepts `List<List<SimonButton>>` rows and
the event callbacks.

**Hardcoded `.offset(y = ...)` in dp for text overlays is fragile.**

```kotlin
// SimonGameScreen.kt:683
modifier = Modifier.offset(y = if (uiState.memoryLightsPlusEnabled) (-200).dp else (-80).dp)
```

This will break on different screen densities or when the layout changes. Use
`Alignment` or measure the layout rather than raw dp offsets.

**`MemoryLightsGame` passes the raw `SimonGameViewModel` to child composables.** Passing
the ViewModel directly couples composables to the ViewModel type. Pass lambdas or a
stable state object instead, which enables better testability and preview support.

**Dynamic color is enabled by default (`dynamicColor = true`) in `Theme.kt:40`.** On
Android 12+ the app's purple `MaterialTheme.colorScheme.primary` could be overridden by
the user's wallpaper color, potentially breaking the dark game aesthetic. Consider
disabling dynamic color for a game that has a deliberate all-black theme.

- `app/src/main/java/.../ui/theme/Theme.kt:40`

**4-button mode has inconsistent padding.** Green uses `padding(2.dp)`, while Red,
Yellow and Blue use `padding(4.dp)`.

- `app/src/main/java/.../ui/screens/SimonGameScreen.kt:366, 382, 398, 414`

---

## 5. Data Layer

### Observations

`SettingsRepository` interface is clean and decoupled. DataStore migration from
SharedPreferences is in place for both settings and statistics.

### Issues

**`runBlocking` on every read in `DataStoreSettingsRepository` and
`StatisticsManager`.**

Both `getPreferences()` implementations use `runBlocking { dataStore.data.first() }`:

- `app/src/main/java/.../data/repository/SettingsRepository.kt:76`
- `app/src/main/java/.../data/manager/StatisticsManager.kt:42`

`runBlocking` on the main thread will block the UI thread until the disk read
completes. In the `init {}` block of the ViewModel, `loadSettings()` calls six separate
`runBlocking` reads. This can add noticeable jank on first launch. The correct pattern
is to expose suspend functions or `Flow` and collect from a coroutine scope.

```kotlin
// BEFORE (SettingsRepository.kt)
override fun getSoundPack(): SoundPack {
    val name = getPreferences()[KEY_SOUND_PACK]  // blocks main thread
    ...
}

// AFTER - expose Flow, let ViewModel collect once on init
fun settingsFlow(): Flow<AppSettings> = dataStore.data.map { prefs ->
    AppSettings(
        soundPack = SoundPack.valueOf(prefs[KEY_SOUND_PACK] ?: SoundPack.STANDARD.name),
        ...
    )
}
```

**High scores are stored twice** - once in `SettingsRepository` (as `high_score_4_button`
/ `high_score_6_button`) and once independently inside `StatisticsManager` (as
`high_score`). The two are updated separately and can diverge. `resetHighScore()` in the
ViewModel calls `statisticsManager.resetStatistics()` but then manually zeroes the
`highScore4Button`/`highScore6Button` fields in state and calls `saveSettings()`, while
`StatisticsManager.highScore` is also cleared. On the other hand, `recordGameResult()`
in `StatisticsManager` updates its own `KEY_HIGH_SCORE` which is separate from the
SettingsRepository high scores. This duplication is a consistency bug.

- `app/src/main/java/.../ui/viewmodels/SimonGameViewModel.kt:453-468` (resetHighScore)
- `app/src/main/java/.../data/manager/StatisticsManager.kt:56-67` (recordGameResult)

**`SettingsRepository.updatePreference()` is defined but never called.**
`DataStoreSettingsRepository.kt:80-86` defines a `private fun updatePreference(block)`
helper, but every `setX()` method uses `scope.launch { dataStore.edit { ... } }`
directly. The helper is dead code.

- `app/src/main/java/.../data/repository/SettingsRepository.kt:80-86`

**Three `SoundPack` entries share `resourcePrefix = "standard"`** (MUSICAL, NATURE,
SCI_FI at `SoundPack.kt:33-42`). The UI shows these as distinct selectable options and
users have no way to know they play the same sounds. Either mark them as "Coming Soon"
and disable selection, or add a badge in the UI making the fallback explicit.

---

## 6. Concurrency & Coroutines

### Issues

**`runBlocking` on the main thread** (covered in section 5, highest priority).

**`StatisticsManager` creates its own `CoroutineScope(Dispatchers.IO)` at line 41.**
This scope is never cancelled. When the application process ends the scope is just
abandoned. For a single-Activity app this is normally benign, but it is poor practice:
the scope is not tied to any lifecycle and cannot be supervised. Use
`ProcessLifecycleOwner.get().lifecycleScope` or inject a scope from the DI container.

- `app/src/main/java/.../data/manager/StatisticsManager.kt:41`

**`DataStoreSettingsRepository` has the same problem at line 74.**

- `app/src/main/java/.../data/repository/SettingsRepository.kt:74`

**`viewModelScope.launch` without structured context in `checkSequenceMatch`.** Two
anonymous `viewModelScope.launch` coroutines are started inside `checkSequenceMatch()`
(lines 748 and 761) to delay before calling `handleGameOver`. If the ViewModel is
cleared between the delay and the call, `handleGameOver` will still run on the default
dispatcher. Since `viewModelScope` is cancelled on `onCleared` this is actually safe,
but starting fire-and-forget coroutines inside a logic function makes the control flow
hard to reason about.

**`ParticleEffect` uses a manual `while` loop + `delay(16)` for animation.**
This bypasses Compose's animation system entirely. Use `Animatable` or
`animateFloatAsState` with a `tween` spec instead, which integrates with the Compose
rendering loop and can be cancelled properly.

- `app/src/main/java/.../ui/components/ParticleEffect.kt:74-79`

**`android.os.Handler(Looper.getMainLooper()).post { vibrate() }` in
`SimonSoundManager.kt:535` and `620`.** Inside `SimonSoundManager`, `vibrate()` is
dispatched via a legacy `Handler` to the main looper. The manager already knows it is
called from coroutines on the main dispatcher (via the ViewModel). This extra dispatch
is unnecessary and adds latency to haptic feedback.

---

## 7. Error Handling

### Issues

**No `Result` wrapper or typed error domain.** All error conditions in the data layer
silently fall back to defaults or log a warning. For example, `getSoundPack()` catches
`Exception` and returns `SoundPack.STANDARD` without surfacing the error to the
ViewModel. `loadSoundPack()` catches `Exception` and logs it, but the ViewModel only
knows whether sounds loaded via the boolean `soundsLoaded` / string `soundLoadError`.

For a game this small, full typed-error propagation is YAGNI. The minimum fix is to
avoid swallowing `Exception` without at least logging at error level. The current mix
of `Log.e` and silent `try/catch` (e.g., `catch (_: Exception)` in `SettingsScreen.kt:419`)
makes debugging harder.

**`catch (_: Exception)` is used for the Play Store intent** at `SettingsScreen.kt:419`.
Catching all exceptions for a simple `startActivity` is overly broad; it should only
catch `ActivityNotFoundException`.

```kotlin
// BEFORE
} catch (_: Exception) {

// AFTER
} catch (_: ActivityNotFoundException) {
```

**`ERROR_SOUND_VOLUME_BOOST = 1.2f` clamps to `MAX_VOLUME = 1.0f`** in
`playSoundWithVolume()` because of `coerceIn(MIN_VOLUME, MAX_VOLUME)`. The constant is
misleading - the boost has no effect. Remove it or raise `MAX_VOLUME` to `1.2f` if
SoundPool supports values above 1.0 (it does, up to the stream volume).

- `app/src/main/java/.../domain/GameConstants.kt:43` (`ERROR_SOUND_VOLUME_BOOST`)
- `app/src/main/java/.../data/manager/SimonSoundManager.kt:680-681`

---

## 8. Testing

### Observations

The existing unit tests are well-structured: they test domain models (`GameState`,
`ScreenState`, `SimonGameUiState`, `SimonButton`, `GameConstants`) and cover the public
API of those objects including edge cases. The use of backtick-style test names
improves readability.

### Issues

**Zero ViewModel tests.** `SimonGameViewModel` is the core of the application and has
no unit tests at all. The key scenarios to cover:

- Correct sequence matching advances the level
- Wrong button triggers game over
- Timeout triggers game over
- `onPause`/`onResume` lifecycle saves and restores state correctly
- High score updates only when current level exceeds prior best
- `resetHighScore` zeroes both high score fields and statistics

`mockk` is already in the dependency catalog. A minimal test would look like:

```kotlin
class SimonGameViewModelTest {
    private val soundManager: SimonSoundManager = mockk(relaxed = true)
    private val statisticsManager: StatisticsManager = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)

    private lateinit var viewModel: SimonGameViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { settingsRepository.getSoundPack() } returns SoundPack.STANDARD
        // ... stub remaining getters
        viewModel = SimonGameViewModel(soundManager, statisticsManager, settingsRepository)
    }

    @Test
    fun `pressing correct button sequence advances level`() = runTest { ... }
}
```

**No `StatisticsManager` / `DataStoreSettingsRepository` tests.** The `runBlocking`
reads and fire-and-forget writes are untested. These are the highest-risk area for
future regressions.

**`ExampleInstrumentedTest` is a generated placeholder** and tests nothing.

- `app/src/androidTest/java/.../ExampleInstrumentedTest.kt`

**Turbine is not a dependency.** If Flow-based tests are added for the repository layer,
adding `app.cash.turbine:turbine` would simplify flow collection in tests.

**No Compose UI tests.** At minimum, a smoke test that the game screen renders without
crashing would be valuable.

---

## 9. Build & Gradle

### Observations

Version catalog usage is correct. BOM for Compose is used properly. `compileSdk = 35`,
`targetSdk = 35` is up to date. AGP 8.10.1 is current. Kotlin 2.1.10 is current.

### Issues

**`isMinifyEnabled = false` in the release build type** (`app/build.gradle.kts:24`).
This ships the release APK unobfuscated and without dead-code stripping. For a Play
Store release, enable R8:

```kotlin
// BEFORE
isMinifyEnabled = false

// AFTER
isMinifyEnabled = true
isShrinkResources = true
```

The default `proguard-android-optimize.txt` already handles most common rules. Add
Koin-specific keep rules to `proguard-rules.pro`:

```
-keep class org.koin.** { *; }
-keep class com.happypuppy.memorylights.di.** { *; }
```

**`versionCode = 1` and `versionName = "1.0"` are hardcoded** in
`app/build.gradle.kts:15-16`. Before Play Store submission these should be managed or
at least documented.

**`rootProject.name = "My Application"`** in `settings.gradle.kts:22` should be
`"MemoryLights"`.

**`org.gradle.parallel=true` is commented out** in `gradle.properties:12`. For a
single-module project this has no effect, but it is a stale comment that could confuse.

**`lifecycle-process` dependency** (`app/build.gradle.kts:48`, `libs.versions.toml:19`)
is included but `ProcessLifecycleOwner` is not used anywhere in the codebase. This is an
unused dependency.

---

## 10. Performance

### Observations

`collectAsStateWithLifecycle` is used correctly for lifecycle-safe collection.
`derivedStateOf` is used in `SimonGameScreen` and `SettingsScreen` to avoid unnecessary
recompositions. `SimonPanel` uses `remember(color)` to cache color calculations.
`animateFloatAsState` is used with a named `label` parameter.

### Issues

**`SimonGameUiState.sequence` and `playerSequence` are `List<SimonButton>` stored
inside the state.** Every button press creates a new list via `+`. For a typical game
lasting 20-30 rounds this is negligible, but it is worth noting.

**`SimonGameScreen` reads `LocalConfiguration.current`** (`SimonGameScreen.kt:238`).
`LocalConfiguration` changes on every orientation change and causes the entire
`SimonGameScreen` to recompose. Move orientation detection to `MainActivity` or higher
and pass `isLandscape: Boolean` as a parameter, or use a `WindowSizeClass` from
`androidx.compose.material3.adaptive` which is stable-typed.

**`ParticleEffect` redraws all particles on every frame by mutating `animationProgress`
state**, which causes the entire `Canvas` to redraw at 60 fps for 2 seconds. This is
correct behavior for a particle system, but the `LaunchedEffect` drives frames via
`delay(16)` instead of `withFrameMillis`, which can cause drift. Use `withFrameMillis`
for accurate frame timing.

```kotlin
// BEFORE (ParticleEffect.kt:74)
while (animationProgress < 1f) {
    val elapsed = System.currentTimeMillis() - startTime
    animationProgress = (elapsed / animationDuration).coerceAtMost(1f)
    delay(16)
}

// AFTER
while (animationProgress < 1f) {
    withFrameMillis { frameTime ->
        animationProgress = ((frameTime - startTime) / animationDuration).toFloat().coerceAtMost(1f)
    }
}
```

**`SimonSoundManager.listAllRawResources()` iterates all `R.raw` fields via reflection
at startup** (`SimonSoundManager.kt:200-217`). This is pure debug logging that ships in
release builds. Wrap it in `if (BuildConfig.DEBUG)`.

**`SimonSoundManager.debugResourceNotFound()` also uses reflection** (`SimonSoundManager.kt:369-401`)
and runs in release builds. Same fix: guard with `BuildConfig.DEBUG`.

---

## 11. Security

### Observations

No network access, no API keys, no user-identifiable data beyond game statistics. The
attack surface is minimal.

### Issues

**`android:allowBackup="true"` in `AndroidManifest.xml:11`.** This allows the user (and
`adb backup`) to extract all DataStore files containing high scores and settings. For a
game this is low-risk, but if the app ever adds any sensitive data, this flag must be
considered. The `backup_rules.xml` and `data_extraction_rules.xml` are present and
should be reviewed to exclude DataStore files from cloud backup if desired.

**`android:configChanges="orientation|keyboardHidden|screenSize"` in the manifest.**
Intercepting configuration changes manually prevents the system from re-creating the
Activity. This is intentional here (the ViewModel handles lifecycle) but means
that any layout resources that differ by configuration (e.g., landscape layout files)
will not be automatically applied. There are no such resources currently, so this is
acceptable.

**No secrets are present in the codebase.** Play Store URL is hardcoded as a string
literal in `SettingsScreen.kt:413`, which is fine.

---

## 12. Accessibility & UX

### Observations

`SimonPanel` applies `contentDescription`, `stateDescription`, and `Role.Button`
correctly. `view.announceForAccessibility()` is called for level changes and game state
transitions. Minimum touch target `sizeIn(minWidth = 48.dp, minHeight = 48.dp)` is
applied.

### Issues

**`IconButton` elements in `SimonGameScreen`'s `TopAppBar` use `contentDescription`
that describe current state, not the action.** For example, the mute button says "Mute"
when sound is on (action) and "Unmute" when sound is off (action). This is actually
correct. However, the vibration button says "Disable Vibration" or "Enable Vibration"
which is also an action description - this is good. No changes needed here.

**Game-over overlay text ("GAME OVER", "HIGH SCORE!") has no accessibility
announcement.** `LaunchedEffect(uiState.gameState)` announces "Game over" via
`announceForAccessibility`, but "HIGH SCORE!" is never announced. Add it when
`uiState.showHighScoreText` becomes true.

**Text overlays use hardcoded `Color.Yellow`** for both "HIGH SCORE!" and "GAME OVER"
(`SimonGameScreen.kt:677, 699`). On light backgrounds or when dynamic color is active,
yellow text on a partially transparent overlay may have insufficient contrast. Since the
game background is always black, this is currently fine but is worth noting.

**No large-screen or foldable adaptation.** The game uses `fillMaxWidth(0.9f)` and
`fillMaxHeight(0.95f)` which will produce enormous buttons on a 12-inch tablet. Consider
capping the game panel with a `widthIn(max = 500.dp)` constraint.

**`SoundPack` options MUSICAL, NATURE, and SCI_FI appear fully selectable** in the UI
even though they play Standard sounds. A user selecting "Nature" will not understand why
they hear the same tones as Standard. Consider adding a "Coming Soon" badge or
disabling selection with a tooltip.

---

## 13. Code Quality

### Positive Callouts

- Sealed classes for `GameState` and `ScreenState` are correct Kotlin idiom.
- `GameConstants` object centralizes all magic numbers.
- DataStore migration from SharedPreferences is handled correctly.
- `SimonSoundManager` has solid memory-trim handling via `ComponentCallbacks2`.
- Koin setup is idiomatic for v4.x (`viewModelOf`, `single<Interface> { Impl() }`).

### Issues

**`private fun String.capitalize()` extension at `SimonGameScreen.kt:45-50` reimplements
`String.replaceFirstChar { it.uppercase() }`.** Kotlin stdlib deprecated `capitalize()`
in 1.5 and this local extension is a workaround, but it is used in at least 4 places
inside `SimonGameScreen` and once in `SimonSoundManager` (implicitly via enum names).
Better: add a `val displayName: String` property to `SimonButton` similar to
`SoundPack.displayName`.

**`onButtonRelease` at `SimonGameViewModel.kt:783` is an exact duplicate of the `else`
branch in `onButtonClick` at line 734.**

```kotlin
// onButtonClick (line 733-736) - release branch
} else {
    _uiState.update { it.copy(activeButtonPresses = it.activeButtonPresses - button) }
}

// onButtonRelease (line 783-785)
fun onButtonRelease(button: SimonButton) {
    _uiState.update { it.copy(activeButtonPresses = it.activeButtonPresses - button) }
}
```

`onButtonRelease` appears to not be called from the UI at all (searching the codebase
shows no call site). This is dead code.

**`handleGameOver()` (no-arg) at `SimonGameViewModel.kt:789` is a one-liner that
delegates to `handleGameOver(String)`.** The no-arg overload adds no value. Call
`handleGameOver("Wrong button pressed")` directly.

**Pervasive `Log.d` calls in production code**, including full sequences and timing
details. In a release build this writes to logcat, wastes I/O, and leaks game state.
Guard all `Log.d` calls with `if (BuildConfig.DEBUG)` or use a logging abstraction.
`Log.e` for real errors should remain unconditional.

**`SimonSoundManager.getContext()` at line 193 is unused** (no call sites found in the
project). Remove it.

**`triggerParticleEffects()` in `SimonGameViewModel.kt:541` is labeled "for testing"**
but is a public method on the production ViewModel. Move it to a test-only subclass or
remove it.

**`previousGameState` field is declared at line 144 but also conceptually conflicts with
`gameStateBeforeBackground` at line 52** - these serve similar but distinct purposes.
The naming could be clearer: `gameStateBeforeSettings` vs `gameStateBeforeBackground`.

**`res/raw/guy/blue_tone.wav`** - there is an orphaned subdirectory `res/raw/guy/`
containing a single `blue_tone.wav` file. Android's resource system does not load files
from subdirectories of `res/raw/`. This file is unreachable and should be deleted or
moved.

---

## 14. Quick Wins vs. Larger Refactors

| # | Item | Effort | Priority | File(s) |
|---|------|--------|----------|---------|
| 1 | Enable R8/minification in release build | S | P0 | `app/build.gradle.kts:24` |
| 2 | Replace `runBlocking` reads with `Flow`/`suspend` in DataStore layer | M | P0 | `SettingsRepository.kt:76`, `StatisticsManager.kt:42` |
| 3 | Fix `activeButtonPresses` duplication - remove from `SimonGameUiState` or remove local state in composable | S | P0 | `SimonGameUiState.kt:30`, `SimonGameScreen.kt:110` |
| 4 | Fix double high-score storage (`SettingsRepository` + `StatisticsManager`) | M | P1 | `StatisticsManager.kt:56`, `SimonGameViewModel.kt:453` |
| 5 | Remove `GameState.Settings` and drive navigation from `ScreenState` only | S | P1 | `SimonGameState.kt`, `MainActivity.kt:55`, `SimonGameViewModel.kt:195` |
| 6 | Remove `koinInject` from `SettingsScreen`; route sound preview through ViewModel | S | P1 | `SettingsScreen.kt:61, 151` |
| 7 | Guard debug logging (`listAllRawResources`, `debugResourceNotFound`, all `Log.d`) with `BuildConfig.DEBUG` | S | P1 | `SimonSoundManager.kt:200, 369` |
| 8 | Add `SimonGameViewModel` unit tests using `mockk` + `UnconfinedTestDispatcher` | L | P1 | new test file |
| 9 | Fix unsafe `viewModel as DefaultLifecycleObserver` cast in `MainActivity` | S | P1 | `MainActivity.kt:40` |
| 10 | Mark MUSICAL/NATURE/SCI_FI sound packs as "Coming Soon" in the UI | S | P1 | `SettingsScreen.kt`, `SoundPack.kt` |
| 11 | Delete orphaned `res/raw/guy/blue_tone.wav` | S | P1 | `app/src/main/res/raw/guy/blue_tone.wav` |
| 12 | Remove dead code: `onButtonRelease`, `updatePreference`, `getContext()`, `triggerParticleEffects` | S | P2 | multiple files |
| 13 | Extract `SimonButtonGrid` composable to reduce `SimonGameScreen` size | M | P2 | `SimonGameScreen.kt` |
| 14 | Replace manual `delay(16)` loop in `ParticleEffect` with `withFrameMillis` | S | P2 | `ParticleEffect.kt:74` |
| 15 | Replace `Handler(Looper.getMainLooper()).post` vibration dispatch with coroutine | S | P2 | `SimonSoundManager.kt:535, 620` |
| 16 | Move `LocalConfiguration` orientation detection to avoid full screen recomposition | S | P2 | `SimonGameScreen.kt:238` |
| 17 | Fix `ERROR_SOUND_VOLUME_BOOST` being silently clamped to 1.0 | S | P2 | `GameConstants.kt:43`, `SimonSoundManager.kt:680` |
| 18 | Add `widthIn(max = 500.dp)` constraint for tablet/large-screen layouts | S | P2 | `SimonGameScreen.kt` |
| 19 | Fix `rootProject.name` from "My Application" to "MemoryLights" | S | P2 | `settings.gradle.kts:22` |
| 20 | Cancel `CoroutineScope` in `StatisticsManager` and `DataStoreSettingsRepository` on app destruction | M | P2 | `StatisticsManager.kt:41`, `SettingsRepository.kt:74` |
| 21 | Add `displayName` property to `SimonButton` and remove local `capitalize()` extension | S | P2 | `SimonButton.kt`, `SimonGameScreen.kt:45` |
| 22 | Announce "HIGH SCORE!" via `announceForAccessibility` | S | P2 | `SimonGameScreen.kt` |
| 23 | Remove unused `lifecycle-process` dependency | S | P2 | `app/build.gradle.kts:48`, `libs.versions.toml:19` |
| 24 | Replace `catch (_: Exception)` with `catch (_: ActivityNotFoundException)` | S | P2 | `SettingsScreen.kt:419` |
| 25 | Disable dynamic color (`dynamicColor = false`) to preserve game aesthetic | S | P2 | `Theme.kt:40` |

**Effort key:** S = under 30 min, M = half day, L = one to two days.
**Priority key:** P0 = fix before Play Store release, P1 = high value / near-term, P2 = good to have.
