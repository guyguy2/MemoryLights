# AGENTS.md

This file provides guidance to AI coding agents when working with code in this repository.

## Project

Memory Lights is a single-module Android memory game (`:app`, package `com.happypuppy.memorylights`) inspired by classic Simon electronic memory games. Players repeat increasingly complex sequences of lights and sounds across Classic and Speed Blitz modes, 4-button Standard and 6-button Memory Lights+ boards, Reverse/Practice/Audio-Only/Daily Challenge/Color-Blind modes, 7 sound packs, statistics, and achievements. Stack: Kotlin `2.4.21`, Jetpack Compose + Material 3 (Compose BOM `2026.11.01`), DataStore Preferences, Koin `4.2.2`, type-safe Navigation Compose. No network layer. `minSdk 31`, `compileSdk 37.1` (`release(37) { minorApiLevel = 1 }`), `targetSdk 36`, AGP `9.5.3`, Gradle `9.9.2`. Source root: `app/src/main/java/com/happypuppy/memorylights`.

## Commands

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest   # local CI verification
./gradlew testDebugUnitTest                                                    # JVM unit tests only (no device needed)
./gradlew testDebugUnitTest --tests "com.happypuppy.memorylights.ui.viewmodels.SimonGameViewModelTest"
./gradlew assembleRelease                                                      # R8-minified APK (signed if keystore configured)
./gradlew lintRelease bundleRelease -PversionCode=N -PversionName=X.Y.Z        # release AAB (needs signing config)
python3 tools/sounds/standard/generate.py app/src/main/res/raw                 # regenerate Standard sound pack WAVs
```

Instrumented tests (`app/src/androidTest`) run on an emulator only, never on a personal device. Never run bare `connectedDebugAndroidTest` or bare `adb`; always target an explicit emulator serial:

```bash
./gradlew assembleDebug assembleDebugAndroidTest
adb -s emulator-5558 install -r -t app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5558 install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5558 shell am instrument -w -r com.happypuppy.memorylights.test/androidx.test.runner.AndroidJUnitRunner
```

When multiple agents or sessions share the `Pixel_10` AVD, start each instance with `-read-only` on its own port (`5558` default for Memory Lights; `5554` Day Counter; `5556` Speech Timer).

## Agent Workflow & Tooling

- **Use subagents as needed:** Delegate parallel codebase research, independent code/architecture reviews, or multi-file analysis to subagents to keep the main context focused; always pass bounded scope and concrete deliverables.
- **Use the `android` CLI when needed:** Run `android --help` / `android <cmd> --help` proactively for official AndroidX/Compose docs (`android docs`), Compose preview rendering without a device (`android preview`), APK/AAB inspection (`android analyze`), device/emulator management (`android emulator`, `android device`, `android run`, `android install`), and UI inspection (`android screen`, `android layout`, `android input`).
- **Use the `Pixel_10` emulator for testing as needed:** Verify UI changes, animations, audio/haptics regressions, gesture navigation, and R8-minified signed release builds on `Pixel_10` before shipping. Always pass `-s emulator-NNNN` and use `-read-only` when running parallel emulator instances.
- **Always prefer local CI/CD steps over GitHub Actions whenever possible:** Run verification, builds, emulator tests, releases, and promotion locally via `/Users/guy/dev/android/cicd` (`rex/pipeline.lua` and `scripts/local-*.sh`) rather than GitHub Actions.

## Android & UX Best Practices

- **State & lifecycle:** Keep unidirectional data flow with immutable `StateFlow` UI state and pure, side-effect-free domain helpers (`SequenceTiming`, `GameConstants`, `ColorBlindShape`). Use `rememberSaveable` for transient UI/dialog state that should survive rotation/recreation. Handle audio focus, coroutines, and singleton shutdown cleanly (`viewModelScope`, `DefaultLifecycleObserver`, Koin `onClose`).
- **Accessibility & inclusivity:** Ensure interactive touch targets are at least `48.dp × 48.dp`. Never convey state by color alone — pair color with shapes, icons, audio, or text (e.g., Color-Blind Mode shapes, clear TalkBack descriptions). Support large font scales and respect reduced motion (`animator_duration_scale == 0`).
- **Strings & localization:** Keep all user-facing strings, labels, and content descriptions in `res/values/strings.xml` (use `<plurals>` for counts and XML entities such as `&#169;`, `&#8211;`, `&#8230;` to avoid lint typography warnings). Never hardcode user-visible literals in Composables or leak raw exception messages into UI.
- **UI polish & consistency:** Follow Material 3 color roles and typography cleanly, support edge-to-edge insets without system-bar clipping, adapt gracefully across portrait/landscape/tablets, and display dynamic app version info via `AppVersionInfo` (`PackageManager`) in the About dialog instead of hardcoded version strings.
- **Quality, R8 & Play readiness:** Keep JVM unit tests fast and deterministic (`unitTests.isReturnDefaultValues = true`). Maintain explicit `app/proguard-rules.pro` keep rules for DI/serialization/Activity classes and smoke-test R8-minified signed release builds on `Pixel_10`. Enforce `targetSdk >= 36`, 16 KB ELF alignment for native libs, `ndk.debugSymbolLevel = "SYMBOL_TABLE"`, and clean semantic `versionName` (never commit hashes).

