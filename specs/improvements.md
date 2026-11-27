# Memory Lights — Consolidated Improvement Roadmap

Canonical audit + execution plan. Combines original 2025 best-practices audit, post-Session-5 file:line audit, and 2026-05-02 multi-agent UI/UX/code/feature scan. Closed items listed in the Done section below; status reflects current `main` HEAD.

---

## Current State Snapshot (updated 2026-05-02 after Phase 1)

| Area | Status |
|---|---|
| Architecture | MVVM with Koin DI, single `:app` module. `SimonGameViewModel` ~1,100 lines (large but functional). Repository pattern done for settings, statistics still in `data/manager/` (rename pending — item #29). |
| Persistence | DataStore migration complete. **Off-main-thread Flow-based reads in place** (`settingsFlow`, `statisticsFlow`). `recordGameResult` now read+write inside single `dataStore.edit` — no more race. |
| Sound | 7 real packs (standard, funny, electronic, retro, **musical**, **sci-fi**, **nature** — last 3 synth-generated 2026-05-03). Lazy loading + memory trim done. Audio focus done. |
| Tests | 52 unit tests, all green (one removed when `GameState.Settings` was deleted in step 3). |
| A11y | Content descriptions, state descriptions, role, level/state announcements, 48dp touch targets — all done. "HIGH SCORE!" still not announced (item #15). |
| Build | **R8 + resource shrinking ON** for release. APK shrunk 35.5 MB → 7.2 MB. Koin + Activity keep rules in `proguard-rules.pro`. `Instantiatable` lint false positive suppressed via `tools:ignore` on MainActivity. `rootProject.name = "MemoryLights"` ✅. `versionCode 1` / `versionName "1.0"` ✅ already set. Signing config still TODO — see `playstore/RELEASE.md`. |
| Git | All Phase 0 + Phase 1 work pushed to `origin/main`. GitHub repo renamed `MyApplication2` → `MemoryLights`. |
| Device | Pixel_8_API_34 emulator started + smoke-tested through Phase 1 — app launches, DataStore-persisted high score loads on startup. |

---

## Master Ranked Status List

**Status legend:** ✅ done · ⚠️ partial · ❌ open · ⏭️ skipped (decision recorded) · 🆕 added by post-Session-5 audit · "(multi-agent)" tag = surfaced by 2026-05-02 parallel UI/UX/code/feature scan

### P0 — Blockers before Play Store release

| # | Item | Status | Effort | Files |
|---|---|---|---|---|
| 1 | Enable R8 / `isMinifyEnabled = true` + Koin keep rules | ✅ Phase 1 | S | `app/build.gradle.kts:23`, `app/proguard-rules.pro`, `AndroidManifest.xml` (Instantiatable suppress) |
| 2 | Replace `runBlocking { dataStore.data.first() }` on main thread with one-time suspend init or `Flow` collected in `viewModelScope` | ✅ Phase 1 | M | `data/repository/SettingsRepository.kt`, `data/manager/StatisticsManager.kt`, `viewmodels/SimonGameViewModel.kt` |
| 3 | `activeButtonPresses` is duplicated: ViewModel state field is dead — only `localPressedButtons` in screen drives UI. Pick one source of truth. | ✅ Phase 1 | S | `domain/model/SimonGameUiState.kt`, `viewmodels/SimonGameViewModel.kt` (private `activePresses` set; dead `onButtonRelease` deleted) |
| 4 | Commit Session 5 work in logical chunks; push to `origin/main` | ✅ Phase 0 | S | 5 commits + push |
| 5 | Reconcile `specs/improvements.md` vs root `IMPROVEMENTS.md` — keep `specs/` as canonical, delete root copy | ✅ Phase 0 | S | merged + deleted |

### P1 — High value, near-term

| # | Item | Status | Effort | Files |
|---|---|---|---|---|
| 6 | Dual high-score storage: `SettingsRepository` (`high_score_4_button`/`high_score_6_button`) and `StatisticsManager` (`high_score`) updated separately, can diverge. Single owner. | ✅ Phase 2 step 3 (high score now lives only in `SettingsRepository`; `StatisticsManager`/`GameStatistics` no longer track it; StatisticsScreen consumes per-mode score from UI state) | M | `domain/model/GameStatistics.kt`, `data/manager/StatisticsManager.kt`, `ui/screens/StatisticsScreen.kt` |
| 7 | Remove `GameState.Settings` (overlaps `ScreenState.Settings`); drive nav from `ScreenState` only. Fixes back-press bug that fires on Statistics screen. | ✅ Phase 2 step 3 (back press now uses `screenState`: Statistics→Settings→Game→exit) | S | `domain/model/SimonGameState.kt`, `ui/MainActivity.kt`, `viewmodels/SimonGameViewModel.kt` |
| 8 | Remove `koinInject<SimonSoundManager>` in `SettingsScreen`; route preview through ViewModel | ✅ Phase 2 step 3 (`VM.setSoundPack` now plays preview tone) | S | `ui/screens/SettingsScreen.kt`, `viewmodels/SimonGameViewModel.kt` |
| 9 | Replace unsafe `viewModel as DefaultLifecycleObserver` cast | ✅ Phase 2 step 3 | S | `ui/MainActivity.kt` |
| 10 | Guard all `Log.d` + reflection-based `listAllRawResources()` / `debugResourceNotFound()` with `BuildConfig.DEBUG` | ✅ Phase 2 step 3 (reflection helpers early-return when not debug; `buildConfig = true` enabled) | S | `app/build.gradle.kts`, `data/manager/SimonSoundManager.kt` |
| 11 | Add `SimonGameViewModelTest` with `mockk` + `UnconfinedTestDispatcher` (deps already in catalog). Cover sequence-match, wrong-button → game over, timeout, high-score, lifecycle save/restore. | ✅ Phase 2 step 1 | L | `app/src/test/java/.../ui/viewmodels/SimonGameViewModelTest.kt` (8 tests), `app/src/test/java/.../domain/SequenceTimingTest.kt` (7 tests) |
| 12 | Add `Turbine` to catalog; write flow tests for `DataStoreSettingsRepository` and `StatisticsManager` | ✅ Phase 2 step 2 | M | `gradle/libs.versions.toml`, `DataStoreSettingsRepositoryTest`, `StatisticsManagerTest` (10 tests, in-memory `PreferenceDataStoreFactory.create` + temp folder) |
| 13 | Mark MUSICAL/NATURE/SCI_FI as "Coming Soon" in UI (see Sound Track below — implementing them obsoletes this) | ⏭️ Phase 3 obsoleted (all 3 packs shipped 2026-05-03; descriptions now reflect actual content) | S | `ui/screens/SettingsScreen.kt`, `domain/enums/SoundPack.kt` |
| 14 | Delete orphan `app/src/main/res/raw/guy/blue_tone.wav` (Android does not load nested raw resources) | ✅ Phase 0 | S | deleted |
| 15 | Add HIGH SCORE accessibility announcement | ✅ Phase 2 step 3 | S | `ui/screens/SimonGameScreen.kt` |
| 38 | `calculateSequenceTiming` has a dead `if` branch — difficulty trigger evaluates differently at level 5 vs 6-8. Collapse to `(currentLevel - 1) / DIFFICULTY_INTERVAL`, gated on `currentLevel >= 5`. | ✅ Phase 2 step 1 (collapsed during extraction to pure helper `domain/SequenceTiming.kt`; numeric output unchanged, locked by `SequenceTimingTest`) | S | `domain/SequenceTiming.kt`, `viewmodels/SimonGameViewModel.kt` |
| 39 | Render `soundLoadError` in UI — sound-load failure currently shows a permanent silent spinner with no explanation/retry. | ✅ Phase 2 step 4 (Warning icon + "Sound load failed" / "Game playable without audio" replaces silent spinner when `soundLoadError != null`) | S | `ui/screens/SimonGameScreen.kt` |
| 40 | `setVibrationEnabled(true)` silently fires test buzz — surprises user, reads as bug. Either remove the side-effect or label it ("Test buzz"). | ✅ Phase 2 step 4 (side-effect removed; `setVibrationEnabled` is now a pure setter) | S | `data/manager/SimonSoundManager.kt` |
| 41 | Settings card double-tap risk: `SettingsCard.clickable` + inner `TextButton("Reset")` both fire on tap. Drop one click target. | ✅ Phase 2 step 4 (Settings reset card no longer wraps in clickable; inner `TextButton` is the sole trigger; `SettingsCard.onClick` is now nullable) | S | `ui/screens/SettingsScreen.kt` |
| 42 | Splash white flash — `themes.xml` inherits `Theme.Material.Light.NoActionBar`. Set `android:windowBackground = #000000` and use Splash Screen API. | ✅ Phase 2 step 4 + Wave 21 (2026-11-27): `windowBackground` set to `@android:color/black`, upgraded in Wave 21 to Jetpack `core-splashscreen 1.2.0` (`Theme.MemoryLights.Starting` + 4-button vector `drawable/ic_splash.xml` + `installSplashScreen()` held until `settingsLoaded` or 2s safety timeout) | S | `app/src/main/res/values/themes.xml`, `app/src/main/res/drawable/ic_splash.xml`, `app/src/main/AndroidManifest.xml`, `ui/MainActivity.kt` |
| 43 | `saveSettings()` writes all 7 DataStore keys per single setting toggle (~9 callsites + onCleared). Replace whole-state flush with per-setting writes; drop redundant `saveSettings()` in `onCleared`. | ✅ Phase 2 step 4 (`saveSettings()` deleted; each setter / high-score updater calls only its own repository write; `onCleared` no longer flushes) | M | `viewmodels/SimonGameViewModel.kt` |
| 44 | "Memory Lights+" toggle mid-game silently wipes current run. Add confirm dialog if `gameState != WaitingToStart && gameState != GameOver`. | ✅ Phase 2 step 4 ("End current game?" dialog with Cancel/Switch shows when `hasActiveGame` is true; toggle is gated on confirmation) | S | `ui/screens/SettingsScreen.kt`, `ui/screens/SimonGameScreen.kt` |
| 45 | "Reset High Score" card title understates — actually wipes ALL statistics. Rename title to "Reset Score & Statistics" so the destructive scope is visible without opening the dialog. | ✅ Phase 2 step 4 | S | `ui/screens/SettingsScreen.kt` |
| 46 | Game-over flow has no score summary before tap-to-restart — center disc tap immediately restarts, killing reward loop and removing share motivation (depends on F10 if added). | ✅ Phase 2 step 4 (center disc on GameOver now stacks final score + best score + play arrow; tap still restarts) | S | `ui/screens/SimonGameScreen.kt` |

### P2 — Polish, post-launch

| # | Item | Status | Effort | Files |
|---|---|---|---|---|
| 16 | Remove dead code: `onButtonRelease`, `updatePreference`, `getContext()`, `triggerParticleEffects` | ✅ Phase 5 step 3 (`getContext()` and `triggerParticleEffects()` deleted; `onButtonRelease` already removed in Phase 1; `updatePreference` not present) | S | multiple |
| 17 | Extract `SimonButtonGrid` composable to shrink 750-line `SimonGameScreen` | ✅ Phase 11 (2026-05-16, Wave 2): new `ui/components/SimonButtonGrid.kt` owns layout selection (6-btn landscape / 6-btn portrait / 4-btn classic), orientation read, and per-mode panel padding. Three near-identical inline blocks (~180 lines) collapsed to a single declarative call. Row orderings extracted as top-level constants so future button-rendering work (e.g. #35 color-blind labels) edits one declaration. Subsumes #20: the screen no longer reads `LocalConfiguration` directly. SimonGameScreen.kt: 1316 → 1134 lines. 64/64 tests green, lint clean, debug APK builds. | M | `ui/screens/SimonGameScreen.kt`, `ui/components/SimonButtonGrid.kt` |
| 18 | Replace `delay(16)` loop in `ParticleEffect` with `withFrameMillis` | ✅ Phase 5 step 3 (frame-clock driven; ticks per actual frame instead of 16-ms timer; cleaned up unused `Log`/`DrawScope`/`dp` imports) | S | `ui/components/ParticleEffect.kt` |
| 19 | Replace `Handler(Looper.getMainLooper()).post` vibration dispatch | ✅ Phase 5 step 5 (`playSound` is only called from `@MainThread` `onButtonClick` and `viewModelScope` launches that default to `Dispatchers.Main.immediate`; `vibrator.vibrate()` is thread-safe — Handler indirection dropped, direct call) | S | `data/manager/SimonSoundManager.kt` |
| 20 | Move `LocalConfiguration` orientation read out of `SimonGameScreen` to avoid full-screen recomposition | ✅ Phase 11 (2026-05-16): subsumed by #17 — the orientation read is now inside `SimonButtonGrid`; the screen no longer touches `LocalConfiguration` at all, so Configuration changes outside the grid don't re-evaluate the screen. | S | `ui/screens/SimonGameScreen.kt`, `ui/components/SimonButtonGrid.kt` |
| 21 | Fix `ERROR_SOUND_VOLUME_BOOST = 1.2f` silently clamped to 1.0 | ✅ Phase 5 step 3 (constant deleted; error sound now uses `MAX_VOLUME` with comment explaining SoundPool's 1.0 ceiling) | S | `domain/GameConstants.kt`, `data/manager/SimonSoundManager.kt` |
| 22 | Tablet/foldable cap: `widthIn(max = 500.dp)` on game panel | ✅ Phase 5 step 3 | S | `ui/screens/SimonGameScreen.kt` |
| 23 | Rename `rootProject.name` → `MemoryLights` | ✅ Phase 5 step 3 | S | `settings.gradle.kts` |
| 24 | Cancel `CoroutineScope` in `StatisticsManager` and `DataStoreSettingsRepository` on app destruction | ✅ Phase 10 (2026-05-16): both repos now distinguish owned vs injected scopes via an `ownsScope` flag on the `internal` 3-arg constructor; `fromContext` factories build a `SupervisorJob() + Dispatchers.IO` scope and pass `ownsScope = true`; tests use the public 2-arg constructor (`ownsScope = false`) so the existing `TestScope` cancellation pattern is untouched. `SettingsRepository` interface gains `close()`; both impls cancel only when they own the scope. Koin module wires `single { ... }.withOptions { onClose { it?.close() } }` so `stopKoin()` (and `SimonApp.onTerminate` on emulators) releases the IO scopes. New tests cover both branches: scope cancelled when owned, scope left alone when injected. 64 tests green. | M | `data/repository/StatisticsRepository.kt`, `data/repository/SettingsRepository.kt`, `di/AppModule.kt`, `SimonApp.kt` |
| 25 | Add `displayName` property to `SimonButton`; remove local `capitalize()` extension | ✅ Phase 5 step 3 (`SimonButton.displayName`; `String.capitalize()` deleted; `currentSoundPack.displayName` reused — UI now shows "Sci-Fi" instead of "Sci_fi") | S | `domain/enums/SimonButton.kt`, `ui/screens/SimonGameScreen.kt` |
| 26 | Remove unused `lifecycle-process` dependency | ✅ Phase 5 step 3 | S | `app/build.gradle.kts`, `libs.versions.toml` |
| 27 | `catch (_: Exception)` → `catch (_: ActivityNotFoundException)` for Play Store intent | ✅ Phase 5 step 3 | S | `ui/screens/SettingsScreen.kt` |
| 28 | Disable dynamic color (`dynamicColor = false`) to preserve all-black aesthetic | ✅ Phase 5 step 3 (default flipped + comment explains why; flip on if F12 theme toggle ships) | S | `ui/theme/Theme.kt` |
| 29 | Move `StatisticsManager` to `data/repository/` and rename `StatisticsRepository` | ✅ Phase 5 step 6 (`StatisticsManager` → `data/repository/StatisticsRepository`; class + test class + DI binding + ViewModel field renamed; old `data/manager/` test dir cleaned up; 53 tests still green) | S | rename |
| 30 | 4-button mode padding inconsistency (Green 2.dp vs others 4.dp) | ✅ Phase 5 step 5 (Green now also 4.dp; all four panels share padding) | S | `ui/screens/SimonGameScreen.kt` |
| 31 | Hardcoded `.offset(y = ...)` for text overlays — switch to `Alignment` | ✅ Phase 5 step 5 (3 named `BiasAlignment` constants — `OverlayBias6Button`, `OverlayBias4Button`, `OverlayBias4ButtonTurn` — replace inline `.offset(y = ...)` calls on HIGH SCORE / GAME OVER / SPEED UP / YOUR TURN; bias is screen-proportional so positioning stays consistent across phone sizes) | S | `ui/screens/SimonGameScreen.kt` |
| 32 | Progressive onboarding / first-run tutorial | ❌ | M | new |
| 33 | Snackbar confirmation on settings change | ✅ Phase 5 step 6 (`SnackbarHost` added to Scaffold; sound-pack pick + difficulty toggle + ML+ confirm-dialog accept all fire transient `Short`-duration snackbar; verified live: "Difficulty: On" appears after toggle) | S | `ui/screens/SettingsScreen.kt` |
| 34 | Reduce-motion option / respect `LocalReducedMotion` | ✅ Wave 20 (2026-11-27): `ParticleEffect` and CTA pulse read `Settings.Global.ANIMATOR_DURATION_SCALE == 0`; reduced motion skips particle loops and completes immediately (tested in `ScreenAccessibilityTest`) | S | `ui/components/ParticleEffect.kt`, `ui/screens/SimonGameScreen.kt` |
| 35 | Color-blind mode (icons/patterns on buttons) | ✅ Wave 10 (2026-07-10): product decision = geometric shapes (not numbers). Default OFF. Green=circle, Red=triangle, Yellow=square, Blue=cross, Purple=diamond, Orange=stripes. Overlays on `SimonPanel` + small glyphs on `CenterDiscCanvas` arcs. Settings toggle under Gameplay. | M | `domain/enums/ColorBlindShape.kt`, `ui/components/{ColorBlindShapeDraw,SimonPanel,SimonButtonGrid,CenterDiscCanvas}.kt`, settings + VM |
| 36 | Keyboard / external controller support (TV / ChromeOS / gamepad) | ⏭️ Deferred (2026-05-03) — out of scope for v1.0; revisit if TV / ChromeOS becomes target platform | L | new |
| 47 | `activePresses` mutable set lacks `@MainThread` discipline — touch + coroutines both touch it without lock. Annotate `@MainThread` or wrap in mutex. | ✅ Phase 5 step 1 (`onButtonClick` annotated `@MainThread`; comment on `activePresses` documents single-mutator contract) | S | `viewmodels/SimonGameViewModel.kt` |
| 48 | Externalize all UI strings to `strings.xml` — currently only `app_name` is externalized. Blocks i18n + string audit. | ✅ Phase 8 step 1 (2026-05-03): all 5 screens + ScoreShareHelper + MainActivity routed through `stringResource(R.string.*)` / `context.getString(...)`; `SoundPack`, `GameMode`, `SimonButton` carry `@StringRes displayNameRes` (and `descriptionRes` for SoundPack) so the enum-bound copy translates cleanly. ~120 new keys. Domain layer holds `R.string.*` integer IDs only — no Context dependency leaks. 50 unit tests still green; build + lint clean; verified live on Pixel_8_API_34 (Settings hub, Game Modes, GameOver overlay all render from resources). | M | `res/values/strings.xml`, all `ui/screens/*.kt`, `ui/share/ScoreShareHelper.kt`, `domain/enums/{SimonButton,SoundPack,GameMode}.kt`, `ui/MainActivity.kt` |
| 49 | Dedupe duplicated dark hex literals (`0xFF1D1D1D`, `0xFF1A1A1A`, `0xFF121212`, `0xFF303030`) across 3 screens — move to `Color.kt`. Prereq for #28 (dynamicColor) and F12 (theme toggle). | ✅ Phase 5 step 1 (5 semantic tokens: `CardBackground`, `DialogBackground`, `SurfaceContainer`, `SurfaceSelected`, `SurfaceContainerFade` in `theme/Color.kt`; all 3 screens import them) | S | `ui/theme/Color.kt`, `ui/screens/*.kt` |
| 50 | `SimonButton.index` duplicates `ordinal` — drop the field, drop the test that asserts on it. | ✅ Phase 5 step 1 (field removed; 2 SimonButton tests asserting on `index` deleted; only `ordinal` remains) | S | `domain/enums/SimonButton.kt`, `domain/enums/SimonButtonTest.kt` |
| 51 | ProGuard `-keep class org.koin.**` is too broad. Koin 4.x with `viewModelOf` doesn't need the wildcard — narrow to `KoinComponent` + reflection helpers actually used. Reclaim release APK size. | ✅ Phase 5 step 2 (wildcard `org.koin.**` keep dropped; only project DI module kept; release APK 7.84 MB → 7.48 MB; signed release verified booting cleanly on emulator) | S | `app/proguard-rules.pro` |
| 52 | `handleButtonInteraction` lambda not `remember`ed — invalidates all 6 `SimonPanel`s every recomposition. Wrap in `remember(localPressedButtons, onButtonClick)`. | ✅ Phase 5 step 1 (wrapped in `remember(onButtonClick)`; closure reads `localPressedButtons` via `MutableState` so stable lambda survives state changes) | S | `ui/screens/SimonGameScreen.kt` |
| 53 | `Brush.linearGradient` allocated per recomposition in `SimonPanel`. Wrap in `remember(buttonColors)`. | ✅ Phase 5 step 1 (`buttonColors` and `buttonBrush` both wrapped; brush only re-allocs when `userPressed`/`isLit`/base colors change) | S | `ui/components/SimonPanel.kt` |
| 54 | `isLit` flash is hard color switch — no smooth brightness ramp. Use `animateColorAsState` so the Simon flash matches the press spring polish. | ✅ Phase 5 step 2 (`litProgress = animateFloatAsState(tween 120ms FastOutSlowInEasing)` + per-component `lerp` between off and lit gradient lists) | M | `ui/components/SimonPanel.kt` |
| 55 | Overlay text (HIGH SCORE / GAME OVER / YOUR TURN) appears with no `AnimatedVisibility` — abrupt pop-in. Wrap each `if` block in fade/slide. | ✅ Phase 5 step 2 (each overlay wrapped in `AnimatedVisibility(fadeIn + scaleIn)`) | S | `ui/screens/SimonGameScreen.kt` |
| 56 | `WaitingToStart` shows static "1" with no tap-to-begin affordance — pulsing animation, play icon, or "Tap to start" label needed (mirrors GameOver's play affordance). | ✅ Phase 5 step 2 (Pattern B: keep level "1", add play arrow + "Tap to start" label; disc now tappable in both `GameOver` and `WaitingToStart`; subtle `rememberInfiniteTransition` scale pulse 1.0→1.06 telegraphs the affordance) | M | `ui/screens/SimonGameScreen.kt` |
| 57 | Statistics screen uses `Icons.DateRange` for 3 unrelated rows ("Games Played", "Average Score", "Total Score"). Pick distinct icons. | ✅ Phase 5 step 5 (Games Played → `PlayArrow`, Average Score → `AutoMirrored.List`, Total Score → `AddCircle`, Best Streak → `CheckCircle` so it stops duplicating High Score's `Star`; verified live on emulator) | S | `ui/screens/StatisticsScreen.kt` |
| 58 | `Typography` scale defined in `Type.kt` but unused — every `Text` sets `fontSize` directly, losing semantic role + a11y type-scale. Migrate to `MaterialTheme.typography.*`. | ✅ Phase 9 step 1 (2026-05-05): `Type.kt` redefined with full role-tuned scale (display/headline/title/body/label, headers Bold by default to match overlay aesthetic). All six UI screens migrated — 36/32/28/24sp Bold → display/headline; 22/18sp Bold → title; 16/14sp → body; 13/11/10sp → label. Odd one-offs (9sp, 26sp) use `role.copy(fontSize=Nsp)`. Unblocks F12. 50 tests green; live verify on Pixel_8_API_34 game screen confirms no regressions. | M | `ui/theme/Type.kt`, all 6 `ui/screens/*.kt` |
| 59 | `SoundPackOption` double ripple: `Row.clickable` + `RadioButton.onClick` both fire ripples on same tap. Pass `interactionSource = remember { MutableInteractionSource() }` and `indication = null` to one of them. | ✅ Phase 5 step 5 (`RadioButton.onClick = null` — Row click handles selection; ripple only fires once over the full row) | S | `ui/screens/SettingsScreen.kt` |
| 60 | "Your Turn" cue vanishes after round 3, leaving only faint halo as turn indicator. Keep persistent affordance throughout the player's turn (subtle pulsing border, mode-tag, or sticky text). | ✅ Phase 5 step 4 (static halo replaced with `rememberInfiniteTransition` alpha-pulse 0.06→0.18 + scale 1.0→1.05; now visible for entire `PlayerRepeating` phase, not just rounds 0-2) | S | `ui/screens/SimonGameScreen.kt` |
| 61 | Timeout has no visible countdown — game ends "from nowhere" at 10s. Add a thin progress ring around the center disc that drains during the player's turn. | ✅ Phase 5 step 2 (`timeoutResetTick: Int` added to `SimonGameUiState`; ViewModel increments on every `startTimeoutTimer`; UI runs `Animatable(1f)` over `PLAYER_TIMEOUT_MS` keyed on tick, draws thin 124-dp ring around the disc with `lerp(red, green, fraction)` color shift) | M | `domain/model/SimonGameUiState.kt`, `viewmodels/SimonGameViewModel.kt`, `ui/screens/SimonGameScreen.kt` |
| 62 | Difficulty speed-up invisible to player. When difficulty reduces timing, flash a "Speed Up!" text or attach a level-tier subtitle ("Level 5 - Fast"). Pairs with item #38. | ✅ Phase 5 step 4 (`showSpeedUpText` flag in `SimonGameUiState`; ViewModel sets true in `advanceToNextLevel` when difficulty enabled and `(newLevel - 1) % DIFFICULTY_INTERVAL == 0` and `newLevel >= 5`; `speedUpTextJob` auto-clears after `SPEED_UP_TEXT_DISPLAY_MS = 1200ms`; UI overlay `AnimatedVisibility` fade+scale) | S | `domain/model/SimonGameUiState.kt`, `domain/GameConstants.kt`, `viewmodels/SimonGameViewModel.kt`, `ui/screens/SimonGameScreen.kt` |
| 63 | Sound pack preview always plays GREEN regardless of pack tapped. Cycle through all colors, or play a random color, so player auditions full pack character. | ✅ Phase 5 step 5 (`previewToneIndex` cursor in ViewModel cycles through `getAvailableButtons(memoryLightsPlusEnabled)` so the player hears each button's tone in turn) | S | `viewmodels/SimonGameViewModel.kt` |
| 66 | Delayed game-over coroutines are untracked: `handleWrongButton` launches `delay(300) -> handleGameOver`, and `handleGameOver` launches `delay(2000) -> updateGameOverState`, neither stored in a Job. Entering Settings or switching mode inside that ~2.3 s window lets GameOver + `recordGameResult` land on the fresh run, and `onReturnToGame` then forces the stale `previousGameState` onto it. Track both in a `pendingGameOverJob` and cancel it in the reset paths (`setGameMode`, `setMemoryLightsPlusEnabled`, `setDailyChallengeEnabled`, `showSettings`, `onPause`, `onCleared`). Found Wave 12 while fixing input-lock and Settings-freeze bugs. | ✅ Wave 15 (2026-11-25): both steps (and the timeout's inner launch) go through `schedulePendingGameOver` (job + immediate finisher). Deviation from the plan above: `showSettings` and `onPause` *complete* the loss immediately instead of cancelling it, because cancelling leaves `isInputLocked` set and a `PlayerRepeating` snapshot (frozen turn, then a timeout game over). Mode switches and `onCleared` cancel. `pauseGame` is ignored while a game over is pending. 5 new tests; emulator-verified. | S | `viewmodels/SimonGameViewModel.kt` |
| 67 | Timeout path plays the error tone twice (`startTimeoutTimer` then `handleGameOver`), while the wrong-press path plays it once. Pick one. | ✅ Wave 15: dropped the call in `startTimeoutTimer`; `handleGameOver` plays it once. 300 ms delay kept so timing matches a wrong press. Test + emulator log (one error tone per timeout). | S | `viewmodels/SimonGameViewModel.kt` |
| 68 | Practice mode (F15) does not cover timeouts: `startTimeoutTimer` calls `handleGameOver` directly, so an idle player in practice mode still gets GameOver + statistics. Product question: replay instead? | ✅ Wave 21 (2026-11-27): `startTimeoutTimer` delegates to shared `handleTurnFailed` (clearing `timeoutJob` first to avoid self-cancellation), so timeouts in Practice Mode play the error tone once, clear `playerSequence`, preserve level/stats/high score, and replay the sequence (including Settings/background deferral). Updated `toggle_practice_summary` string + 2 new `SimonGameViewModelTest` cases. | S | `viewmodels/SimonGameViewModel.kt`, `res/values/strings.xml`, `SimonGameViewModelTest.kt` |
| 69 | `MainActivity.checkSoundResources()` / `checkResource()` is startup debug logging that only knows the standard and funny packs; it logs "Resource missing" for the other five packs on every launch, including release. Either delete or gate on `BuildConfig.DEBUG` and use `SimonSoundManager`'s full mapping. Also dead: `SimonSoundManager.vibrate()` legacy `SDK_INT < O` branch (minSdk 31) and the no-arg `handleGameOver()` overload in the ViewModel. | ✅ Wave 18: gated `checkSoundResources()` on `BuildConfig.DEBUG` and routed lookups through shared `SimonSoundManager.getResourceId()` across all available `SoundPack.entries`; deleted dead `SDK_INT < O` branch in `SimonSoundManager.vibrate()` and dead no-arg `handleGameOver()` overload in `SimonGameViewModel`. | S | `ui/MainActivity.kt`, `data/manager/SimonSoundManager.kt`, `viewmodels/SimonGameViewModel.kt` |
| 70 | `View.announceForAccessibility` is deprecated as of compileSdk 37 (5 call sites in `SimonGameScreen`: level-up, your turn, game over, paused, high score). Still works. Replacement is a Compose live region (`Modifier.semantics { liveRegion = LiveRegionMode.Polite }` on a node whose text changes) or a pane title; verify with TalkBack, since live regions announce on content change, not on demand. Found Wave 13. Wave 14 note: the `LaunchedEffect(uiState.gameState)` announcements fire on every (re)composition of the Game route, and with predictive back (targetSdk 36) NavHost composes the Game route while a back swipe from Settings is still in progress (confirmed by a mid-gesture screenshot: Game route visible behind the shrinking Settings card at 36, absent at 35), so a cancelled swipe may announce "Your turn"/"Paused". Not verified with TalkBack; the live-region migration should remove it. | ✅ Wave 20 (2026-11-27): replaced all `View.announceForAccessibility` calls with a `Modifier.semantics { liveRegion = LiveRegionMode.Polite }` node in `SimonGameScreen.kt` (shipped in `1.0.4 / 5`) | S-M | `ui/screens/SimonGameScreen.kt` |
| 71 | targetSdk is still 35. Google Play requires new apps and updates to target API 36 since Aug 2026, so this blocks Phase 4 upload. API 36 behavior changes to check: predictive back on by default, orientation/resizability restrictions ignored on large screens (sw600dp+), edge-to-edge opt-out removed. Needs an API 36+ emulator pass (Pixel_10 is API 37). Found Wave 13. | ✅ Wave 14 (2026-11-25): `targetSdk = 36`, no other code change needed. Only predictive back applies (no `enableOnBackInvokedCallback` opt-out; back is all NavHost, no `BackHandler` anywhere). Edge-to-edge and large-screen changes are no-ops (already `enableEdgeToEdge()`, no orientation/resize restrictions declared). Verified on Pixel_10 against a targetSdk 35 baseline; details in "Wave 14". | S | `app/build.gradle.kts` |
| 72 | Center-disc text collides when the disc is small relative to the text: in landscape on a phone, "YOUR TURN" overlaps the level number and the "Standard" pack tag; at sw720dp (density override) "Tap to start" overlaps "High: N". The landscape overlap reproduces identically at targetSdk 35, so it is not an API 36 regression; the sw720dp case was only seen at 36 (the 35 run had no high score to show), but density scaling does not depend on targetSdk. Matters more now that sw600dp+ devices ignore orientation locks. Found Wave 14. | ✅ Wave 19 (2026-11-27): Option A layout redesign moves pack/best metadata into top HUD strips (`SlimMetadataStrip` / `FullTopHudStrip`), removes the 6-tile center circle, bounds font scaling inside the `104.dp` 4-tile disc, and places `HeadlineOverlay` inside upper non-overlapping layout slots. Verified in `Wave19UiTest` (portrait `360x640`, compact landscape `800x350`, `sw720dp` `720x1024` at `2.0x` font scale) and live `Pixel_10` screenshots. | S | `ui/screens/SimonGameScreen.kt`, `ui/components/{CenterDiscContent,TopHudStrip,HeadlineOverlay}.kt` |
| 73 | Same race as #66 in the practice-mode miss path: `handleWrongButton` (practice branch) launches an untracked `delay(300)` then `showSequence()`. A practice miss followed by opening Settings within 300 ms replays the sequence and arms the timer on the Settings screen. Also minor: `MemoryLightsGame` captures `gameStateAtSettingsEntry` before `showSettings()` completes a pending loss, so the mid-game mode-switch confirmation can appear right after a loss. Found Wave 15 (from reading the code; not reproduced). | ✅ Wave 15 (`c71e301`): reproduced by 3 tests (Settings within 300 ms replayed and timed out on the Settings screen; backgrounding kept the missed press; pause was overwritten). The replay now uses the tracked turn-end step (renamed from pending game over); leaving the Game screen parks the turn in `ShowingSequence` so it replays on return. `gameStateAtSettingsEntry` is read after `showSettings()`. Emulator-verified. | S | `viewmodels/SimonGameViewModel.kt`, `ui/screens/SimonGameScreen.kt` |
| 74 | Update Play Store listing title to **"Memory Lights: Sequence Game"** (28/30 chars; post-approval store listing update — sync `playstore/listing.md` and Play Console main store listing). | open | S | `playstore/listing.md`, Play Console |
| 64 | No sequence-progress "3 / 7" indicator during repeat — players lose count at high levels. Add `playerSequence.size / sequence.size` text below the center disc when `gameState == PlayerRepeating`. | ✅ Phase 5 step 4 (small "X / Y" Text rendered under the level number inside the center disc Column when `PlayerRepeating` and sequence non-empty; verified live: layout dump shows "0 / 1") | S | `ui/screens/SimonGameScreen.kt` |

### Done (verified — closing the loop on the original 2025 audit)

✅ Mutable map → immutable map · TAG companion constants · `entries` over `values()` · unused import removed · `dynamicColor && true` fixed · GameConstants object created · content descriptions / state descriptions / role / 48dp touch targets · screen-reader announcements (level, your-turn, game-over) · sound loading state + indicator · `derivedStateOf` for `availableButtons` · lazy sound-pack loading · `ComponentCallbacks2` memory-trim · audio focus + listener · graceful fallback to GREEN tone · master volume control · `SettingsRepository` interface + impl · DataStore migration with auto-migrate-from-SharedPreferences · 28 unit tests for domain/state/constants.

⏭️ Skipped (decision documented in `progress.md`): split `GamePlayState` / `SettingsState` (low recomposition benefit for this app size).

---

## Feature Backlog (multi-agent scan, 2026-05-02)

Product features that are not strictly audit fixes — sequenced separately so engineering polish doesn't block product direction. Pick which to ship in v1.0 vs v1.1+ before Phase 4 (Play Store prep). Numbered F1-F18 to keep them visually distinct from audit items #1-#64.

### Game variants

| # | Feature | Effort | Notes |
|---|---|---|---|
| F1 | **Reverse Mode** — watch then repeat backwards | ✅ Phase 6 step 5 (2026-05-03): `reverseModeEnabled` DataStore key + `AppSettings`/`UiState` field + `setReverseModeEnabled` setter. `checkSequenceMatch` compares against `sequence[size-1-index]` when flag is set; the display path is intentionally unchanged so the asymmetry (watch forward, recall backward) is the entire puzzle. Settings card with snackbar. Verified on emulator: with reverse on, pressing the displayed sequence forward triggers `Wrong button selected! Expected: <last>, Got: <first>`; pressing in reverse advances levels normally. | S | `data/repository/SettingsRepository.kt`, `domain/model/SimonGameUiState.kt`, `viewmodels/SimonGameViewModel.kt`, `ui/screens/SettingsScreen.kt`, `ui/screens/SimonGameScreen.kt` |
| F3 | **Audio-Only Mode** — hide button colors during sequence playback | ✅ Phase 7 step 2 (2026-05-03): `audioOnlyModeEnabled` DataStore key + `AppSettings`/`UiState` field + setter. Suppression done at the UI layer: `displayLit = if (audioOnlyModeEnabled && gameState == ShowingSequence) null else currentlyLit` — keeps the watch phase dark while sound still fires through `soundManager.playSound` on every step, and player presses still light up because that path runs in `PlayerRepeating`. Game-over flash (`allButtonsLit`) intentionally not gated. New ToggleCard in `GameModesScreen`. Verified on Pixel_8_API_34: toggled on, started a fresh blitz run, watch phase played silently with no panel highlights, "YOUR TURN" landed correctly. | M | `data/repository/SettingsRepository.kt`, `domain/model/SimonGameUiState.kt`, `viewmodels/SimonGameViewModel.kt`, `ui/screens/GameModesScreen.kt`, `ui/screens/SimonGameScreen.kt`, tests |
| F4 | **Speed Blitz Mode** — fixed-length sprint (e.g. 20 buttons) with leaderboard time | ✅ Phase 7 step 2 (2026-05-03): new `domain/enums/GameMode` enum (`CLASSIC` / `SPEED_BLITZ`), `GameConstants.BLITZ_TARGET_LEVEL = 20`, two new DataStore Long keys for best blitz time per button-count (4-button + 6-button slots are independent — keeps per-mode bests honest). `setGameMode(GameMode)` resets the run end-to-end (level, sequence, blitz timer fields) and persists. Blitz start time captured lazily in `showSequence` when `level==1 && blitzStartTimeMs==0L` so the startup animation isn't included. `advanceToNextLevel` short-circuits to `handleBlitzWin` once `newLevel > BLITZ_TARGET_LEVEL`; win path computes `elapsedMs = now - start`, persists best if lower (or first), routes through GameOver with `blitzWon=true` so the existing HIGH SCORE / GAME OVER overlays render `BEST TIME!` / `VICTORY!` (green) instead. Loss path captures `blitzElapsedMs` but skips the classic high-score slot — blitz is time-based, mixing into level-based highScore would be wrong. Stats (`recordGameResult`) deliberately not touched on blitz win to keep classic streak/avg meaningful. New `BlitzHud` composable at top center renders `Lvl X / 20 • M:SS.s • Best M:SS` while `gameMode == SPEED_BLITZ` and the run is active; ticks at 10 Hz from a `LaunchedEffect` reading `System.currentTimeMillis()` (kept out of UiState so the VM isn't emitting at 10 Hz). GameOver disc shows `formatBlitzTime(blitzElapsedMs)` instead of level number when blitz. New chip-group "Game Mode" card in `GameModesScreen` (Classic / Speed Blitz) with the same mid-game confirm-dialog pattern as the ML+ toggle. Verified on Pixel_8_API_34: toggled on with Audio-Only on, HUD ticked from 0:00.0 upward, wrong tap landed on GameOver with disc showing "0:11.9" — final elapsed preserved, Replay/Share still available. | M | `domain/enums/GameMode.kt` (new), `domain/GameConstants.kt`, `data/repository/SettingsRepository.kt`, `domain/model/SimonGameUiState.kt`, `viewmodels/SimonGameViewModel.kt`, `ui/screens/GameModesScreen.kt`, `ui/screens/SimonGameScreen.kt`, `ui/screens/SettingsScreen.kt` (NavCard summary), tests |
| F5 | **Smoother difficulty curve** (logarithmic) | ✅ Phase 6 step 5 (2026-05-03): replaced the four-level stair-step (`(level-1)/DIFFICULTY_INTERVAL` reductions of 20% each) with a smooth log curve in `domain/SequenceTiming.kt`. Levels 1–4 still pin to base; from L5 onward `factor = 1 - 0.7 * ln(eff+1) / ln(26)` where `eff = level - 4`. Existing `MIN_LIT_DURATION_MS` / `MIN_PAUSE_DURATION_MS` floors still cap the fast end. The Speed Up overlay still fires at 5/9/13/... — useful UX milestones even with a continuous curve. `SequenceTimingTest` rewritten: pinned values at L5 (510/340) / L9 (369/246) / L13 (303/202), monotonic-decrease property test across L5–L15, base + clamp tests preserved. | S | `domain/SequenceTiming.kt`, `app/src/test/java/.../domain/SequenceTimingTest.kt` |

### Engagement / retention

| # | Feature | Effort | Notes |
|---|---|---|---|
| F6 | **Daily Challenge** — deterministic seed `Random(date.toEpochDay())` | ✅ Phase 8 step 2 (2026-05-03): `dailyChallengeEnabled` toggle in `GameModesScreen` + DataStore key. ViewModel holds a `dailyRandom: Random?` for the entire run; reseeded on every fresh-run entry point (`initializeNewGame`, `setMemoryLightsPlusEnabled`, `setGameMode`, the daily toggle itself) with `LocalDate.now().toEpochDay()` so successive `generateNextSequence` calls pull from the seeded stream — same calendar day → same sequence for every player. `currentEpochDay: () -> Long` indirection lets tests pin the seed. On game-over, when daily mode is on, persists `dailyCompletedEpochDay` + `dailyBestLevel` (rolls over when the day changes). Toggle subtitle leads with "Today's best: level N" once a run is recorded. Verified live on Pixel_8_API_34: enabled mode, run produced `BLUE`; force-stopped, relaunched — first button still `BLUE`. New ViewModel test seeds two VMs with same epoch day and asserts identical first sequence; new repo test asserts `setDailyCompletion` writes both fields atomically. 51 unit tests, all green. | M | `data/repository/SettingsRepository.kt`, `domain/model/SimonGameUiState.kt`, `viewmodels/SimonGameViewModel.kt`, `ui/screens/{SimonGameScreen,GameModesScreen}.kt`, tests |
| F7 | **Win-streak tracking** | ✅ Phase 6 step 5 (2026-05-03): re-defined `bestStreak` to mean "consecutive games where each beat the previous game's level," not "longest sequence ever seen" (which was indistinguishable from high score). Added two persisted DataStore keys (`current_streak`, `previous_level`) so the count survives app death; `recordGameResult` increments `currentStreak` when score > previousLevel else resets to 0, then `bestStreak = max(bestStreak, currentStreak)`. First game of a fresh run counts as 1. `StatisticsRepositoryTest` got a new test (`streak counts consecutive level improvements not raw sequence length`) plus an updated multi-game scenario. Already wired into `StatisticsScreen` via existing `statistics.bestStreak` row. | S | `data/repository/StatisticsRepository.kt`, `app/src/test/java/.../data/repository/StatisticsRepositoryTest.kt` |
| F8 | **Local achievements / badges** — milestone rewards | ✅ Phase 12 (2026-05-17 Wave 5): two-commit delivery. **Step 1** (`4ec8c80`, prior wave): new `Achievement` enum (`FirstWin`, `Level10`, `Level25`, `Streak3`, `Streak7`, `BlitzChampion`, `Speedrun30s`, `DailyDone`) with a pure `evaluate(level, gamesPlayed, bestStreak, blitzWon, blitzElapsedMs, dailyCompleted): Set<Achievement>` companion. Persisted as `Set<String>` (`Achievement.name`) in a new `KEY_UNLOCKED_ACHIEVEMENTS = stringSetPreferencesKey("unlocked_achievements")` DataStore key — data layer never imports the UI-facing enum. Unknown ids on read are silently dropped (forward-compat for removed badges). 11 new strings in `strings.xml` (heading + counter + empty-state + 8 title/desc pairs). **Step 2** (`0a59583`, this wave): UI + VM wiring. `SimonGameUiState.unlockedAchievements: Set<String>` hydrated from the settings flow. `evaluateAndPersistAchievements` is called from both `handleBlitzWin` (passes `BLITZ_TARGET_LEVEL` + elapsedMs so `Speedrun30s` sub-30s fires) and `updateGameOverState` (passes `currentLevel`, `gamesPlayed + 1`, `dailyChallengeEnabled`). Streak-based unlock fires on the following game-over because the repo owns the previous-level cursor we can't project from the VM — acceptable vs. duplicating the streak math. Union-merge + early-return when nothing's new, so zero writes once every badge is earned. New `AchievementsSection` composable on `StatisticsScreen`: star-prefixed heading with "N / 8 unlocked" counter, empty-state line when N=0, one Card per Achievement (in enum declaration order). Locked rows = Lock icon + grey tint; unlocked = yellow CheckCircle. Each Card has a TalkBack `contentDescription` that announces lock state + description. 4 new VM tests (FirstWin at level 1; FirstWin + DailyDone together on daily completion; hydration from AppSettings on init; re-unlock-already-stored no-op). DataStore round-trip is covered by `DataStoreSettingsRepositoryTest` shipped in step 1. 80/80 tests green, `./gradlew assembleDebug` succeeds. | M | `domain/model/Achievement.kt` (new), `domain/model/SimonGameUiState.kt`, `data/repository/SettingsRepository.kt`, `ui/viewmodels/SimonGameViewModel.kt`, `ui/screens/StatisticsScreen.kt`, `ui/screens/SimonGameScreen.kt`, `res/values/strings.xml`, new tests in `SimonGameViewModelTest.kt` + `DataStoreSettingsRepositoryTest.kt` |
| F9 | **Replay last sequence after game-over** ★ | ✅ Phase 6 step 1 (2026-05-03): `VM.replayLastSequence` keeps `sequence` + `level`, clears player state + overlay flags, drives existing `showSequence` path. New `OutlinedButton("Replay last sequence")` overlay at GameOver bottom-center, hidden via `AnimatedVisibility` everywhere else. Verified on emulator: GameOver → tap → same `Showing sequence item 0: BLUE` → PlayerRepeating with countdown ring. |
| F10 | **Share score card image** — Android share sheet | ✅ Phase 6 step 6 (2026-05-03): GameOver overlay now hosts a side-by-side **Replay** + **Share** Row. Tapping Share invokes `ScoreShareHelper.shareScore` which renders a 1080×1080 PNG via native `android.graphics.Canvas` (deliberately not a Compose-to-Bitmap path — sidesteps recomposition timing surprises and `rememberGraphicsLayer` API churn), writes it to `cacheDir/shared/memory_lights_score.png`, builds an `ACTION_SEND` Intent with `image/png` MIME + `EXTRA_STREAM` URI from `androidx.core.content.FileProvider` + `EXTRA_TEXT` caption ("I scored level X in Memory Lights! Best: Y"), and fires through `Intent.createChooser`. Required `clipData = ClipData.newRawUri("", uri)` on the source intent + `FLAG_GRANT_READ_URI_PERMISSION` on the chooser to propagate URI permission to the system Sharesheet preview process — without `clipData` the preview hits Permission Denial. New `<provider>` entry in `AndroidManifest.xml` with authority `${applicationId}.fileprovider` + `res/xml/file_paths.xml` mapping `cache-path shared/`. Card layout: green "MEMORY LIGHTS" header, six-color disc row (4 in standard mode, 6 in ML+), "LEVEL REACHED" label, big white level number, "Best: N", green mode badge ("STANDARD MODE" or "MEMORY LIGHTS+"), subtle dark border. Verified live on Pixel_8_API_34: triggered GameOver, tapped Share, Sharesheet rendered "Sharing image" + caption + destinations (Quick Share/Drive/Maps/Messages), pulled cache PNG, confirmed `1080 x 1080 8-bit RGBA PNG` via `file(1)`. | M | `ui/share/ScoreShareHelper.kt` (new), `ui/screens/SimonGameScreen.kt` (Share button + Replay→Row), `AndroidManifest.xml` (provider), `res/xml/file_paths.xml` (new) |
| F11 | **Google Play Games Services leaderboard** | L | GPGS SDK + Console project + sign-in flow. **Post-launch v1.1.** |

### Customization / quality-of-life

| # | Feature | Effort | Notes |
|---|---|---|---|
| F12 | **Light / AMOLED theme toggle** | ✅ Phase 9 step 2 (2026-05-05): ships as **AMOLED Black / Dark** toggle (Light deferred to v1.1 — game screen needs Color.White/Gray audit first). New `ThemeMode` enum (AMOLED, DARK), DataStore key, `AppSettings.themeMode`, `UiState.themeMode`, `VM.setThemeMode`. Two `ColorScheme` instances in `Color.kt`: `AmoledColorScheme` (#000 / #1D1D1D / #303030 surface stack matching prior look) and `DarkMaterialColorScheme` (#141218 / #211F26 / #2B2930 tinted gray). `MyApplicationTheme(themeMode)` picks the scheme; MainActivity collects `uiState` into Compose so changes propagate live. 14 surface-token callsites + 12 Scaffold/TopBar backgrounds migrated to `MaterialTheme.colorScheme.surface*` / `.background` so they swap with the scheme. Game-disc center stays `Color.Black` in both modes (panel contrast). New `ThemeCard` chip-group in Settings hub above the divider, snackbar feedback ("Theme: Dark"). Verified on Pixel_8_API_34: Dark switches surfaces from #1D1D1D → #2B2930; persists across force-stop/relaunch. | S | `domain/enums/ThemeMode.kt` (new), `data/repository/SettingsRepository.kt`, `domain/model/SimonGameUiState.kt`, `viewmodels/SimonGameViewModel.kt`, `ui/MainActivity.kt`, `ui/theme/{Theme,Color}.kt`, `ui/screens/SettingsScreen.kt` (+ ThemeCard composable), all other `ui/screens/*.kt` (Scaffold + surface migration) |
| F13 | **Per-button color picker** (colorblind a11y) | L | Replaces / complements audit item #35. Requires per-button override map in DataStore. |
| F14 | **Explicit pause button** + new `GameState.Paused` | ✅ Phase 6 step 3 (2026-05-03): `GameState.Paused` added. `pauseGame()` (no-op outside `PlayerRepeating`) cancels the inactivity timer + sets state. `resumeGame()` (no-op outside `Paused`, also gated on `isAppInForeground`) flips back and calls `startTimeoutTimer` — timer restarts at the player's full configured timeout, not the remaining time (simpler model; pause is rare). Top-bar `Pause` IconButton appears only during `PlayerRepeating`. Full-screen 70%-alpha black overlay during `Paused` shows a 96-dp play arrow + `PAUSED` label + "Tap to resume" hint; entire backdrop is one click target so any tap resumes. Settings `hasActiveGame` includes `Paused` so the ML+ confirm-dialog still fires. `exitSettings` restores `Paused` if the player entered Settings while paused. Verified on emulator: PlayerRepeating → tap pause → overlay shown, Pause icon hidden, ring stops drawing → tap overlay → back to PlayerRepeating, ring re-drains. New `res/drawable/pause_24px.xml` + a11y announcement "Game paused." | S | `domain/model/SimonGameState.kt`, `viewmodels/SimonGameViewModel.kt`, `ui/screens/SimonGameScreen.kt`, new `res/drawable/pause_24px.xml`, `app/src/test/java/.../domain/model/GameStateTest.kt` |
| F15 | **Practice mode** — wrong button doesn't end game | ✅ Phase 6 step 4 (2026-05-03): `practiceModeEnabled` DataStore key + `AppSettings`/`UiState` field + setter. Extracted shared `handleWrongButton(reason)` helper from `checkSequenceMatch`'s out-of-bounds and mismatch branches; in practice mode it plays the error tone, brief delay, clears `playerSequence`, and re-runs `showSequence` instead of `handleGameOver`. Settings toggle card with snackbar. Scope intentionally narrow: timeouts still end the run (toggle text says "Wrong buttons replay the sequence" — clear on scope). High scores never advance in practice mode because game-over → `recordGameResult` path is never taken. New `SimonGameViewModelTest`: `practice mode wrong button replays sequence instead of game over` asserts `recordGameResult`/`setHighScore` never fire. Verified on emulator: toggled on, tapped RED when expecting GREEN, logcat shows "Practice mode: Wrong button pressed — replaying sequence" → state stays PlayerRepeating after replay. | M | `data/repository/SettingsRepository.kt`, `domain/model/SimonGameUiState.kt`, `viewmodels/SimonGameViewModel.kt`, `ui/screens/SettingsScreen.kt`, `ui/screens/SimonGameScreen.kt`, tests |
| F16 | **Adjustable timeout slider** (5 / 10 / 15 / 30) | ✅ Phase 6 step 2 (2026-05-03): `playerTimeoutSeconds` DataStore key + `AppSettings` field (default 10) + per-key setter. `SimonGameUiState.playerTimeoutMs` computed from seconds; `startTimeoutTimer` and the countdown ring both read it dynamically. Settings card shows a 4-FilterChip segmented control (5s / 10s / 15s / 30s) with snackbar feedback; mid-turn change resets the in-flight timer via `resetTimeoutTimer` so the ring re-drains immediately. Verified on emulator: select 5s → snackbar fires → kill app → relaunch → 5s still selected. ∞ option dropped from spec — rare use case, would complicate the ring drain animation; can revisit if user requests. | S | `data/repository/SettingsRepository.kt`, `domain/model/SimonGameUiState.kt`, `viewmodels/SimonGameViewModel.kt`, `ui/screens/SettingsScreen.kt`, `ui/screens/SimonGameScreen.kt`, new `res/drawable/schedule_24px.xml` |
| F18 | **Split Settings into sub-screens** (Game Modes / Gameplay / Sound & Haptics) | ✅ Phase 7 (2026-05-03): two-step delivery. **Step 1** (commit `8e7c733`): NavHost migration — replaced flat `ScreenState` sealed class with `androidx.navigation:navigation-compose 2.8.5` and `@Serializable` route objects (`GameRoute`, `SettingsRoute`, `StatisticsRoute`); ViewModel keeps side-effect methods (`showSettings`, `showStatistics`, `onReturnToGame`) and UI owns `NavController`; `LaunchedEffect(currentDestination)` fires `onReturnToGame()` whenever `NavDestination.hasRoute<GameRoute>()`. Removed the hand-rolled `OnBackPressedCallback` from MainActivity — NavHost handles back via the activity's OnBackPressedDispatcher. Added kotlinx-serialization plugin + JSON runtime. **Step 2** (commit `db7781a`): three new sub-screen routes (`GameModesRoute`, `GameplayRoute`, `SoundAndHapticsRoute`) and three new screen composables. `GameModesScreen` hosts Difficulty / Reverse / Practice / Memory Lights+ (mid-game switch confirm dialog moved here with the ML+ toggle, plumbed through `gameStateAtSettingsEntry`). `GameplayScreen` hosts the Player Timeout chip group (sized to absorb upcoming F3/F4/F6/F8 toggles). `SoundAndHapticsScreen` hosts the sound-pack LazyColumn with the same scroll-fade affordances; vibration toggle deliberately not duplicated here since the Game screen's top-bar icon is the single canonical control. Top-level `SettingsScreen` becomes a hub — three chevron NavCards above a divider, then Statistics / Reset / Rate / About below it. SettingsScreen.kt shrunk ~860 → ~370 lines, parameter list dropped 15 → 6 props. Verified on emulator: each sub-screen renders with persisted state intact; back-stack walks Sub-screen → Settings → Game with `Resuming from PlayerRepeating state` in logcat. 50 tests green, lint clean. | M | `ui/navigation/Routes.kt`, new `GameModesScreen.kt`/`GameplayScreen.kt`/`SoundAndHapticsScreen.kt` under `ui/screens/`, rewritten `SettingsScreen.kt`, `SimonGameScreen.kt` (NavHost), `SimonGameViewModel.kt`, `MainActivity.kt`, `SimonGameUiState.kt` (dropped `screenState`), `SimonGameState.kt` doc, `app/build.gradle.kts`, root `build.gradle.kts`, `gradle/libs.versions.toml`, deleted `ScreenStateTest.kt` |

### Monetization (decision required — discuss before code)

| # | Feature | Effort | Notes |
|---|---|---|---|
| F17 | **Premium sound packs IAP bundle** | XL | Single "Unlock All" IAP for Musical/Nature/Sci-Fi packs once they ship. **Requires explicit ads-vs-IAP product decision before any billing code.** Not before Phase 3 sound assets exist. |

★ = recommended high-impact / low-effort starting point.

---

## Sound Track — Implement the Three Missing Sound Packs

Each pack needs **7 files** at 22 050 Hz mono WAV, ~150–400 ms each: `green`, `red`, `yellow`, `blue`, `purple`, `orange`, `error`. Total = **21 new files**. Drop into `app/src/main/res/raw/` with naming `{prefix}_{color}_tone.wav`.

### Step 1 — Update `SoundPack.resourcePrefix`
File: `app/src/main/java/.../domain/enums/SoundPack.kt:32-46`. Change MUSICAL → `"musical"`, NATURE → `"nature"`, SCI_FI → `"scifi"`. (Use `scifi` not `sci_fi` — Android raw resource names must be lowercase + underscores; `scifi` keeps it short and matches existing single-token prefixes.)

### Step 2 — Pack design (musical theme of each pack)

**Musical** — pitched instrument notes mapped to a pentatonic scale to stay consonant in any sequence:
- Suggested mapping: green = C5 piano, red = E5 piano, yellow = G5 piano, blue = A5 piano, purple = D5 guitar pluck, orange = F#5 flute, error = dissonant minor 2nd cluster.

**Nature** — short organic samples:
- green = bird chirp, red = water drop, yellow = wind chime ding, blue = frog ribbit, purple = cricket, owl, or rustling leaves, orange = soft thunder rumble (short), error = hawk screech or branch snap.

**Sci-Fi** — synth FX:
- green = ascending bleep, red = descending bleep, yellow = modulated square wave, blue = filter sweep, purple = laser zap, orange = warp/whoosh, error = klaxon or alarm buzz.

### Step 3 — Source vs. generate (decision matrix)

| Approach | Pros | Cons | Best for |
|---|---|---|---|
| Royalty-free libraries (Freesound CC0, Pixabay, OpenGameArt, Kenney.nl) | Real recordings, professional quality | License audit per file (CC0 vs CC-BY vs other), inconsistent loudness, must trim | Nature pack |
| Procedural synth via `sox` / `ffmpeg` (sine + envelope) | Fully controllable, zero license risk, scriptable | Sounds clinical | Musical pack (pure tones), Sci-Fi (with FM/filter) |
| Browser tools: ChipTone, jsfxr, Bfxr | Instant, retro-style, exportable WAV | Sci-Fi only — not musical | Sci-Fi pack |
| Python with `numpy` + `scipy.io.wavfile` | Total control over envelope, ADSR, harmonics | Requires Python deps | Musical pack (additive synthesis) |
| AI generators: ElevenLabs SFX API, Suno (with care) | Fast iteration, varied output | Cost, license terms vary, quality inconsistent for short FX | Backup option |

**Recommended path:**
- **Musical:** generate with a 30-line Python `numpy` script (sine + 2 harmonics + exponential decay envelope) for clean, license-free pitched tones.
- **Sci-Fi:** generate with `jsfxr` (ChipTone) presets — purpose-built for game FX. Export WAV. Fully license-free.
- **Nature:** source from Freesound (filter to CC0). Trim to ≤400 ms with `sox` / `ffmpeg`. Keep license attributions in `app/src/main/assets/SOUND_LICENSES.md` even if CC0.

### Step 4 — Pipeline (one-time script)

Create `tools/sounds/` (gitignored intermediates, committed final WAVs):
```
tools/sounds/
├── musical/generate.py          # Python additive synth → 7 WAVs
├── scifi/                       # Drop jsfxr exports here
├── nature/sources.txt           # Freesound IDs + license per file
├── normalize.sh                 # sox normalize -1dB, trim silence
└── install.sh                   # cp final/*.wav app/src/main/res/raw/
```

Verification per file:
- 22 050 Hz mono 16-bit PCM (`soxi` to confirm).
- Duration ≤ 500 ms (game timing tolerates 600 ms lit + 400 ms pause; longer sounds would clip).
- Peak normalized to −1 dBFS.
- A/B against existing `standard_*` files for loudness parity.

### Step 5 — Update UI affordances

After packs ship: remove the "Coming Soon" badge work from item #13. Until they ship, item #13 stands.

### Step 6 — Tests / verification

- Add a `SimonSoundManagerTest` (instrumented) that asserts each `SoundPack.resourcePrefix` resolves to all 7 button + error resource IDs. This catches missing files at CI time.
- Manual: install on device, switch each pack in Settings, play a round, listen for fallback warnings in `logcat | grep com.happypuppy.memorylights`.

---

## Decisions (locked in)

- **Canonical audit doc:** this file (`specs/improvements.md`). Root `IMPROVEMENTS.md` deleted in Phase 0.
- **Sound production:** synth-first. Musical = Python `numpy` additive synth, Sci-Fi = `jsfxr`/ChipTone exports, Nature = Freesound CC0 (with license file).
- **Next external milestone:** real sound packs ship **before** Play Store launch.
- **Commit cadence:** ~4-5 logical commits per session (Session 5 split into 5 commits in Phase 0).

## Execution Sequence

1. **Phase 0 — Stabilize** ✅ DONE (2026-05-02)
   - Item #4: ✅ Session 5 work committed as 5 logical commits + pushed to origin/main:
     - `1c33fe5` Consolidate improvement docs and clean up orphan resources
     - `c60376d` Migrate persistence to DataStore with SettingsRepository
     - `14f06e6` Refactor SimonSoundManager: audio focus, lazy loading, memory awareness
     - `6a69f5a` Polish UI/state: a11y, immutable map, GameConstants, Compose hygiene
     - `55d9ee3` Add domain unit tests and test dependencies
   - Item #5: ✅ merged root `IMPROVEMENTS.md` into this file (root version is the canonical post-Session-5 audit, supersedes Dec 2025 version). Root copy deleted.
   - Item #14: ✅ deleted `app/src/main/res/raw/guy/blue_tone.wav` and empty `guy/` subdir.
   - Bonus: GitHub repo renamed `MyApplication2` → `MemoryLights`. Stripped leaked PAT (`ghp_QJH5...`) from `.git/config` — **manual TODO: revoke at https://github.com/settings/tokens**. Added `.claude/` to `.gitignore`.
2. **Phase 1 — P0 audit fixes** ✅ DONE (2026-05-02, single commit `8b2bd94`)
   - #1 R8 + Koin keep rules + Activity keep rules + Instantiatable lint suppress on MainActivity. APK 35.5 MB → 7.2 MB.
   - #2 DataStore reads off main thread via `settingsFlow` + `statisticsFlow`. Added `AppSettings` data class. ViewModel collects flows in `viewModelScope` on init; statistics flow continuous; settings load via `.first()` then runs startup animation. `recordGameResult` now read+update inside single `dataStore.edit` (no race).
   - #3 `activeButtonPresses` removed from `SimonGameUiState`; private `activePresses: MutableSet<SimonButton>` in ViewModel handles touch debounce. Dead `onButtonRelease` deleted.
   - Smoke-tested on Pixel_8_API_34 emulator: app launches, persisted high score loads correctly via Flow.
3. **Phase 2 — VM/repo tests then P1 fixes (2–3 days)** ⏭️ IN PROGRESS
   - **Step 1:** ✅ done — `SimonGameViewModelTest` (8 tests: sequence advance, wrong button, timeout, new high score, no-regress high score, resetHighScore, onPause/onResume, mode switch) + `SequenceTimingTest` (7 tests: difficulty off / L1-4 / L5 / L6-8 / L9 / monotonic / clamp). `calculateSequenceTiming` extracted to pure `domain/SequenceTiming.kt`, collapsing the #38 dead branch. `testOptions.unitTests.isReturnDefaultValues = true` added so `Log.d` calls in unit tests no-op.
   - **Step 2:** ✅ done — Turbine added to catalog. Both `DataStoreSettingsRepository` and `StatisticsManager` refactored to accept `DataStore<Preferences>` directly via primary constructor, with `fromContext(context)` factory used by the Koin module. Tests use `PreferenceDataStoreFactory.create` against a `TemporaryFolder` file. `DataStoreSettingsRepositoryTest` (6 tests: defaults, soundPack, scores, multi-toggle, mode-switch) and `StatisticsManagerTest` (4 tests: empty, recordGameResult monotonicity, resetStatistics, averageScore). 53 unit tests total, all green.
   - Step 2: add `Turbine` to catalog; flow tests for `DataStoreSettingsRepository` and `StatisticsManager` (#12).
   - Step 3: ✅ P1 refactors — #6 (drop `GameStatistics.highScore`; SettingsRepository owns per-mode score), #7 (delete `GameState.Settings`; back-press driven by `ScreenState`), #8 (`VM.setSoundPack` plays preview, removes `koinInject` from SettingsScreen), #9 (drop unsafe `viewModel as DefaultLifecycleObserver` cast), #10 (`BuildConfig.DEBUG` guards on reflection helpers; `buildConfig = true` enabled), #15 (HIGH SCORE a11y announcement).
   - Step 4 (multi-agent fixes — code-only subset): ✅ #40 (drop test buzz from `setVibrationEnabled`), #41 (drop card-level click on the Settings reset card; inner button is sole trigger), #43 (delete `saveSettings()`; per-setting writes; no flush in `onCleared`), #45 (rename to "Reset Score & Statistics").
   - Step 5 (multi-agent fixes — device-verified on Pixel_8_API_34): ✅ #39 (Warning icon + message replaces silent spinner when `soundLoadError != null`), #42 (themes.xml `windowBackground` + system bars set to black so splash matches the all-black UI), #44 (mode-switch confirm dialog when toggling Memory Lights+ during active game; verified: dialog shown, Cancel keeps mode unchanged), #46 (game-over center disc stacks final score / best score / play arrow). Smoke-tested through Settings → ML+ toggle → Cancel flow.
   - Step 4: P1 multi-agent finds — fix #38 (dead `if` branch in difficulty), #39 (render `soundLoadError`), #40 (vibration test-buzz side-effect), #41 (settings card double-tap), #42 (splash white flash), #43 (saveSettings write storm), #44 (mid-game mode-switch confirm), #45 (reset card title clarity), #46 (game-over score summary).
   - Step 5: #13 "Coming Soon" badge — throwaway, removed when sound packs ship in Phase 3. Skip if Phase 3 starts immediately after.
4. **Phase 3 — Sound packs (2026-05-03)** ✅ DONE — all 3 packs synth-generated, no third-party samples (license-clean):
   - Musical: Python `numpy` additive synth (sine + 2 harmonics + ADSR), pentatonic piano-style: C5/E5/G5/A5/D5/F#5 + minor-2nd error cluster.
   - Sci-Fi: Python synth, chiptune retro: ascending/descending bleeps, modulated square, filter sweep, laser zap, warp whoosh, klaxon error.
   - Nature: Python synth approximations (Freesound/Pixabay CC0 sourcing requires account-gated downloads): bird chirp, water drop, wind chime, frog ribbit, cricket, thunder rumble, hawk screech.
   - 21 WAVs at 44100 Hz mono 16-bit PCM, peak −1 dBFS, 220–500 ms each.
   - `SoundPack.resourcePrefix` updated for all three; descriptions reflect actual content.
   - `SimonSoundManager.getResourceId` `when` table extended with 21 new entries.
   - Verified on Pixel_8_API_34 emulator: switching each pack triggers `Loading sound pack: X with prefix: x`, all 7 sounds `Successfully loaded`, no fallback warnings. Release APK builds at 7.8 MB (was 7.2 MB, +600 KB for new WAVs).
   - `tools/sounds/{musical,scifi,nature}/generate.py` deterministic — re-running produces byte-identical output. License notes in `tools/sounds/SOUND_LICENSES.md`.
5. **Phase 4 — Play Store prep (2026-05-03 — paste-ready bundle, signing pending):** ✅ paste-ready prep delivered in `playstore/` directory. Contents: `RELEASE.md` (keystore generation + Gradle signing config wiring + AAB build + Play Console upload steps), `listing.md` (app title, short/full descriptions, categorization, content rating notes, data safety form values, asset requirements), `privacy_policy.md` (full text — app collects/transmits zero data, hosting note for GitHub Pages), `feature_graphic.md` (spec + design notes for the missing 1024x500), `screenshots/` (7 PNG 1080x2400 captures: 4-button play, 6-button play, GameOver+Share, Settings hub showing F18 split, Sound & Haptics with all 7 packs, Game Modes with 4 toggles, Statistics with 5 distinct icons), `share_card_preview.png` (sample 1080x1080 score card from F10 ready as marketing visual). Item #23 (`rootProject.name`) already done in Phase 5 step 3; `versionCode 1` `versionName "1.0"` already set in `app/build.gradle.kts` (roadmap was stale on this). Still pending — user-action only: keystore generation, hosting privacy policy, designing the 1024x500 feature graphic, the actual Play Console submission.
6. **Phase 5 — P2 polish (ongoing post-launch):** items #16–#37 (existing audit) **plus #47–#64** (multi-agent finds: thread-safety annotation, strings.xml, color dedupe, dead `index` field, narrow ProGuard keep, lambda/brush `remember`, animated lit/overlay transitions, tap-to-start affordance, Statistics icons, Typography migration, ripple cleanup, persistent turn cue, timeout countdown, speed-up signal, varied preview, sequence-progress indicator).
7. **Phase 5 step 1 — P2 code-only cluster (2026-05-03):** ✅ #47 (`@MainThread` on `onButtonClick`), #49 (5 semantic dark surface tokens in `theme/Color.kt`), #50 (drop `SimonButton.index`), #52 (`remember`ed `handleButtonInteraction`), #53 (`remember`ed `buttonColors` + `buttonBrush` in `SimonPanel`). 50 unit tests green, lint clean.
8. **Phase 5 step 2 — P2 UX cluster (2026-05-03):** ✅ #51 (narrow ProGuard, release APK 7.84 → 7.48 MB), #54 (smooth `litProgress` lerp on flash), #55 (`AnimatedVisibility` on HIGH SCORE/GAME OVER/YOUR TURN overlays), #56 (WaitingToStart play-arrow + "Tap to start" affordance + scale pulse), #61 (timeout countdown ring around center disc, color shifts red→green by remaining fraction). Verified on Pixel_8_API_34 emulator + release APK signed with debug keystore launched cleanly with narrow Koin keep.
9. **Phase 6 step 1 — F9 Replay last sequence (2026-05-03):** ✅ `replayLastSequence` keeps existing sequence + level, drives the existing `showSequence` path. `OutlinedButton` shows at GameOver bottom-center; hidden in all other states via `AnimatedVisibility`. Highest-leverage retention loop for pattern-memory games — players can immediately retry the run they just lost on without losing their level progress.
16. **Phase 6 step 4 — F15 Practice Mode (2026-05-03):** ✅ DataStore-backed `practiceModeEnabled` toggle. Extracted `handleWrongButton(reason)` helper from `checkSequenceMatch`. In practice mode the wrong-button branch plays the error tone, briefly waits, clears `playerSequence`, and re-shows the same sequence — `handleGameOver` is bypassed entirely so high scores cannot advance and no statistics are recorded. Settings toggle (Switch) with descriptive subtitle and snackbar feedback. Timeouts intentionally still game over; the toggle copy ("Wrong buttons replay the sequence") sets that scope. New ViewModel test asserts `recordGameResult` + `setHighScore` are never called in this flow. Verified on Pixel_8_API_34: toggle on, intentional wrong tap → logcat shows "Practice mode: Wrong button pressed — replaying sequence" + state lands back in PlayerRepeating. 52 unit tests green, lint clean.

15. **Phase 6 step 3 — F14 Pause button (2026-05-03):** ✅ New `GameState.Paused`. `SimonGameViewModel.pauseGame()` cancels the inactivity timer; `resumeGame()` restarts it at full duration (simpler model — pause is rare and remembering "remaining ms" complicates the ring re-drain). Top-bar `Pause` IconButton appears only during `PlayerRepeating`; full-screen 70%-alpha overlay during `Paused` is the resume target. Settings `hasActiveGame` and `exitSettings` both handle `Paused` so opening Settings mid-pause restores the paused state on return. New `res/drawable/pause_24px.xml`, a11y announcement, and an updated `GameStateTest` exhaustive-when assertion. Verified on Pixel_8_API_34: pause → overlay → tap → resume cycle works; Pause icon visibility correctly tracks state.

14. **Phase 6 step 2 — F16 Adjustable player timeout (2026-05-03):** ✅ Adds a 5/10/15/30-second segmented control to Settings driving the inactivity timeout (and the existing #61 countdown ring). DataStore key + `AppSettings.playerTimeoutSeconds` (default 10) + per-key setter. `SimonGameUiState.playerTimeoutMs` computed from seconds; `SimonGameViewModel.startTimeoutTimer` and the ring `LaunchedEffect` both read it dynamically. Mid-turn change calls `resetTimeoutTimer` so the timer + ring re-drain at the new duration immediately. Verified on Pixel_8_API_34 emulator: select 5s → snackbar "Timeout: 5s" → kill app → relaunch → 5s still selected. New `res/drawable/schedule_24px.xml` (Material `Icons.Default.Schedule` not in this version's icon set). 51 unit tests green (added `setPlayerTimeoutSeconds persists` test), lint clean.
10. **Phase 5 step 3 — code-only cleanup batch (2026-05-03):** ✅ #16 (delete `getContext()` + `triggerParticleEffects()`), #18 (`withFrameMillis` in `ParticleEffect`), #21 (drop misleading `ERROR_SOUND_VOLUME_BOOST`; error tone uses `MAX_VOLUME`), #22 (`widthIn(max = 500.dp)` tablet cap), #23 (`rootProject.name` → `MemoryLights`), #25 (`SimonButton.displayName` + delete local `String.capitalize()`; sound pack indicator now shows "Sci-Fi" properly), #26 (drop unused `lifecycle-process`), #27 (narrow `catch (_: ActivityNotFoundException)` for Play Store intent), #28 (`dynamicColor = false` default).
11. **Phase 5 step 4 — PlayerRepeating UX feedback cluster (2026-05-03):** ✅ #60 (animated halo pulse persistent through full `PlayerRepeating` phase, not just rounds 0-2), #62 ("Speed Up!" overlay on difficulty-tier crossings — `showSpeedUpText` flag + `speedUpTextJob` auto-clear after `SPEED_UP_TEXT_DISPLAY_MS = 1200ms`), #64 ("X / Y" sequence-progress under level number inside center disc). Smoke-tested on Pixel_8_API_34: layout dump confirms "0 / 1", "YOUR TURN", halo visible during PlayerRepeating; no crashes through start → sequence → game over flow. 53 unit tests green, lint clean, build clean.
12. **Phase 5 step 5 — code-only quick wins batch (2026-05-03):** ✅ #19 (drop `Handler(Looper.getMainLooper()).post` indirection — direct `vibrate()` since callers are `@MainThread` and `Vibrator` is thread-safe), #20 partial (`remember(orientation)` wrap; full extraction tracked as #17), #30 (4-btn Green padding 2 → 4dp matches other panels), #31 (named `BiasAlignment` constants replace inline `.offset(y = ...)` on the four overlays), #57 (5 distinct Statistics row icons), #59 (`RadioButton.onClick = null` so Row click is the sole ripple source), #63 (`previewToneIndex` cursor cycles preview through available buttons). 53 unit tests green, lint clean, screenshots verified Statistics screen + game flow.
13. **Phase 5 step 6 — code organization + UX feedback (2026-05-03):** ✅ #29 (`StatisticsManager` → `data/repository/StatisticsRepository`; class + test + DI + VM field renamed; package layout now matches `SettingsRepository`), #33 (`SnackbarHost` added to Settings Scaffold; transient confirmation toast on sound-pack pick / difficulty toggle / ML+ confirm-dialog accept). 53 unit tests still green, lint clean, screenshot confirms snackbar "Difficulty: On" rendering.
18. **Phase 8 — Strings + Daily Challenge (2026-05-03):** ✅ #48 strings.xml extraction (~120 keys; `SoundPack/GameMode/SimonButton` enums gain `@StringRes` IDs; ScoreShareHelper threads Context through) and F6 Daily Challenge (seeded RNG per run + persisted completion). Tests + lint clean; verified live across Settings hub, Game Modes, GameOver overlay, daily run determinism across app restart.

17. **Phase 6 step 6 — F10 Share score card (2026-05-03):** ✅ GameOver overlay now hosts side-by-side Replay + Share buttons. New `ScoreShareHelper` renders a 1080×1080 PNG via native `android.graphics.Canvas` (deliberately not Compose-to-Bitmap — sidesteps `rememberGraphicsLayer` API churn and recomposition timing surprises), writes to `cacheDir/shared/`, fires `ACTION_SEND` through `Intent.createChooser` with `image/png` MIME + `EXTRA_TEXT` caption + `FileProvider` URI. Required `clipData = ClipData.newRawUri("", uri)` on the inner intent + `FLAG_GRANT_READ_URI_PERMISSION` on the chooser to propagate URI permission to the system Sharesheet preview process — without `clipData` the preview hits Permission Denial on the URI even though the source intent has the flag (Android Sharesheet runs in a separate UID). Manifest gains a `<provider>` entry with authority `${applicationId}.fileprovider`; new `res/xml/file_paths.xml` maps `cache-path shared/`. Card layout: green "MEMORY LIGHTS" header, four/six-disc colour row sized to active mode, big white level number, "Best: N", green mode badge, subtle dark border. Verified live: triggered GameOver, tapped Share, captured Sharesheet showing "Sharing image" + "I scored level 1 in Memory Lights! Best: 5" + Quick Share/Drive/Maps/Messages destinations; pulled the cache PNG and confirmed `1080 x 1080 8-bit RGBA` via `file(1)`. 53 tests green, lint clean.
8. **Phase 6 — Feature backlog (product roadmap, post-launch):** Feature Backlog items F1–F17. Recommended starting set: F9 (replay last sequence) + F14 (pause button) + F16 (adjustable timeout) — all S-effort, high impact. F17 (IAP) requires explicit ads-vs-IAP product decision before any code.

19. **Phase 9 — Typography + Theme toggle (2026-05-05):** ✅ #58 typography migration to `MaterialTheme.typography.*` (Type.kt redefined with full role-tuned scale; all six screens migrated; ~30 callsites; unblocks F12) and F12 Light/AMOLED theme toggle (ships as AMOLED Black / Dark — Light deferred to v1.1; new `ThemeMode` enum, two `ColorScheme` instances in Color.kt, surface-token migration to `MaterialTheme.colorScheme.surface*`, ThemeCard chip-group in Settings hub). Verified live on Pixel_8_API_34: Dark visibly tints surfaces; persists across force-stop. 51 tests green, lint clean.

20. **Phase 10 — Repository scope cleanup (2026-05-16):** ✅ #24 `CoroutineScope` cancellation. Both `StatisticsRepository` and `DataStoreSettingsRepository` previously held a `CoroutineScope(Dispatchers.IO)` that was never cancelled — leaked across Koin `stopKoin()` and instrumented-test teardowns. Refactored each to distinguish owned vs injected scopes via an `ownsScope` flag on a new `internal` 3-arg constructor; the public 2-arg constructor stays `ownsScope = false` so existing tests (which inject a `TestScope` and cancel it themselves) are untouched. `fromContext` factories now build `SupervisorJob() + Dispatchers.IO` and pass `ownsScope = true`. Added `close()` to the `SettingsRepository` interface; both impls cancel only the scopes they own. Koin module wires `single { ... }.withOptions { onClose { it?.close() } }` so the IO scopes release when `stopKoin()` runs. `SimonApp.onTerminate()` calls `GlobalContext.getOrNull()?.close()` for emulator/instrumented-test cleanup (no-op on real devices — process death already cleans up). New tests cover both ownership branches in each repo: scope cancelled when owned, scope left alone when injected. 64 unit tests green (was 60), lint clean, debug APK builds. | M | `data/repository/StatisticsRepository.kt`, `data/repository/SettingsRepository.kt`, `di/AppModule.kt`, `SimonApp.kt`, test files |

## Session 2026-05-16

Single-item background pass: closed the long-open audit item #24 (CoroutineScope leak in `StatisticsRepository` + `DataStoreSettingsRepository`). The repos owned their `Dispatchers.IO` scope but exposed no `close()`, so Koin `stopKoin()` and Android instrumented teardowns leaked the job. Fix is the standard "ownership flag on internal constructor" pattern: production `fromContext` owns the scope and cancels in `close()`; tests inject a `TestScope` and retain ownership. Wired through Koin's `onClose` DSL (Koin 4.x: `single { ... }.withOptions { onClose { ... } }`) plus `Application.onTerminate` for completeness. 4 new tests (2 per repo) verify both branches. 64 / 64 unit tests green, lint clean, debug build clean. No device verification needed — change is process-lifecycle only.

Remaining open audit items: #17 (extract `SimonButtonGrid`), #32 (onboarding), #34 (reduce-motion — previously cut as low-value), #35 (color-blind mode). Open features: F8 achievements, F11 GPGS leaderboard (post-launch v1.1), F13 per-button color picker, F17 IAP (decision pending).

Next batch recommendation: **#17 SimonButtonGrid extract** — the `SimonGameScreen` is now ~750 lines and a target-of-opportunity refactor. Risk is low (pure composable extraction) and unblocks future feature work that touches button rendering.

## Session 2026-05-16 — Wave 2

Single-item background pass: closed audit #17 (and subsumed #20). Extracted `SimonButtonGrid` from `SimonGameScreen` — the inline grid had three near-identical layouts (6-button landscape, 6-button portrait, 4-button classic) totalling ~180 lines of repeated `SimonPanel(...)` construction. The new `ui/components/SimonButtonGrid.kt` (246 lines, single responsibility) owns layout selection, orientation reading, and per-mode panel padding (4dp classic, 2dp 6-button — preserved verbatim). Row orderings are top-level `private val` constants so future button-rendering work (e.g. #35 color-blind labels) edits one declaration instead of three blocks.

The grid is intentionally state-free: it accepts `litButton`, `allButtonsLit`, `pressedButtons`, and `onButtonInteraction`. The screen continues to own the `displayLit` derivation (Audio-Only mode swallow) and the `localPressedButtons` map — the grid is unaware of game state, which is the right seam. As a bonus, `LocalConfiguration` is no longer read on the screen, so Configuration changes (font scale, locale, density) outside the grid no longer re-evaluate the screen composable.

SimonGameScreen.kt: 1316 → 1134 lines (-182). 64/64 unit tests green, lint clean, `./gradlew assembleDebug` succeeds. No behavioural change — layouts, paddings, lit/pressed semantics, and `onTouchStateChanged` contracts are byte-identical to the previous inline blocks. No emulator running this session, so live verification was skipped; the change is a pure structural refactor with no logic edits.

Remaining open audit items: #32 (onboarding), #34 (reduce-motion — previously cut as low-value), #35 (color-blind mode — now easier with #17 done). Open features unchanged: F8 achievements, F11 GPGS leaderboard (post-launch v1.1), F13 per-button color picker, F17 IAP (decision pending).

Next batch recommendation: **#35 color-blind mode** is the natural pickup — the freshly-extracted `SimonButtonGrid` is exactly where pattern/icon overlays would land, and the row-order constants already group "which button sits where" in one place. Needs a brief design call (numeric labels vs distinct shapes vs both) before code.

## Session 2026-05-16 — Wave 3

Single mechanical refactor (#35 deferred — needs UX decision per session brief). Picked the next clear extraction target: **#65 `HeadlineOverlay` extract**. The four center-anchored text overlays above the game disc (HIGH SCORE / BEST TIME, GAME OVER / VICTORY, SPEED UP, YOUR TURN) inlined the same `AnimatedVisibility { Box(fillMax + Center) { Text(...) } }` wrapper four times with only the parameters varying (text/color/style/initialScale/scaleOut). Consolidated into a single `ui/components/HeadlineOverlay.kt` composable parameterised by `visible`, `text`, `color`, `style`, `alignment`, `initialScale`, and `useScaleOut`. The three `OverlayBias*` constants moved alongside the composable so the screen and the overlay share one source of truth (was: `private val` in `SimonGameScreen.kt`; now: top-level in `HeadlineOverlay.kt`). The four call sites went from ~25 lines each to ~9 lines each.

Variance preserved verbatim:
- HIGH SCORE / BEST TIME: scale 0.7 in+out, `headlineLarge`, color flips green/yellow on `blitzWon`.
- GAME OVER / VICTORY: scale 0.85 in+out, `headlineLarge`, same color flip.
- SPEED UP: scale 0.7 in+out, `headlineMedium`, fixed orange.
- YOUR TURN: scale 0.9 in, **plain `fadeOut()` (no scaleOut)**, `headlineSmall`, fixed white, uses `OverlayBias4ButtonTurn` instead of `OverlayBias4Button` for the 4-button case. The `useScaleOut = false` flag captures that asymmetry without losing the byte-equivalence promise.

`SimonGameScreen.kt`: 1134 → 1088 lines (-46). New file: 85 lines. Net +39 lines on the project but four duplicated wrappers became four declarative calls, future overlay tweaks (animation curve, alignment math, shadow depth) edit one place. `BiasAlignment` + `scaleOut` imports removed from the screen — no longer needed there.

64/64 unit tests green, lint clean, `./gradlew assembleDebug` succeeds. Pure structural refactor: no UiState changes, no animation parameter changes, no behavioural surface to verify live.

Remaining open audit items: #32 (onboarding — needs UX), #34 (reduce-motion — previously cut), #35 (color-blind mode — needs UX). Open features unchanged: F8 achievements, F11 GPGS leaderboard, F13 per-button color picker, F17 IAP, Light theme variant of F12.

Next batch recommendation: **#35 color-blind mode** is still the natural pickup once the UX decision (numeric labels vs distinct shapes vs both) lands — `SimonButtonGrid` already extracts the row-order constants, and now `HeadlineOverlay` shows the pattern for parameterised UI helpers. Alternatively **F8 local achievements / badges** is the next clear mechanical feature: DataStore key-set + a stats-screen row, no UX decision needed.

## Session 2026-05-16 — Wave 4

Four mechanical extractions continuing the Wave 2/Wave 3 thread (#17 `SimonButtonGrid`, #65 `HeadlineOverlay`). `SimonGameScreen.kt` 1088 → 940 lines (-148), below the 1000-line threshold for the first time since Speed Blitz HUD and the inactivity ring landed. New files all live in `ui/components/`:

1. **`PausedOverlay`** — 47-line inline F14 overlay. Dim backdrop + play icon + Paused headline + tap-to-resume. Self-contained semantics (`contentDescription` on the backdrop for TalkBack). Lets `SimonGameScreen` drop both `androidx.compose.ui.semantics.*` imports — every other `contentDescription` in the screen is the positional Icon parameter, not the modifier.
2. **`ReplayShareButtons`** — 55-line GameOver-only `AnimatedVisibility` row. The `ScoreShareHelper.shareScore(...)` call stays in the screen as an `onShare` lambda so the component doesn't need `LocalContext.current`, `uiState.level`, `uiState.currentHighScore`, or `uiState.memoryLightsPlusEnabled`. Lets the screen drop `BorderStroke`, `OutlinedButton`, `ButtonDefaults`, `Icons.Default.Refresh`, `Icons.Default.Share`, plus the `AnimatedVisibility`/`fadeIn`/`fadeOut`/`scaleIn` set — none of which the screen uses directly anymore.
3. **`TimeoutCountdownRing`** — 37-line drain-ring around the center disc during `PlayerRepeating`. Takes `timeoutMs` + `resetTick`. Re-keying on `timeoutMs` (not just `resetTick`) lets a mid-turn slider in Settings -> Gameplay re-launch at the new duration without waiting for the old animation to finish. Lets the screen drop `Animatable`, `LinearEasing`, `Size`, `lerp`, and the `drawscope.Stroke` imports — the center-disc Canvas inside the screen uses fully-qualified `androidx.compose.ui.graphics.drawscope.Stroke` inline.
4. **`PlayerTurnHalo`** — 30-line constant-size pulsing halo during `PlayerRepeating`. Zero parameters (size 130dp, radius 65dp, 0.06->0.18 alpha sweep, 1.00->1.05 scale sweep, 1100ms FastOutSlowIn — all constants). Caller-side `gameState` gate stays in the screen.

All four are byte-equivalent to the prior inline blocks: same sizes, same animation curves, same colors, same z-order. No behaviour change.

64/64 unit tests green, `./gradlew assembleDebug` succeeds. Lint not re-run this wave.

Remaining seams in `SimonGameScreen.kt` couple to multiple state fields and can't be ripped out as cheaply:
- The center-disc Canvas (4-button + 6-button arc drawing, ~100 lines) splits along the existing `memoryLightsPlusEnabled` branch and is a candidate **if** we extend to color-blind mode (which would add per-button labels there).
- The level-display Column (~120 lines: level number + Speed Blitz time + tap-to-start + sound-load error + sequence-progress) reads ~9 fields off `uiState`. Worth extracting only if its surface is about to grow.
- `BlitzHud` is already a private composable inside the screen file — moving it to `ui/components/` is a one-line lift but adds no clarity.

Next batch recommendation: **F8 local achievements / badges** is the highest-leverage no-UX-decision pickup now — the cheap extraction seams are exhausted, and F8 is a clean DataStore + stats-screen row that doesn't require any design call. **#35 color-blind mode** remains the natural pickup once the UX decision lands (numeric labels vs distinct shapes vs both).

## Session 2026-05-17 — Wave 5

**F8 local achievements landed**, completing the highest-leverage no-UX-decision pickup from the Wave 4 recommendation list. Two-commit delivery — the prior wave (`4ec8c80`) added the `Achievement` enum, `evaluate(...)` companion, DataStore key-set, repo-level `setUnlockedAchievements(...)` + read-back, 11 new strings, and `DataStoreSettingsRepositoryTest.setUnlockedAchievements persists set and reads back`. This wave (`0a59583`) wired those into the live game flow:

- `SimonGameUiState.unlockedAchievements: Set<String>` hydrated from the settings flow alongside the other persisted fields.
- New `evaluateAndPersistAchievements` private helper in `SimonGameViewModel` — calls `Achievement.evaluate(...)`, unions with the current set, early-returns when nothing's new, persists via `settingsRepository.setUnlockedAchievements(merged)`. Safe to call on every game-over; zero writes once every badge is earned.
- Two call sites: `handleBlitzWin` passes `BLITZ_TARGET_LEVEL` + `blitzElapsedMs` so `BlitzChampion` and the sub-30s `Speedrun30s` can fire; `updateGameOverState` passes `currentLevel`, `gamesPlayed + 1`, and `dailyChallengeEnabled`. Streak-based unlocks (`Streak3` / `Streak7`) fire on the following game-over because the StatisticsRepository owns the previous-level cursor we can't project from the VM — acceptable tradeoff vs. duplicating the streak math.
- New `AchievementsSection` composable in `StatisticsScreen.kt`: star-prefixed heading with "N / 8 unlocked" counter, empty-state line when N=0, one Card per Achievement (enum declaration order). Locked rows render with a Lock icon + grey tint; unlocked rows get a yellow `CheckCircle`. Each Card carries a TalkBack-friendly `contentDescription` announcing lock state + description.
- 4 new `SimonGameViewModelTest` cases — FirstWin unlocks at level-1 game-over and persists; daily-mode completion unlocks FirstWin + DailyDone together; `unlockedAchievements` hydrate from `AppSettings` on init; re-unlocking an already-stored achievement is a no-op write (`verify(exactly = 0)` against the repo setter).

80/80 unit tests green (was 64 — +13 from F8 step 1 in `4ec8c80`, +4 from this wave, +remaining wave-4 deltas). `./gradlew assembleDebug` succeeds. Lint not re-run this wave.

Triage note: tree at session start showed 5 files dirty matching prior in-flight F8 stats-screen work — inspected via `git diff`, all five pieces were already coherent (Achievement enum reachable, strings present, repo setter live, GameConstants.BLITZ_TARGET_LEVEL exists at line 42, `currentEpochDay` injection seam on VM at line 84). Only fix needed was a trailing newline on `StatisticsScreen.kt` (diff showed `\ No newline at end of file`). No code rewrites — the work was finish-and-commit, not finish-and-implement.

Remaining open audit items: #32 (onboarding — needs UX), #34 (reduce-motion — previously cut), #35 (color-blind mode — needs UX). Open features: F11 GPGS leaderboard (post-launch v1.1), F13 per-button color picker, F17 IAP (decision pending), Light theme variant of F12.

Next batch recommendation: **#35 color-blind mode** is now the highest-priority mechanical-once-decided feature, but needs the UX call first (numeric labels vs distinct shapes vs both). Alternatively the remaining `SimonGameScreen.kt` extractions (CenterDiscCanvas, LevelDisplayColumn) are S-effort but coupling-heavy — diminishing return at 940 lines unless the surface is about to grow (which #35 would do, since color-blind labels would land in `SimonButtonGrid` and the disc Canvas).

---

## Session 2026-05-17 — Wave 7

**F8 polish landed** — the two follow-ups deliberately scoped out of the Wave 5 initial cut (unlock toast + settings entrypoint card). Single-commit delivery (`d8b1ae0`):

- **Channel-backed unlock event**: New `Channel<List<Achievement>>(capacity = BUFFERED)` + `unlockEvents: Flow<List<Achievement>>` on `SimonGameViewModel`. Lives outside `SimonGameUiState` because a unlock is a transient event — re-collecting state on rotation must not re-fire a stale toast. `evaluateAndPersistAchievements` computes the newly-unlocked delta in enum-declaration order and `trySend`s through the channel after persisting; the existing "nothing new" early-return short-circuits before the send so re-evaluation of an already-unlocked badge never toasts. Channel closes in `onCleared` so in-flight collectors complete cleanly.
- **Snackbar in `SimonGameScreen`**: New `unlockEvents: Flow<List<Achievement>>` parameter (defaulted to `emptyFlow()` for previews / non-VM callers). The screen hosts its own `SnackbarHostState`, wires a `SnackbarHost` into the Scaffold, and `LaunchedEffect(unlockEvents)` collects + shows a Short snackbar per emission. Single-badge form uses the badge title; multi-badge form (rare — only on first deep-progress game-over) collapses to "N achievements unlocked" rather than queuing N snackbars. Dismisses any in-flight snackbar before showing the next so back-to-back unlocks don't pile up.
- **Settings entrypoint card**: New "Achievements" `NavCard` between the Theme card and the Statistics row on `SettingsScreen`, summary "X / 8 unlocked. Tap to view all." Uses `Icons.Default.Star` + amber tint (`0xFFFFC107`) matching the Statistics-screen achievement section. Fans through the existing `onStatisticsClicked` so the full list lives on exactly one route — this is a shortcut, not a duplicated surface. `SettingsScreen` now takes `unlockedAchievements: Set<String> = emptySet()`.
- **Wiring**: `MemoryLightsGame` passes `viewModel.unlockEvents` to `SimonGameScreen` and `uiState.unlockedAchievements` to `SettingsScreen`. The `composable<SettingsRoute>` block already had access to `uiState`, so this is one-line each side.
- **4 new strings**: `achievement_unlocked_toast` (single, `%1$s` = title), `achievement_unlocked_toast_multi` (`%1$d` = count), `settings_achievements_title`, `settings_achievements_summary` (`%1$d / %2$d` = unlocked / total).
- **2 new tests**: `unlockEvents emits newly unlocked achievements in enum order` (collect via `backgroundScope.launch(testDispatcher)` so cancellation is automatic — avoids the deprecated-`launch` lint that fires on bare `kotlinx.coroutines.launch` inside `runTest`); `unlockEvents does not emit when already-unlocked badge re-evaluates`. Existing 80 cases unchanged.

82/82 unit tests green. `./gradlew assembleDebug testDebugUnitTest` succeeds. Lint not re-run this wave.

#35 color-blind mode skipped per the session brief — still needs a UX decision (numeric labels vs distinct shapes vs both). Wave 7 cap was 1 plan chunk OR ≤5 small fixes; F8 polish was one logical chunk shipped in one commit.

Next-session pickup options (in priority order): (1) #35 color-blind mode once the UX decision is made; (2) remaining coupling-heavy `SimonGameScreen.kt` extractions — `CenterDiscCanvas` (~100 lines, splits along the `if (memoryLightsPlusEnabled)` branch — natural pairing with #35 since labels would land in the Canvas) or `LevelDisplayColumn` (~120 lines); (3) #32 progressive onboarding (also UX-blocked).

---

## Session 2026-05-19 — Wave 8

**`CenterDiscCanvas` extracted from `SimonGameScreen`** — the natural pairing-with-#35 mechanical step flagged in the Wave 4 / Wave 7 followup lists. New `ui/components/CenterDiscCanvas.kt` (141 lines) owns both arc-drawing paths; `SimonGameScreen` drops the inline Canvas plus three Compose imports (`foundation.Canvas`, `ui.geometry.Offset`, `ui.graphics.StrokeCap`) and the now-unused `availableButtons` `derivedStateOf` cache (the screen never read it outside the Canvas — its single use was the `sweepAngle = 360f / availableButtons.size` divisor, replaced by a literal `arcCount = if (memoryLightsPlusEnabled) 6 else 4` since `SimonButton.getAvailableButtons(true).size == 6` and `getAvailableButtons(false).size == 4` by enum construction).

Behaviour-equivalence check: the 6-button branch's `sweepAngle = 60f` and per-arc `startAngle = index * 60f` are byte-identical to the prior inline math (`360f / 6 = 60f`). The 4-button branch hardcoded `90f` sweeps with explicit `startAngle`s (GREEN 180, RED 270, BLUE 0, YELLOW 90) — preserved verbatim. `Stroke` is now imported directly (vs. the prior fully-qualified `androidx.compose.ui.graphics.drawscope.Stroke` inline) since the screen's other inline Canvas (the timeout ring) already moved out in Wave 4.

`SimonGameScreen.kt` 975 → 867 lines (-108; the Wave-4 doc-quoted 940 was stale — Wave-5 + Wave-7 F8 wiring grew the screen back to 975). The remaining ~870 lines cluster around the level-display Column (~120 lines, 9 `uiState` fields) and the GameOver/Settings overlay branches — both coupling-heavy enough that further extraction is diminishing returns at this size without a feature surface (#35 labels, #32 onboarding) to grow into.

82/82 unit tests green (no change in count — the extracted Canvas has no logic, only draws). `./gradlew assembleDebug testDebugUnitTest` succeeds. Lint not re-run this wave.

Triage note: tree at session start had two pieces of in-flight Wave 8 work from a prior agent that bailed on credits — modified `SimonGameScreen.kt` (the inline Canvas removed + `CenterDiscCanvas` call added + `availableButtons` derivedState removed) and new untracked `ui/components/CenterDiscCanvas.kt`. Inspected via `git diff` + read of the new file. The extraction was clean and byte-equivalent; the only verification work this wave was the build/test run + this log entry + the commit. No code rewrites.

Next-session pickup options (in priority order): (1) #35 color-blind mode once the UX decision is made — `CenterDiscCanvas` is now the landing zone for per-arc labels alongside `SimonButtonGrid` for per-button labels; (2) `LevelDisplayColumn` extract from `SimonGameScreen` (~120 lines, 9 `uiState` fields — coupling-heavy, only worth doing if its surface is about to grow); (3) #32 progressive onboarding (UX-blocked); (4) Phase 4 Play Store prep (keystore + Play Console submission — user-action only).

---

## Session 2026-07-10 — Wave 9

**`CenterDiscContent` extracted from `SimonGameScreen`** — the coupling-heavy level-display block flagged as the last remaining mechanical seam after Wave 8's `CenterDiscCanvas`. Wave 8 primary target was already committed; this wave picked the next clean extract.

New `ui/components/CenterDiscContent.kt` (260 lines) owns:
- Sound-pack chip (hidden on GameOver)
- Loading / sound-error / WaitingToStart / GameOver score summary / in-progress animated level + N/M sequence progress
- High-score footer (hidden on GameOver)
- Shared `formatBlitzTime` (was private in the screen; BlitzHud still imports it for the live timer)

API is explicit parameters (11 fields) rather than the whole `SimonGameUiState` — matches Wave 4 component style. Sequence progress uses `sequenceSize` / `playerSequenceSize` ints so the component does not depend on list identity. Branching, strings, sizes, colors, and the `AnimatedContent` vertical slide are byte-equivalent to the prior inline block.

`SimonGameScreen.kt` 867 → 687 lines (-180). Imports dropped: `AnimatedContent`, `slideInVertically` / `slideOutVertically` / `togetherWith`, `Icons.Warning`, `unit.sp` (no remaining callers on the screen). `BlitzHud` stays private in the screen (already isolated; lifting it alone adds little).

#35 color-blind mode still skipped (needs UX decision). Cheap mechanical seams in the game screen are now exhausted; remaining private HUD is `BlitzHud` only.

---

## Session 2026-07-10 — Wave 10

Wave 9 CenterDiscContent was already on `main` at session start. #35 / #32 still UX-blocked. Picked the next high-value mechanical chunk: **missing unit coverage for shipped game-logic paths**.

- **`SimonGameViewModelTest` +5** (21 → 26): reverse-mode wrong first press + reverse-order advance to L3; `pauseGame`/`resumeGame` gates; `replayLastSequence` preserves level/sequence after GameOver; Speed Blitz loss does not touch classic high-score but still records stats.
- Shared **`tap()` helper** (press+release) so multi-step input clears `activePresses` — without release, later presses are treated as multi-touch and game logic is ignored.
- **`FormatBlitzTimeTest` +3**: zero/negative clamp, tenths floor, minute rollover for the public `formatBlitzTime` shared by CenterDiscContent + BlitzHud.

82 → 90 unit tests green. `./gradlew assembleDebug testDebugUnitTest` succeeds. No production code changes.

---

## Session 2026-07-10 — Wave 10 continued (#35 Color-blind mode)

**Product decision (locked):** geometric shape/pattern overlays per color — not numeric labels. Distinct silhouettes survive common color-vision deficiencies; numbers would compete with level/sequence UI and read poorly on small panels. Optional small glyphs also drawn on center-disc arcs (cheap once draw helper exists). Default **OFF** so the classic look is unchanged.

Implementation:
- **`ColorBlindShape` enum** + `forButton(SimonButton)` pure mapping (GREEN=circle, RED=triangle, YELLOW=square, BLUE=cross, PURPLE=diamond, ORANGE=stripes).
- **`drawColorBlindShape` / `arcMidpoint`** in `ui/components/ColorBlindShapeDraw.kt` — white fill + dark outline for contrast on lit/dim panels.
- **`SimonPanel`**: optional `colorBlindShape`; centered glyph ~22% of shorter edge.
- **`SimonButtonGrid`**: `colorBlindModeEnabled` threaded to every row; maps button → shape at the panel call site.
- **`CenterDiscCanvas`**: small glyphs at each arc midpoint when enabled (4- and 6-button).
- **Prefs**: `color_blind_mode_enabled` DataStore key; `AppSettings` / `UiState` / `VM.setColorBlindModeEnabled` (no mid-run reset — pure presentation).
- **Settings UI**: toggle on `GameplayScreen` (accessibility/pacing sub-screen) with snackbar feedback.

Tests: +3 shape mapping, +1 DataStore persist, +1 VM setter (no run reset). 90 → 95 unit tests green.

---

## Session 2026-07-10 — Wave 11 (BlitzHud extract)

#32 progressive onboarding skipped (UX-blocked per session brief). Optional mechanical leftover from Wave 9:

- **`BlitzHud` lifted** from private composable in `SimonGameScreen` to public `ui/components/BlitzHud.kt`. Live 10 Hz timer, progress text, best-time chip, pause freeze unchanged. Shares `formatBlitzTime` with `CenterDiscContent`.
- `SimonGameScreen.kt` 691 → 625. No remaining private game-area HUD composables on the screen.

95/95 unit tests green (unchanged). `./gradlew assembleDebug testDebugUnitTest` succeeds.

Cheap game-screen extraction seams are exhausted. Remaining open: #32 (UX), Phase 4 keystore/Console (user), F13 / Light F12 / F11 / F17 (product).

---

## Session 2026-09-12 - Wave 12 (ViewModel lifecycle bug fixes)

All roadmap items were UX/user/product-gated, so this wave audited `SimonGameViewModel` by reading and fixed three real bugs, test-first (7 new `SimonGameViewModelTest` cases, 95 -> 102 green):

- `6553732` **fix(vm): lock input after a miss or timeout until the next turn.** `gameState` stays `PlayerRepeating` through the error-tone delay and the game-over flash, so a stray press could complete the sequence after a miss (level advanced, then GameOver at the wrong level) or run a second `handleGameOver` (statistics recorded twice; practice mode replayed and beeped twice). New private `isInputLocked` flag gates `onButtonClick`.
- `c0f7be8` **fix(vm): freeze the game while navigation is on Settings.** New private `isInSettings` flag. `onResume` on the Settings screen no longer re-arms the inactivity timer or replays the sequence (which ended runs behind the settings UI). Rules-change setters (`setGameMode`, `setMemoryLightsPlusEnabled`, `setDailyChallengeEnabled`) now share `restartRunAfterModeChange()`, which defers the new run to `onReturnToGame` while on Settings. `onReturnToGame` is a no-op until the startup animation has run; it used to call `startNewGame()` on cold start before DataStore answered, racing init.
- `2e962fe` **fix(di): release SimonSoundManager with Koin, not the ViewModel.** `onCleared()` released the process-lifetime Koin singleton, so Back-to-exit then relaunch in a warm process had no audio. Release now lives in the module's `onClose`, matching the repository scopes.

New P2 rows #66-#69 record issues found but not fixed (untracked delayed game-over jobs, timeout double tone, practice mode vs timeout, `MainActivity` debug resource logging + small dead code).

Build note: Gradle 8.14.2 fails under the default Android Studio JBR 25 with the bare message `25.0.2`; use `JAVA_HOME` = JDK 17 or 21 from `~/.gradle/jdks`. Lint re-run this wave: 0 errors, 42 warnings (mostly GradleDependency / UseKtx / UnusedResources).

---

## Session 2026-11-25 - Wave 13 (toolchain + dependency upgrade)

Everything moved to the latest stable releases in three commits, each verified with `./gradlew build` (lint + R8 release) and 102/102 unit tests:

- `fb1f66c` **Gradle 8.14.2 -> 9.9.2, AGP 8.10.1 -> 9.5.3, Kotlin 2.1.10 -> 2.4.21.** AGP 9 compiles Kotlin itself, so the `org.jetbrains.kotlin.android` plugin is removed and the `kotlinOptions` block deleted (jvmTarget now follows `compileOptions.targetCompatibility` = 11). No opt-out flags in `gradle.properties`. The default Android Studio JBR 25 now works, so the JDK 17 `JAVA_HOME` workaround from Wave 12 is obsolete.
- `1baef6f` **AndroidX + Compose BOM 2026.11.01, compileSdk 37.1.** Libraries require compileSdk 37 (navigation, ripple) and 37.1 (compose foundation 1.13), set through the AGP 9 `compileSdk { version = release(37) { minorApiLevel = 1 } }` DSL. targetSdk stays 35 (see #71). Material3 dropped its transitive `material-icons-core` dependency, so it is now declared explicitly (BOM-managed 1.7.8). The new Compose lint error `LocalContextGetResourceValueCall` (26 hits) is fixed by reading strings from `LocalResources.current`.
- `ae16686` **Koin 4.2.2, kotlinx-serialization 1.13.0, coroutines-test 1.11.0, MockK 1.14.12.** No source changes needed.

Device check on Pixel_10 (API 37; Pixel_8_API_34 no longer exists): debug and signed release both launch, play the opening sequence with sound, reach game over with the achievement snackbar, navigate Settings / Statistics / Game Modes (type-safe `@Serializable` routes under R8), persist stats through DataStore, and show the changed toasts. No fatal exceptions.

Lint after the wave: 0 errors, 29 warnings (UseKtx 11, UnusedResources 8, ModifierParameter 4, PluralsCandidate 3, EmptySuperCall 1, ObsoleteSdkInt 1, OldTargetApi 1). No dependency-version warnings left. New rows #70 (`announceForAccessibility` deprecated at API 37) and #71 (targetSdk 36 for Play) record follow-ups.

## Session 2026-11-25 - Wave 14 (targetSdk 36, closes #71)

One-line change: `targetSdk = 35` -> `36` in `app/build.gradle.kts`. No code fixes were needed.

Which Android 16 target-gated changes apply (from `kb://android/about/versions/16/behavior-changes-16`):

- **Predictive back on by default: applies.** The manifest has no `enableOnBackInvokedCallback`, and nothing overrides `onBackPressed` / `KEYCODE_BACK`. The earlier brief said six screens use Compose `BackHandler`; none do. The screens only take `onBackPressed` lambdas for their top-bar arrows, and system Back is handled entirely by NavHost (navigation-compose 2.10.2) through the `OnBackPressedDispatcher`.
- **Edge-to-edge opt-out removed: no-op.** `enableEdgeToEdge()` was already called and there is no opt-out attribute or versioned theme. `statusBarColor` / `navigationBarColor` in `themes.xml` have been ignored since API 35 (left in place).
- **Orientation/resizability ignored on sw600dp+: no-op.** No `screenOrientation`, `resizeableActivity`, or aspect-ratio attributes are declared; `configChanges` is not a restriction.
- **Not applicable:** `elegantTextHeight` (Compose only), `scheduleAtFixedRate` (coroutines), health permissions, Bluetooth, MediaStore, Safer Intents and local-network protection (both opt-in; no INTERNET permission). Nothing touches SoundPool, audio focus, vibration, or DataStore.

Verification on Pixel_10 (API 37, gesture navigation), each check run against a targetSdk 35 baseline first:

- `./gradlew build` passes (lint + R8 release); 102/102 unit tests; lint 0 errors / 29 warnings, same ids as Wave 13. `OldTargetApi` still fires because lint now treats 37 as latest.
- Back gestures driven with `input motionevent` (a cancelled edge swipe and a committed one, since `keyevent 4` skips the gesture progress that changes at 36). Settings -> cancelled swipe stays on Settings and logs `onReturnToGame` 0 times; committed swipe returns to Game and logs it exactly once. Same counts at 35 and 36. Game Modes, Gameplay, Sound & Haptics, Achievements and Statistics each survive a cancelled swipe and pop exactly one level on a committed one. A cancelled swipe on the Game route keeps the app resumed.
- Mid-gesture screenshots (swipe held at x=600): at 36 the Settings screen shrinks into a card with the Game route rendered behind it (NavHost predictive animation); at 35 Settings stays full-screen with only the system back arrow. This is the visible change, and it means the Game route composes during an uncommitted swipe (see #70 note).
- Back on the Game route finishes the activity (`onDestroy`) at both 35 and 36, even with a MAIN/LAUNCHER intent, matching the MainActivity comment. Relaunch recreates it in the same process (the Wave 12 SoundPool path) and sound plays. Cold start after force-stop plays sound too.
- Played a round: sequence, player press, vibration, level 2, timeout game over. Rotation keeps the run (`configChanges`). Top bars clear the status bar on Game, Settings and all five sub-screens, and the "Sound pack: ..." snackbar sits above the gesture bar.
- Large screen simulated with `wm density 240` (sw720dp): the app fills the display with no letterboxing.
- Signed release APK (apksigner, build-tools 37.0.0): launches, navigates Settings and sub-screens with cancelled and committed swipes, plays; no fatal exceptions or R8/Koin errors.

Follow-ups recorded: #72 (center-disc text overlap in landscape and at sw720dp, pre-existing) and a note on #70 (a11y announcements can fire while a back swipe previews the Game route; untested with TalkBack).

## Session 2026-11-25 - Wave 15 (game-over race fixes, closes #66, #67, #73)

- `f51afca` **#66:** a loss runs as two delayed steps (300 ms error-tone wait, 2 s flash wait) that were untracked; the timeout's step was a sibling coroutine `cancelTimeoutTimer` could not reach. New tests showed opening Settings or backgrounding inside the ~2.3 s window recorded **two** game overs: the stale `PlayerRepeating` snapshot was restored with input locked, then the timer fired again. Now tracked through `schedulePendingGameOver`; Settings and `onPause` finish the loss immediately (no tone/flash, lights cleared) before capturing state, mode switches and `onCleared` cancel it, and `pauseGame` is ignored while it is pending.
- **#67:** one error tone per timeout instead of two.
- Six new `SimonGameViewModelTest` cases, all red before the fix; 108/108 green after. `./gradlew build` passes; lint unchanged (0 errors / 29 warnings).
- Pixel_10: miss, open Settings within 0.6 s, back -> Game Over screen, nothing lit, one error tone, no later timeout. A turn timeout logs one error tone.
- `c71e301` **#73** (found while fixing #66): the practice-mode replay had the same untracked 300 ms delay. It now uses the same tracked step (renamed "pending turn end"); leaving the Game screen parks the turn in `ShowingSequence` so it replays on return. `MemoryLightsGame` reads `gameStateAtSettingsEntry` after `showSettings()`, so the mode-switch confirmation no longer appears right after a loss. 3 more tests (111/111). Pixel_10: practice miss + Settings ~120 ms later stays quiet for 12 s and replays on return; after a classic loss, switching to Speed Blitz asks no confirmation.

## Session 2026-11-26 - Wave 16 (new app icon)

- Replaced the placeholder Simon wheel with a generated "glowing button loop" icon (four glossy buttons, sequence arrows, center sparkle). Source art is `playstore/icon_master.png` (1254 px, made in an image model).
- Adaptive icon: raster `mipmap-*/ic_launcher_bg.webp` + `ic_launcher_fg.webp` (108 dp canvas, artwork scaled to 84 dp so it sits inside the 66 dp safe zone, edges feathered into a rebuilt navy background). Monochrome layer is a hand-drawn vector, `drawable/ic_launcher_monochrome.xml`. `ic_simon_launcher.xml` removed.
- `playstore/icon_512.png` is the Play Store icon (512 px, opaque, no pre-rounded corners).
- Verified on Pixel_10: icon renders correctly in the app drawer. The themed-icon (monochrome) layer was only previewed as a render; the emulator did not apply themed icons to any app, so it is unverified on a device.
- Not touched: `ic_launcher_background.xml`, `ic_launcher_foreground.xml` and the robot `mipmap-*/ic_launcher*.webp` files are pre-existing dead resources (lint UnusedResources).

## Session 2026-11-26 - Wave 17 (GitHub cleanup, signing, first Play submission)

- **Repo (public, `guyguy2/MemoryLights`):** README rewritten to match the shipped app (the old one was template leftovers: "Simon Says", `MyApplication2`, wrong package, targetSdk 35). MIT `LICENSE` added and detected by GitHub. About section, website (Pages) and 8 topics set. `.gitignore` now covers `keystore.properties` and `.kotlin/`. Full-history scan found no secrets.
- **Signing:** `app/build.gradle.kts` signs release from `ANDROID_KEYSTORE_*` env or a git-ignored `keystore.properties`, and takes `-PversionCode` / `-PversionName` (same pattern as SimpleDayCounter and SpeechTimer_Jetpack). One upload key per app, per `../cicd/02-signing-and-secrets.md`: `~/.config/android-cicd/keystores/key_memoryLights.p12`, alias `upload`, RSA 4096, backed up to Dropbox.
- **Release checks:** signed AAB 1.0.0 (code 1), target SDK 36, fingerprint matches the key, all 8 native libs 16 KB aligned (zip + ELF), signed release APK smoke-tested on Pixel_10 (full game cycle, no crash).
- **Play Console:** draft app created, listing copy refreshed (removed the "Simon" trademark wording, added Speed Blitz, Audio-Only, color-blind mode, themes, achievements; Daily Challenge deliberately omitted because it has no UI), contact email, 13+ audience, IARC rating, data safety, 178 countries, PC opt-out. Production track was open with no closed-test lock, so the user chose to skip internal testing and submit straight to production at 100%.
- **Submitted:** 11 changes sent for review on 2026-11-26; the result is unknown at the time of writing. See the top block of `specs/RESUME.md` for the review checklist and open follow-ups.

## Session 2026-11-27 - Wave 19 (`1.0.3 / versionCode 4`, Option A layout redesign, closes #72)

- **4-Tile Classic Mode (`!memoryLightsPlusEnabled`):**
  - Added `SlimMetadataStrip` (`ui/components/TopHudStrip.kt`) directly below `TopAppBar` aligned to the `widthIn(max = 500.dp)` game column: renders `★ Best N` (when `> 0`) + `♪ <Sound Pack>`, and integrates Speed Blitz (`Lvl X / 20`, live 10 Hz elapsed clock excluding paused time, `★ Best M:SS.s`, `♪ <Sound Pack>`) into one single compact strip with no stacked double-HUD.
  - Decluttered the `104.dp` center disc (`CenterDiscContent.kt`): removed pack/high-score clutter and `"1"` on `WaitingToStart`; shows Play + `"Tap to start"` on `WaitingToStart`, animated Round + `playerSequenceSize / sequenceSize` during active play, final score/Blitz time + `"Best ..."` + Play Again icon on `GameOver`, and bounded font scaling (`<= 1.25f`) for `2.0x` font scale (`#72`).
  - Replaced `CenterDiscCanvas`, `TimeoutCountdownRing`, and `PlayerTurnHalo` with unified `CenterDiscRing.kt`: neutral track with `displayLit?.color` tint/glow (stays neutral in Audio-Only mode) and smooth green (`0xFF4CAF50`) → amber (`0xFFFFB300`) → red (`0xFFE53935`) countdown ring during `PlayerRepeating` (`countdownProgressColor`).
  - Added circular pointer consumption (`consumeCircularTouch`) on the 4-tile center disc during active play so stray taps never hit `SimonPanel` underneath.
- **6-Tile Memory Lights+ Mode (`memoryLightsPlusEnabled`):**
  - Removed the center circle completely so all 6 tiles (`3×2` portrait / `2×3` landscape) stay unobstructed.
  - Added `FullTopHudStrip` (`ui/components/TopHudStrip.kt`) directly below `TopAppBar` with `ROUND` + animated level (or `BLITZ` + `Lvl X / 20`), center status/step pill (`3 / 7`, `Watch`, `Paused`, `Ready`, `Game Over` / `Victory!`, `Loading…`, `Audio error`, paired with live Blitz timer), `★ Best` + `♪ <Sound Pack>`, and a persistent `4.dp` bottom-edge green → amber → red countdown progress bar.
  - Added accessible 6-tile `WaitingToStart` `"Start Game"` CTA, `soundLoadError` status pill, and `SixTileGameOverSummary` integrated via `ReplayShareButtons(topContent = ...)` above `"New Game"` + `Replay` / `Share`.
- **Headline Overlays (`#72`) & Preserved Behaviors:**
  - Replaced fixed bias alignments (`OverlayBias*`) with layout-aware non-overlapping overlay slots in `SimonGameScreen.kt` + pill-backed `HeadlineOverlay.kt` (clearing the 4-tile disc and Top HUD strips across portrait, compact landscape, `sw720dp`, and `2.0x` font scale). Preserved Color-Blind Mode (`#35`), stationary leftmost `Pause`, Speed Blitz pause clock, Difficulty default ON, all 7 sound packs, and all 6 `Wave18UiTest` behaviors.
- **Verification:** `129/129` JVM unit tests (`CountdownProgressColorTest` + existing 123), `13/13` Compose instrumented tests on `Pixel_10` (`Wave18UiTest` + `Wave19UiTest` + `ExampleInstrumentedTest`), `lintDebug` / `lintRelease` `0` errors, live `Pixel_10` visual check across 4/6-tile × portrait/landscape × `1.0x`/`2.0x` + Blitz + Color-Blind Mode (`#35`), R8 release smoke test, `gplay preflight`, and 16 KB alignment check.

## Session 2026-11-27 - Wave 20 (`1.0.4 / versionCode 5`, UX craft, WCAG contrast & automated guards, closes #34 and #70)

- Implemented all 15 items of `specs/ux-polish-plan.md` (Waves A–D): centralized `@Immutable GameAccentColors` (`LocalGameAccentColors`, Total Score `#CE93D8` `>= 6.0:1`), replaced `Color.Gray`/`Color.White` literals with M3 tokens, moved TalkBack achievement/panel strings to `strings.xml`, removed nested clickable traps, enabled `tnum` tabular numerals on live Blitz timers and scores, honored `ANIMATOR_DURATION_SCALE == 0` in `ParticleEffect` and CTA pulse (`#34`), fixed AMOLED dialog `surfaceContainerHigh` separation, persisted reset dialogs with `rememberSaveable`, capped Settings/Statistics/sub-screen width at `840.dp`, replaced deprecated `announceForAccessibility` with `LiveRegionMode.Polite` (`#70`), and added permanent JVM guards (`BrandColorSchemeTest`, `ThemeContrastTest`, `StringLiteralGuardTest`), Compose multipreviews (62 renders), and Compose ATF tests (`ScreenAccessibilityTest`, `AdaptiveScreenAccessibilityTest`).
- **Verification:** `140/140` JVM unit tests, `27/27` instrumented/ATF tests on `Pixel_10`, `lintDebug`/`lintRelease` `0` errors, R8 release smoke test, `gplay preflight`, 16 KB ELF alignment check. Shipped `1.0.4 (5)` to `internal` & `production` (`completed`, 100%).

## Session 2026-11-27 - Wave 21 (Polish, Compliance & Cleanup, closes #68)

- **In-app Privacy Policy link:** Added a `Privacy Policy` `SettingsCard` in `SettingsScreen.kt` (between `Rate this App` and `About`) opening `https://guyguy2.github.io/MemoryLights/privacy.html` with `ActivityNotFoundException` snackbar fallback.
- **Practice Mode timeout replay (`#68`):** Unified `handleWrongButton` and `startTimeoutTimer` failure paths into `handleTurnFailed` (clearing `timeoutJob` before delegating so it never self-cancels). The first inactivity timeout in Practice Mode plays one error tone, clears `playerSequence`, keeps level/stats/high score intact, and replays the sequence; a second consecutive idle timeout with no player input cleanly pauses the game instead of looping audio forever. Updated `toggle_practice_summary` copy.
- **Jetpack `core-splashscreen 1.2.0`:** Added `Theme.MemoryLights.Starting` (`windowSplashScreenBackground = @android:color/black`, 4-button vector `drawable/ic_splash.xml`, `postSplashScreenTheme = Theme.MemoryLights`), wired `installSplashScreen()` in `MainActivity.onCreate` held until `SimonGameUiState.settingsLoaded` (or 2s safety cap).
- **Asset & docs hygiene:** Removed 12 unused legacy launcher files (`drawable/ic_launcher_{background,foreground}.xml` and `mipmap-*/ic_launcher*.webp`), eliminating `[UnusedResources]` lint warnings, and synced `specs/improvements.md`, `specs/RESUME.md`, and `AGENTS.md`.

---

## Critical Files

| Concern | File |
|---|---|
| Build / R8 / Play Store | `app/build.gradle.kts`, `app/proguard-rules.pro`, `settings.gradle.kts` |
| DataStore main-thread fix | `app/src/main/java/.../data/repository/SettingsRepository.kt`, `app/src/main/java/.../data/manager/StatisticsManager.kt` |
| State / nav cleanup | `app/src/main/java/.../domain/model/SimonGameUiState.kt`, `domain/model/SimonGameState.kt`, `ui/MainActivity.kt`, `ui/screens/SimonGameScreen.kt`, `viewmodels/SimonGameViewModel.kt` |
| Sound packs | `app/src/main/java/.../domain/enums/SoundPack.kt`, `app/src/main/res/raw/`, new `tools/sounds/` |
| ViewModel tests (new) | `app/src/test/java/.../ui/viewmodels/SimonGameViewModelTest.kt` |
| Audit source of truth | this file (`specs/improvements.md`) |

---

## Verification

End-to-end smoke test after each phase:
- `./gradlew test` — all unit tests pass (28 → grows).
- `./gradlew lint` — no new warnings.
- `./gradlew assembleRelease` — R8 produces a working APK once item #1 lands.
- Start emulator: `android emulator list` → `android emulator start <name>` → `./gradlew installDebug` → play one round in 4-button + 6-button mode, switch each sound pack, force-stop and relaunch, confirm high score + settings persist.
- After sound packs: `adb logcat | grep com.happypuppy.memorylights` and listen for "Sound not available" warnings — there should be none for the three new packs.

---

## Resume Prompt (paste in next session)

```
Resume the Memory Lights roadmap from specs/improvements.md.

Decisions already locked: canonical doc = specs/improvements.md (root IMPROVEMENTS.md already deleted); sound production = synth-first (Python numpy for Musical, jsfxr for Sci-Fi, Freesound CC0 for Nature); milestone order = sound packs first, then Play Store launch.

Status: Wave 21 closed #68 (Practice Mode timeout replay), added in-app Privacy Policy link, wired Jetpack core-splashscreen 1.2.0 with ic_splash.xml, and removed dead legacy launcher assets. Wave 20 (1.0.4 / 5) closed #34 and #70 with WCAG contrast fixes, tabular numerals, AMOLED dialog separation, tablet 840dp caps, and JVM + Compose ATF guards. Wave 19 (1.0.3 / 4) closed #72 with Option A layout redesign. Open audit/polish: #74 Play Store listing title & post-approval store assets (waiting on Play approval), #32 onboarding, #36 keyboard/controller (deferred). Open v1.1+ features: F11 GPGS, F13 per-button color picker, F17 IAP (decision pending), Light theme variant of F12. Build runs on default Android Studio JBR; prefer local CI/CD (/Users/guy/dev/android/cicd). Read specs/RESUME.md for the latest wave pointer.
```

Tweak the last line if you want to push further per session — e.g., "go through all of Phase 2 and stop", or "run all the way through Phase 2 then start Phase 3 step 1".

---

## Open Questions (resolved 2026-05-03)

1. **Musical pack mapping** — ✅ resolved: pentatonic piano-only (C5 / E5 / G5 / A5 / D5 / F#5 + minor-2nd error cluster). Single-instrument for consistent feel.
2. **Sci-Fi pack** — ✅ resolved: chiptune retro (Python synth, jsfxr-style presets — square/saw with sweeps, klaxon error). License-free vs jsfxr GUI export.
3. **Nature pack** — ✅ resolved: synth approximations rather than Freesound CC0. CC0 sourcing requires account-gated downloads; synth keeps the pipeline reproducible and license-clean. Documented in `tools/sounds/SOUND_LICENSES.md`.
