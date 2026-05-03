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
| Build | **R8 + resource shrinking ON** for release. APK shrunk 35.5 MB → 7.2 MB. Koin + Activity keep rules in `proguard-rules.pro`. `Instantiatable` lint false positive suppressed via `tools:ignore` on MainActivity. `rootProject.name = "My Application"` still pending (item #23). `versionCode 1` `versionName 1.0` still pending. |
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
| 42 | Splash white flash — `themes.xml` inherits `Theme.Material.Light.NoActionBar`. Set `android:windowBackground = #000000` and use Splash Screen API. | ✅ Phase 2 step 4 (windowBackground + statusBar + navigationBar all overridden to `@android:color/black`; Splash Screen API not used yet — can be added later if needed) | S | `app/src/main/res/values/themes.xml` |
| 43 | `saveSettings()` writes all 7 DataStore keys per single setting toggle (~9 callsites + onCleared). Replace whole-state flush with per-setting writes; drop redundant `saveSettings()` in `onCleared`. | ✅ Phase 2 step 4 (`saveSettings()` deleted; each setter / high-score updater calls only its own repository write; `onCleared` no longer flushes) | M | `viewmodels/SimonGameViewModel.kt` |
| 44 | "Memory Lights+" toggle mid-game silently wipes current run. Add confirm dialog if `gameState != WaitingToStart && gameState != GameOver`. | ✅ Phase 2 step 4 ("End current game?" dialog with Cancel/Switch shows when `hasActiveGame` is true; toggle is gated on confirmation) | S | `ui/screens/SettingsScreen.kt`, `ui/screens/SimonGameScreen.kt` |
| 45 | "Reset High Score" card title understates — actually wipes ALL statistics. Rename title to "Reset Score & Statistics" so the destructive scope is visible without opening the dialog. | ✅ Phase 2 step 4 | S | `ui/screens/SettingsScreen.kt` |
| 46 | Game-over flow has no score summary before tap-to-restart — center disc tap immediately restarts, killing reward loop and removing share motivation (depends on F10 if added). | ✅ Phase 2 step 4 (center disc on GameOver now stacks final score + best score + play arrow; tap still restarts) | S | `ui/screens/SimonGameScreen.kt` |

### P2 — Polish, post-launch

| # | Item | Status | Effort | Files |
|---|---|---|---|---|
| 16 | Remove dead code: `onButtonRelease`, `updatePreference`, `getContext()`, `triggerParticleEffects` | ✅ Phase 5 step 3 (`getContext()` and `triggerParticleEffects()` deleted; `onButtonRelease` already removed in Phase 1; `updatePreference` not present) | S | multiple |
| 17 | Extract `SimonButtonGrid` composable to shrink 750-line `SimonGameScreen` | ❌ 🆕 | M | `ui/screens/SimonGameScreen.kt` |
| 18 | Replace `delay(16)` loop in `ParticleEffect` with `withFrameMillis` | ✅ Phase 5 step 3 (frame-clock driven; ticks per actual frame instead of 16-ms timer; cleaned up unused `Log`/`DrawScope`/`dp` imports) | S | `ui/components/ParticleEffect.kt` |
| 19 | Replace `Handler(Looper.getMainLooper()).post` vibration dispatch | ✅ Phase 5 step 5 (`playSound` is only called from `@MainThread` `onButtonClick` and `viewModelScope` launches that default to `Dispatchers.Main.immediate`; `vibrator.vibrate()` is thread-safe — Handler indirection dropped, direct call) | S | `data/manager/SimonSoundManager.kt` |
| 20 | Move `LocalConfiguration` orientation read out of `SimonGameScreen` to avoid full-screen recomposition | ⚠️ Phase 5 step 5 partial (orientation read wrapped in `remember(orientation) { ... }` so the boolean is stable across unrelated Configuration changes; full panel extraction tracked as #17) | S | `ui/screens/SimonGameScreen.kt` |
| 21 | Fix `ERROR_SOUND_VOLUME_BOOST = 1.2f` silently clamped to 1.0 | ✅ Phase 5 step 3 (constant deleted; error sound now uses `MAX_VOLUME` with comment explaining SoundPool's 1.0 ceiling) | S | `domain/GameConstants.kt`, `data/manager/SimonSoundManager.kt` |
| 22 | Tablet/foldable cap: `widthIn(max = 500.dp)` on game panel | ✅ Phase 5 step 3 | S | `ui/screens/SimonGameScreen.kt` |
| 23 | Rename `rootProject.name` → `MemoryLights` | ✅ Phase 5 step 3 | S | `settings.gradle.kts` |
| 24 | Cancel `CoroutineScope` in `StatisticsManager` and `DataStoreSettingsRepository` on app destruction | ❌ 🆕 | M | `data/manager/StatisticsManager.kt:41`, `data/repository/SettingsRepository.kt:74` |
| 25 | Add `displayName` property to `SimonButton`; remove local `capitalize()` extension | ✅ Phase 5 step 3 (`SimonButton.displayName`; `String.capitalize()` deleted; `currentSoundPack.displayName` reused — UI now shows "Sci-Fi" instead of "Sci_fi") | S | `domain/enums/SimonButton.kt`, `ui/screens/SimonGameScreen.kt` |
| 26 | Remove unused `lifecycle-process` dependency | ✅ Phase 5 step 3 | S | `app/build.gradle.kts`, `libs.versions.toml` |
| 27 | `catch (_: Exception)` → `catch (_: ActivityNotFoundException)` for Play Store intent | ✅ Phase 5 step 3 | S | `ui/screens/SettingsScreen.kt` |
| 28 | Disable dynamic color (`dynamicColor = false`) to preserve all-black aesthetic | ✅ Phase 5 step 3 (default flipped + comment explains why; flip on if F12 theme toggle ships) | S | `ui/theme/Theme.kt` |
| 29 | Move `StatisticsManager` to `data/repository/` and rename `StatisticsRepository` | ✅ Phase 5 step 6 (`StatisticsManager` → `data/repository/StatisticsRepository`; class + test class + DI binding + ViewModel field renamed; old `data/manager/` test dir cleaned up; 53 tests still green) | S | rename |
| 30 | 4-button mode padding inconsistency (Green 2.dp vs others 4.dp) | ✅ Phase 5 step 5 (Green now also 4.dp; all four panels share padding) | S | `ui/screens/SimonGameScreen.kt` |
| 31 | Hardcoded `.offset(y = ...)` for text overlays — switch to `Alignment` | ✅ Phase 5 step 5 (3 named `BiasAlignment` constants — `OverlayBias6Button`, `OverlayBias4Button`, `OverlayBias4ButtonTurn` — replace inline `.offset(y = ...)` calls on HIGH SCORE / GAME OVER / SPEED UP / YOUR TURN; bias is screen-proportional so positioning stays consistent across phone sizes) | S | `ui/screens/SimonGameScreen.kt` |
| 32 | Progressive onboarding / first-run tutorial | ❌ | M | new |
| 33 | Snackbar confirmation on settings change | ✅ Phase 5 step 6 (`SnackbarHost` added to Scaffold; sound-pack pick + difficulty toggle + ML+ confirm-dialog accept all fire transient `Short`-duration snackbar; verified live: "Difficulty: On" appears after toggle) | S | `ui/screens/SettingsScreen.kt` |
| 34 | Reduce-motion option / respect `LocalReducedMotion` | ❌ | S | theme + screen |
| 35 | Color-blind mode (icons/patterns on buttons) | ❌ | M | `ui/components/SimonPanel.kt` |
| 36 | Keyboard / external controller support (TV / ChromeOS / gamepad) | ⏭️ Deferred (2026-05-03) — out of scope for v1.0; revisit if TV / ChromeOS becomes target platform | L | new |
| 47 | `activePresses` mutable set lacks `@MainThread` discipline — touch + coroutines both touch it without lock. Annotate `@MainThread` or wrap in mutex. | ✅ Phase 5 step 1 (`onButtonClick` annotated `@MainThread`; comment on `activePresses` documents single-mutator contract) | S | `viewmodels/SimonGameViewModel.kt` |
| 48 | Externalize all UI strings to `strings.xml` — currently only `app_name` is externalized. Blocks i18n + string audit. | ❌ 🆕 (multi-agent) | M | `ui/screens/SimonGameScreen.kt`, `ui/screens/SettingsScreen.kt`, `ui/screens/StatisticsScreen.kt` |
| 49 | Dedupe duplicated dark hex literals (`0xFF1D1D1D`, `0xFF1A1A1A`, `0xFF121212`, `0xFF303030`) across 3 screens — move to `Color.kt`. Prereq for #28 (dynamicColor) and F12 (theme toggle). | ✅ Phase 5 step 1 (5 semantic tokens: `CardBackground`, `DialogBackground`, `SurfaceContainer`, `SurfaceSelected`, `SurfaceContainerFade` in `theme/Color.kt`; all 3 screens import them) | S | `ui/theme/Color.kt`, `ui/screens/*.kt` |
| 50 | `SimonButton.index` duplicates `ordinal` — drop the field, drop the test that asserts on it. | ✅ Phase 5 step 1 (field removed; 2 SimonButton tests asserting on `index` deleted; only `ordinal` remains) | S | `domain/enums/SimonButton.kt`, `domain/enums/SimonButtonTest.kt` |
| 51 | ProGuard `-keep class org.koin.**` is too broad. Koin 4.x with `viewModelOf` doesn't need the wildcard — narrow to `KoinComponent` + reflection helpers actually used. Reclaim release APK size. | ✅ Phase 5 step 2 (wildcard `org.koin.**` keep dropped; only project DI module kept; release APK 7.84 MB → 7.48 MB; signed release verified booting cleanly on emulator) | S | `app/proguard-rules.pro` |
| 52 | `handleButtonInteraction` lambda not `remember`ed — invalidates all 6 `SimonPanel`s every recomposition. Wrap in `remember(localPressedButtons, onButtonClick)`. | ✅ Phase 5 step 1 (wrapped in `remember(onButtonClick)`; closure reads `localPressedButtons` via `MutableState` so stable lambda survives state changes) | S | `ui/screens/SimonGameScreen.kt` |
| 53 | `Brush.linearGradient` allocated per recomposition in `SimonPanel`. Wrap in `remember(buttonColors)`. | ✅ Phase 5 step 1 (`buttonColors` and `buttonBrush` both wrapped; brush only re-allocs when `userPressed`/`isLit`/base colors change) | S | `ui/components/SimonPanel.kt` |
| 54 | `isLit` flash is hard color switch — no smooth brightness ramp. Use `animateColorAsState` so the Simon flash matches the press spring polish. | ✅ Phase 5 step 2 (`litProgress = animateFloatAsState(tween 120ms FastOutSlowInEasing)` + per-component `lerp` between off and lit gradient lists) | M | `ui/components/SimonPanel.kt` |
| 55 | Overlay text (HIGH SCORE / GAME OVER / YOUR TURN) appears with no `AnimatedVisibility` — abrupt pop-in. Wrap each `if` block in fade/slide. | ✅ Phase 5 step 2 (each overlay wrapped in `AnimatedVisibility(fadeIn + scaleIn)`) | S | `ui/screens/SimonGameScreen.kt` |
| 56 | `WaitingToStart` shows static "1" with no tap-to-begin affordance — pulsing animation, play icon, or "Tap to start" label needed (mirrors GameOver's play affordance). | ✅ Phase 5 step 2 (Pattern B: keep level "1", add play arrow + "Tap to start" label; disc now tappable in both `GameOver` and `WaitingToStart`; subtle `rememberInfiniteTransition` scale pulse 1.0→1.06 telegraphs the affordance) | M | `ui/screens/SimonGameScreen.kt` |
| 57 | Statistics screen uses `Icons.DateRange` for 3 unrelated rows ("Games Played", "Average Score", "Total Score"). Pick distinct icons. | ✅ Phase 5 step 5 (Games Played → `PlayArrow`, Average Score → `AutoMirrored.List`, Total Score → `AddCircle`, Best Streak → `CheckCircle` so it stops duplicating High Score's `Star`; verified live on emulator) | S | `ui/screens/StatisticsScreen.kt` |
| 58 | `Typography` scale defined in `Type.kt` but unused — every `Text` sets `fontSize` directly, losing semantic role + a11y type-scale. Migrate to `MaterialTheme.typography.*`. | ❌ 🆕 (multi-agent) | M | `ui/theme/Type.kt`, all 3 screens |
| 59 | `SoundPackOption` double ripple: `Row.clickable` + `RadioButton.onClick` both fire ripples on same tap. Pass `interactionSource = remember { MutableInteractionSource() }` and `indication = null` to one of them. | ✅ Phase 5 step 5 (`RadioButton.onClick = null` — Row click handles selection; ripple only fires once over the full row) | S | `ui/screens/SettingsScreen.kt` |
| 60 | "Your Turn" cue vanishes after round 3, leaving only faint halo as turn indicator. Keep persistent affordance throughout the player's turn (subtle pulsing border, mode-tag, or sticky text). | ✅ Phase 5 step 4 (static halo replaced with `rememberInfiniteTransition` alpha-pulse 0.06→0.18 + scale 1.0→1.05; now visible for entire `PlayerRepeating` phase, not just rounds 0-2) | S | `ui/screens/SimonGameScreen.kt` |
| 61 | Timeout has no visible countdown — game ends "from nowhere" at 10s. Add a thin progress ring around the center disc that drains during the player's turn. | ✅ Phase 5 step 2 (`timeoutResetTick: Int` added to `SimonGameUiState`; ViewModel increments on every `startTimeoutTimer`; UI runs `Animatable(1f)` over `PLAYER_TIMEOUT_MS` keyed on tick, draws thin 124-dp ring around the disc with `lerp(red, green, fraction)` color shift) | M | `domain/model/SimonGameUiState.kt`, `viewmodels/SimonGameViewModel.kt`, `ui/screens/SimonGameScreen.kt` |
| 62 | Difficulty speed-up invisible to player. When difficulty reduces timing, flash a "Speed Up!" text or attach a level-tier subtitle ("Level 5 - Fast"). Pairs with item #38. | ✅ Phase 5 step 4 (`showSpeedUpText` flag in `SimonGameUiState`; ViewModel sets true in `advanceToNextLevel` when difficulty enabled and `(newLevel - 1) % DIFFICULTY_INTERVAL == 0` and `newLevel >= 5`; `speedUpTextJob` auto-clears after `SPEED_UP_TEXT_DISPLAY_MS = 1200ms`; UI overlay `AnimatedVisibility` fade+scale) | S | `domain/model/SimonGameUiState.kt`, `domain/GameConstants.kt`, `viewmodels/SimonGameViewModel.kt`, `ui/screens/SimonGameScreen.kt` |
| 63 | Sound pack preview always plays GREEN regardless of pack tapped. Cycle through all colors, or play a random color, so player auditions full pack character. | ✅ Phase 5 step 5 (`previewToneIndex` cursor in ViewModel cycles through `getAvailableButtons(memoryLightsPlusEnabled)` so the player hears each button's tone in turn) | S | `viewmodels/SimonGameViewModel.kt` |
| 64 | No sequence-progress "3 / 7" indicator during repeat — players lose count at high levels. Add `playerSequence.size / sequence.size` text below the center disc when `gameState == PlayerRepeating`. | ✅ Phase 5 step 4 (small "X / Y" Text rendered under the level number inside the center disc Column when `PlayerRepeating` and sequence non-empty; verified live: layout dump shows "0 / 1") | S | `ui/screens/SimonGameScreen.kt` |

### Done (verified — closing the loop on the original 2025 audit)

✅ Mutable map → immutable map · TAG companion constants · `entries` over `values()` · unused import removed · `dynamicColor && true` fixed · GameConstants object created · content descriptions / state descriptions / role / 48dp touch targets · screen-reader announcements (level, your-turn, game-over) · sound loading state + indicator · `derivedStateOf` for `availableButtons` · lazy sound-pack loading · `ComponentCallbacks2` memory-trim · audio focus + listener · graceful fallback to GREEN tone · master volume control · `SettingsRepository` interface + impl · DataStore migration with auto-migrate-from-SharedPreferences · 28 unit tests for domain/state/constants.

⏭️ Skipped (decision documented in `progress.md`): split `GamePlayState` / `SettingsState` (low recomposition benefit for this app size).

---

## Feature Backlog (multi-agent scan, 2026-05-02)

Product features that are not strictly audit fixes — sequenced separately so engineering polish doesn't block product direction. Pick which to ship in v1.0 vs v1.1+ before Phase 4 (Play Store prep). Numbered F1-F17 to keep them visually distinct from audit items #1-#64.

### Game variants

| # | Feature | Effort | Notes |
|---|---|---|---|
| F1 | **Reverse Mode** — watch then repeat backwards | S | Toggle in Settings; one-line change in `checkSequenceMatch` to compare against reversed sequence. |
| F2 | **Visual-Only (silent) Mode** — skip `soundManager.playSound` during sequence | S | Doubles as accessibility for hearing-impaired. |
| F3 | **Audio-Only Mode** — hide button colors during sequence playback | M | Exposes weak placeholder packs — pair with Phase 3 sound pack work. |
| F4 | **Speed Blitz Mode** — fixed-length sprint (e.g. 20 buttons) with leaderboard time | M | New `GameMode` enum + separate high-score slot + sprint timer UI. |
| F5 | **Smoother difficulty curve** (logarithmic) | S | Overlaps with audit fix #38 (`calculateSequenceTiming`); do them together. |

### Engagement / retention

| # | Feature | Effort | Notes |
|---|---|---|---|
| F6 | **Daily Challenge** — deterministic seed `Random(date.toEpochDay())` | M | Persist "completed today" flag in DataStore. |
| F7 | **Win-streak tracking** | S | Extends existing `bestStreak` slot in `StatisticsManager`; pick definition (beat prior session vs beat personal best). |
| F8 | **Local achievements / badges** — milestone rewards | M | New DataStore key-set; no backend. |
| F9 | **Replay last sequence after game-over** ★ | ✅ Phase 6 step 1 (2026-05-03): `VM.replayLastSequence` keeps `sequence` + `level`, clears player state + overlay flags, drives existing `showSequence` path. New `OutlinedButton("Replay last sequence")` overlay at GameOver bottom-center, hidden via `AnimatedVisibility` everywhere else. Verified on emulator: GameOver → tap → same `Showing sequence item 0: BLUE` → PlayerRepeating with countdown ring. |
| F10 | **Share score card image** — Android share sheet | M | `Compose → Bitmap` (Picture or `View.drawToBitmap`). Pairs with audit fix #46 (game-over score summary). |
| F11 | **Google Play Games Services leaderboard** | L | GPGS SDK + Console project + sign-in flow. **Post-launch v1.1.** |

### Customization / quality-of-life

| # | Feature | Effort | Notes |
|---|---|---|---|
| F12 | **Light / AMOLED theme toggle** | S | Depends on audit fixes #49 (dedupe color hex literals) and #58 (typography scale). |
| F13 | **Per-button color picker** (colorblind a11y) | L | Replaces / complements audit item #35. Requires per-button override map in DataStore. |
| F14 | **Explicit pause button** + new `GameState.Paused` | ✅ Phase 6 step 3 (2026-05-03): `GameState.Paused` added. `pauseGame()` (no-op outside `PlayerRepeating`) cancels the inactivity timer + sets state. `resumeGame()` (no-op outside `Paused`, also gated on `isAppInForeground`) flips back and calls `startTimeoutTimer` — timer restarts at the player's full configured timeout, not the remaining time (simpler model; pause is rare). Top-bar `Pause` IconButton appears only during `PlayerRepeating`. Full-screen 70%-alpha black overlay during `Paused` shows a 96-dp play arrow + `PAUSED` label + "Tap to resume" hint; entire backdrop is one click target so any tap resumes. Settings `hasActiveGame` includes `Paused` so the ML+ confirm-dialog still fires. `exitSettings` restores `Paused` if the player entered Settings while paused. Verified on emulator: PlayerRepeating → tap pause → overlay shown, Pause icon hidden, ring stops drawing → tap overlay → back to PlayerRepeating, ring re-drains. New `res/drawable/pause_24px.xml` + a11y announcement "Game paused." | S | `domain/model/SimonGameState.kt`, `viewmodels/SimonGameViewModel.kt`, `ui/screens/SimonGameScreen.kt`, new `res/drawable/pause_24px.xml`, `app/src/test/java/.../domain/model/GameStateTest.kt` |
| F15 | **Practice mode** — wrong button doesn't end game | ✅ Phase 6 step 4 (2026-05-03): `practiceModeEnabled` DataStore key + `AppSettings`/`UiState` field + setter. Extracted shared `handleWrongButton(reason)` helper from `checkSequenceMatch`'s out-of-bounds and mismatch branches; in practice mode it plays the error tone, brief delay, clears `playerSequence`, and re-runs `showSequence` instead of `handleGameOver`. Settings toggle card with snackbar. Scope intentionally narrow: timeouts still end the run (toggle text says "Wrong buttons replay the sequence" — clear on scope). High scores never advance in practice mode because game-over → `recordGameResult` path is never taken. New `SimonGameViewModelTest`: `practice mode wrong button replays sequence instead of game over` asserts `recordGameResult`/`setHighScore` never fire. Verified on emulator: toggled on, tapped RED when expecting GREEN, logcat shows "Practice mode: Wrong button pressed — replaying sequence" → state stays PlayerRepeating after replay. | M | `data/repository/SettingsRepository.kt`, `domain/model/SimonGameUiState.kt`, `viewmodels/SimonGameViewModel.kt`, `ui/screens/SettingsScreen.kt`, `ui/screens/SimonGameScreen.kt`, tests |
| F16 | **Adjustable timeout slider** (5 / 10 / 15 / 30) | ✅ Phase 6 step 2 (2026-05-03): `playerTimeoutSeconds` DataStore key + `AppSettings` field (default 10) + per-key setter. `SimonGameUiState.playerTimeoutMs` computed from seconds; `startTimeoutTimer` and the countdown ring both read it dynamically. Settings card shows a 4-FilterChip segmented control (5s / 10s / 15s / 30s) with snackbar feedback; mid-turn change resets the in-flight timer via `resetTimeoutTimer` so the ring re-drains immediately. Verified on emulator: select 5s → snackbar fires → kill app → relaunch → 5s still selected. ∞ option dropped from spec — rare use case, would complicate the ring drain animation; can revisit if user requests. | S | `data/repository/SettingsRepository.kt`, `domain/model/SimonGameUiState.kt`, `viewmodels/SimonGameViewModel.kt`, `ui/screens/SettingsScreen.kt`, `ui/screens/SimonGameScreen.kt`, new `res/drawable/schedule_24px.xml` |

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
5. **Phase 4 — Play Store prep (1 day):** item #23 (rootProject.name), signed AAB, store listing, screenshots showing all 7 packs, privacy policy.
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
8. **Phase 6 — Feature backlog (product roadmap, post-launch):** Feature Backlog items F1–F17. Recommended starting set: F9 (replay last sequence) + F14 (pause button) + F16 (adjustable timeout) — all S-effort, high impact. F17 (IAP) requires explicit ads-vs-IAP product decision before any code.

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

Status: Phase 0 + Phase 1 done and pushed to origin/main (HEAD = 8b2bd94). About to start Phase 2 step 1: write SimonGameViewModelTest using mockk + UnconfinedTestDispatcher. ViewModel constructor takes (SimonSoundManager, StatisticsManager, SettingsRepository); tests must stub settingsFlow/statisticsFlow with flowOf(AppSettings(...))/flowOf(GameStatistics(...)). Read the plan file, confirm git state matches, and start Phase 2 step 1. Stop after the test file is added + passing, then ask before continuing.
```

Tweak the last line if you want to push further per session — e.g., "go through all of Phase 2 and stop", or "run all the way through Phase 2 then start Phase 3 step 1".

---

## Open Questions (resolved 2026-05-03)

1. **Musical pack mapping** — ✅ resolved: pentatonic piano-only (C5 / E5 / G5 / A5 / D5 / F#5 + minor-2nd error cluster). Single-instrument for consistent feel.
2. **Sci-Fi pack** — ✅ resolved: chiptune retro (Python synth, jsfxr-style presets — square/saw with sweeps, klaxon error). License-free vs jsfxr GUI export.
3. **Nature pack** — ✅ resolved: synth approximations rather than Freesound CC0. CC0 sourcing requires account-gated downloads; synth keeps the pipeline reproducible and license-clean. Documented in `tools/sounds/SOUND_LICENSES.md`.