## Architecture

- **DI (`di/AppModule.kt`):** Koin 4.x started in `SimonApp`. Singletons: `SimonSoundManager` (process-lifetime `SoundPool` + `AudioFocus` + `VibratorManager`, released via Koin `onClose` rather than `ViewModel.onCleared()`), `StatisticsRepository`, and `SettingsRepository` (`DataStoreSettingsRepository`), each closing its owned IO scope via Koin `onClose`. ViewModel: `viewModelOf(::SimonGameViewModel)`. `app/proguard-rules.pro` keeps `di.**` and Activity classes for R8.
- **Navigation (`ui/navigation/Routes.kt` & `ui/MainActivity.kt`):** Type-safe `@Serializable` route objects (`GameRoute`, `SettingsRoute`, `StatisticsRoute`, `GameModesRoute`, `GameplayRoute`, `SoundAndHapticsRoute`) wired in `NavHost` with edge-to-edge support and default predictive back handling.
- **State & game engine (`ui/viewmodels/SimonGameViewModel.kt`, `domain/`):**
  - `SimonGameViewModel` manages immutable `StateFlow<SimonGameUiState>` via `.update { it.copy(...) }`, lifecycle-aware via `DefaultLifecycleObserver`.
  - Pure domain logic: `calculateSequenceTiming` (`domain/SequenceTiming.kt`), `GameConstants`, `SimonButton` (4-button `GREEN/RED/YELLOW/BLUE`, 6-button adds `PURPLE/ORANGE`), `GameMode` (`CLASSIC`, `SPEED_BLITZ`), `SoundPack` (7 packs), `ColorBlindShape`, `ThemeMode` (`AMOLED`, `DARK`), and `Achievement`.
- **Persistence (`data/repository/`):**
  - `DataStoreSettingsRepository` (`settings` DataStore, migrates legacy `simon_game_prefs` SharedPreferences): sound pack, 4/6-button high scores and Speed Blitz best times, vibration/sound toggles, difficulty, Memory Lights+, timeout, practice/reverse/audio-only/color-blind/daily-challenge flags, theme mode, and unlocked achievements.
  - `StatisticsRepository` (`statistics` DataStore, migrates legacy `simon_statistics` SharedPreferences): games played, total score, best/current win streaks.
- **Audio & sound packs (`data/manager/SimonSoundManager.kt`, `app/src/main/res/raw/`, `tools/sounds/`):**
  - Resource naming: `{soundpack}_{color}_tone.wav` + `{soundpack}_error_tone.wav` (44.1 kHz mono 16-bit PCM, peak-normalized to `-1.0 dBFS`).
  - `SimonSoundManager` lazy-loads the active pack into `SoundPool`, manages `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`, and degrades gracefully to haptic feedback if sound playback fails.
  - Reproducible Python generators live in `tools/sounds/{standard,electronic,retro,funny,musical,scifi,nature}/generate.py` (see `tools/sounds/SOUND_LICENSES.md`). `standard`, `electronic`, `retro`, and `funny` use Python stdlib only.
- **Utilities & UI:**
  - `util/AppVersionInfo.kt` reads `versionName` and `longVersionCode` dynamically from `PackageManager` for the Settings About card and `AboutDialog` (matching sibling apps).
  - `ui/share/ScoreShareHelper.kt` renders and shares score cards via `FileProvider` (`res/xml/file_paths.xml`).
  - Game UI components live under `ui/components/` (`SimonButtonGrid`, `SimonPanel`, `CenterDiscCanvas`, `CenterDiscContent`, `BlitzHud`, `ColorBlindShapeDraw`, `ParticleEffect`, `PausedOverlay`, `PlayerTurnHalo`, `TimeoutCountdownRing`, `HeadlineOverlay`, `ReplayShareButtons`).

## Testing

- **JVM unit tests (`app/src/test`, `./gradlew testDebugUnitTest`, 113 tests):** `SimonGameViewModelTest`, `DataStoreSettingsRepositoryTest`, `StatisticsRepositoryTest`, `SequenceTimingTest`, `GameConstantsTest`, `SimonButtonTest`, `ColorBlindShapeTest`, `SimonGameUiStateTest`, `GameStateTest`, `AchievementTest`, `FormatBlitzTimeTest`, and `AppVersionInfoTest`.
- **Instrumented tests (`app/src/androidTest`):** Run on `Pixel_10` emulator via explicit `-s emulator-5558`.

## Build, versioning and release

- `versionCode`/`versionName` default to `1` / `1.0` in `app/build.gradle.kts` and are overridden by `-PversionCode`/`-PversionName` or `VERSION_CODE`/`VERSION_NAME` env vars. Always use clean semantic `versionName` (never commit hashes — see `../cicd/AGENTS.md`).
- Release builds enable R8, resource shrinking, `ndk.debugSymbolLevel = "SYMBOL_TABLE"`, and fail the build if `targetSdk < 36` (Play policy gate in `app/build.gradle.kts`).
- Local signing reads `keystore.properties` (git-ignored) or `ANDROID_KEYSTORE_*` env vars; never commit keystores, `keystore.properties`, or service-account files.
- `app/src/main/play/release-notes/` is generated by the local release pipeline from commit subjects (`wip`, `ci`, `chore`, `docs`, `build`, and `merge` commits are dropped; conventional prefixes are stripped; 500 char cap) and is git-ignored. Write commit subjects accordingly.
- Dependency versions live in `gradle/libs.versions.toml`.

## Release and CI/CD (linked repos)

This repo does not hold the release tooling. **Always prefer local CI/CD steps (`rex/pipeline.lua` + `scripts/local-*.sh`) over GitHub Actions whenever possible.**

- CI/CD hub: `/Users/guy/dev/android/cicd` (app alias `memorylights` in `cicd/apps.json`, GCP project `happypuppy-play-cicd`, service accounts `play-ci-testing` and `play-ci-production` with Account-level Play Console permissions, keyless `gcloud` ADC impersonation). Read before any release work:
  - `cicd/app-state.md`: living source of truth (per-app Play state, package names, keystore locations and aliases, what is live on which track).
  - `cicd/AGENTS.md`: workspace rules (clean semver `versionName`, local-first CI/CD policy).
  - `cicd/14-local-and-self-hosted-pipelines.md`: running CI, release, and promote workflows locally.
  - `cicd/release-log.md`: chronological history and lessons.
- Sibling apps on the same pipeline:
  - `SimpleDayCounter` (`com.happypuppy.daycounter`, alias `daycounter`) at `/Users/guy/dev/android/SimpleDayCounter`
  - `SpeechTimer_Jetpack` (`com.happypuppy.toastmasterstimer`, alias `speechtimer`) at `/Users/guy/dev/android/SpeechTimer_Jetpack`
- Run local CI/CD from the hub directory:

```bash
cd /Users/guy/dev/android/cicd
scripts/local-ci.sh --app memorylights
scripts/local-release-internal.sh --app memorylights --use-keystore-props --version-code auto --version-name X.Y.Z
scripts/local-promote.sh --app memorylights --from internal --to production --status completed --yes
rex do /Users/guy/dev/android/cicd/rex/pipeline.lua app=memorylights mode=verify
```

- Rules: Uploading to Play, promoting, or changing a rollout is outward-facing — only with the owner's explicit yes. Release from a clean, merged `main` unless the owner says otherwise (the local script refuses a dirty tree). Never commit keystores, `keystore.properties`, or service-account files; the hub documents where they live, with locations only.

## Known gaps

- `SimonGameScreen.kt` still has deprecated `announceForAccessibility` calls (tracked in `specs/improvements.md` #70).
- Post-approval Play Store polish and roadmap items live in `specs/RESUME.md` and `specs/improvements.md`.
