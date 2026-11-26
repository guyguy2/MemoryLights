# Resume note for next session (written 2026-11-25, Wave 15 - game-over and practice-replay race fixes)

Canonical roadmap: `specs/improvements.md`. This file is a tactical pointer; if it disagrees with `specs/improvements.md`, trust the spec.

## State at session end (2026-11-26, Wave 16 - new app icon)

- New adaptive launcher icon, monochrome layer and `playstore/icon_512.png`. Details in `specs/improvements.md` "Wave 16". No code changes; `./gradlew assembleDebug lintDebug` passes (0 errors).
- Verified in the Pixel_10 app drawer. The themed-icon silhouette is unverified on a device.
- Dependency check the same day: AGP 9.5.3, Kotlin 2.4.21, Gradle 9.9.2 and every other library are on their latest stable release.

## State at session end (2026-11-25, Wave 15 - game-over race fixes)

- Branch `main`. Wave 15 commits: `f51afca` (#66/#67), `6145766` (docs), `c71e301` (#73), plus a docs commit. Check `git status -sb` for push state.
- #66, #67 and #73 closed. The practice-mode replay uses the same tracked turn-end step; the Settings-entry snapshot is taken after `showSettings()`.
- #66 and #67: Pending game-over steps are tracked; Settings/background finish a loss immediately, mode switches cancel it. One error tone per timeout. Details in `specs/improvements.md` "Wave 15".
- 111/111 unit tests (9 new this wave, all red before the fixes). `./gradlew build` passes; lint 0 errors / 29 warnings.
- Emulator-verified on Pixel_10 (Settings opened 0.6 s after a miss; timeout tone count).
- Next candidates: #70 + #72 (game-screen emulator pass), #68/#69 (need a product decision).

## State at session end (2026-11-25, Wave 14 - targetSdk 36)

- Branch `main`. One build commit (`targetSdk = 36`) plus a docs commit on top of Wave 13 (which is pushed). Wave 14 commits are pushed (origin/main at ddc81af).
- Not verified: TalkBack (see #70 note), a real tablet AVD (only a `wm density 240` override), 6-button mode in landscape.
- #71 closed: targetSdk 36, which Play requires for uploads. No code fixes were needed. Only predictive back applies, and NavHost already handles it. Details are in `specs/improvements.md` "Wave 14".
- 102/102 unit tests green. `./gradlew build` passes; lint 0 errors / 29 warnings (`OldTargetApi` now means 37).
- Verified on Pixel_10 (API 37, gesture nav) with debug and signed release builds. Back gestures were driven by `input motionevent` (cancelled and committed edge swipes), not `keyevent 4`.
- New follow-up: #72 (center-disc text overlap in landscape / sw600dp+, pre-existing). Added a predictive-back note to #70.
- Next candidates: #70 (Compose live regions, TalkBack pass), #72, #66-#69.

## State at session end (2026-11-25, Wave 13 - toolchain + dependency upgrade)

- Branch `main`. Three build commits plus a docs commit, local only (not pushed).
- Gradle 9.9.2, AGP 9.5.3 (built-in Kotlin, no `kotlin-android` plugin), Kotlin 2.4.21, compileSdk 37.1, Compose BOM 2026.11.01, Koin 4.2.2. Details in `specs/improvements.md` "Wave 13".
- 102/102 unit tests green. `./gradlew build` passes; lint 0 errors / 29 warnings.
- **Build environment:** the default Android Studio JBR 25 now works. The JDK 17 `JAVA_HOME` workaround below (Wave 12) is obsolete.
- **Emulator:** `Pixel_8_API_34` no longer exists. Use `Pixel_10` (API 37). Debug and signed release were smoke-tested on it this wave.
- New follow-ups: #70 (`announceForAccessibility` deprecated at API 37), #71 (targetSdk 36 needed for Play upload).

## State at session end (2026-09-12, Wave 12 - three ViewModel bug fixes)

- Branch `main`. Working tree clean after this wave's four commits. Local commits ahead of origin (no push during background-agent waves).
- HEAD: `docs: next-session resume block after Wave 12` on top of `2e962fe fix(di)`, `c0f7be8 fix(vm)`, `6553732 fix(vm)`, then Wave 11 `8224fa6`.
- 102/102 unit tests green (95 + 7 new `SimonGameViewModelTest` cases). `./gradlew assembleDebug testDebugUnitTest lintDebug` succeeds; lint 0 errors / 42 warnings (GradleDependency 13, UseKtx 11, UnusedResources 8, PluralsCandidate 3, ModifierParameter 2, AGP version 2, VectorPath, OldTargetApi, ObsoleteSdkInt).
- No device used. All three fixes are ViewModel/DI logic with unit coverage.
- **Build environment note:** Gradle 8.14.2 rejects the default Android Studio JBR (Java 25; the build fails with the bare message `25.0.2`). Run Gradle with `JAVA_HOME=~/.gradle/jdks/eclipse_adoptium-17-aarch64-os_x.2/jdk-17.0.19+10/Contents/Home` (JDK 21 under `~/.gradle/jdks` also works). Not committed to the repo; environment only.

## What changed this session (Wave 12)

Roadmap items were all UX/user/product-gated (#32, Phase 4, F13/F12/F11/F17), so this wave audited `SimonGameViewModel` for lifecycle bugs and fixed three, each with reproducing tests written first:

1. `6553732` **Input lock after a miss/timeout.** `gameState` stays `PlayerRepeating` through the 300 ms error-tone delay and the 2 s game-over flash; presses there were still scored (level advance after a miss, double `recordGameResult`, double practice-mode replay). New private `isInputLocked`, set on miss/timeout, cleared in `showSequence` / `updateGameOverState`.
2. `c0f7be8` **Game frozen while on Settings.** New private `isInSettings` (set in `showSettings`, cleared in `onReturnToGame`). `onResume` no longer re-arms the timer / replays the sequence on the Settings screen; the three rules-change setters share `restartRunAfterModeChange()` which defers the new run to `onReturnToGame`'s WaitingToStart branch while on Settings; `onReturnToGame` is a no-op until the startup animation has run (it used to call `startNewGame()` on cold start before DataStore answered).
3. `2e962fe` **SoundManager release moved to Koin `onClose`.** `onCleared()` released the process-lifetime `SimonSoundManager` singleton, so reopening the app in a warm process had no audio.

## Found but not fixed (candidates for Wave 13)

See the P2 table rows #66-#69 in `specs/improvements.md`. Highest value: #66 (untracked delayed game-over coroutines can land on the next run if the player enters Settings / switches mode within ~2.3 s of a miss).

## Recommended next batch
1. #66 track the delayed game-over jobs (S) - closes the remaining hole behind Wave 12 items 1 and 2.
2. #67 timeout double error tone (S) and #68 practice mode does not cover timeouts (product question: should a timeout replay in practice mode?).
3. #69 `MainActivity.checkSoundResources` debug logging: only knows standard/funny packs, logs "Resource missing" for the other five in release too (S).
4. Then the previously listed gated items: #32 onboarding (UX), Phase 4 Play Store (user), F13 / Light F12 / F11 / F17 (product).

## State at session end (2026-07-10, Wave 11 — BlitzHud extract)

- Branch `main`. Working tree clean after this wave's single commit. Local commits ahead of origin (no push during background-agent waves).
- HEAD: Wave 11 `refactor: extract BlitzHud to ui/components` on top of Wave 10 #35 `1f42b67`.
- 95/95 unit tests green (unchanged — pure UI extract, no logic moved).
- `./gradlew assembleDebug testDebugUnitTest` succeeds. Lint not re-run this wave.
- No emulator needed — byte-equivalent HUD lift, no behavioural surface to verify live.

## What changed this session (Wave 11)

**SKIP #32 onboarding** (user decision). Picked optional mechanical extract from recommended next batch.

- **`BlitzHud` lift** — private Speed Blitz progress + live timer HUD moves from `SimonGameScreen` into `ui/components/BlitzHud.kt` (~90 lines including KDoc). Same parameters, 10 Hz tick via `System.currentTimeMillis()`, pause freeze, `formatBlitzTime` for elapsed/best.
- **`SimonGameScreen.kt` 691 → 625** (-66). No remaining private game HUD composables in the screen file; game-area components now live under `ui/components/`.
- KDoc on `formatBlitzTime` updated to link public `[BlitzHud]`.

## Recommended next batch
1. **#32 Progressive onboarding** (M, still UX-blocked — skip until UX decision).
2. **Phase 4 Play Store prep** (user-action only — keystore + Console).
3. F13 per-button color picker (L — complements #35 for custom palettes).
4. Light theme variant of F12 (deferred — needs game-screen `Color.White` audit first).
5. Cheap seams in game screen are exhausted; next value is product/feature or user-gated launch work.

## State at session end (2026-07-10, Wave 10 — #35 Color-blind mode)

- Branch `main`. Working tree clean after this wave's single commit. Local commits ahead of origin (no push during background-agent waves).
- HEAD: Wave 10 `feat(#35): color-blind mode with shape overlays` on top of prior Wave 10 test commit `161a429`.
- 95/95 unit tests green (was 90 — +3 shape mapping, +1 DataStore, +1 VM).
- `./gradlew assembleDebug testDebugUnitTest` succeeds. Lint not re-run this wave.
- No emulator needed for the pure-presentation toggle; live smoke optional (Gameplay → Color-Blind Mode On → shapes on panels + disc arcs).

## What changed this session (Wave 10 — #35)

**Product decision (documented):** geometric shapes per color, not numeric labels. Default OFF preserves classic look. Optional small glyphs on center-disc arcs included (shared draw helper).

- Domain: `ColorBlindShape` enum + `forButton` mapping (circle/triangle/square/cross/diamond/stripes).
- Draw: `ColorBlindShapeDraw.kt` (fill+outline glyphs, arc midpoint helper).
- UI: `SimonPanel` overlay, `SimonButtonGrid` + `CenterDiscCanvas` wiring.
- Prefs: `color_blind_mode_enabled` DataStore key through AppSettings → UiState → VM setter (safe mid-run).
- Settings: toggle on `GameplayScreen` with snackbar; hub summary updated.

## Verification flow
Standard:
1. `./gradlew test lint`
2. `./gradlew installDebug`
3. `adb shell am force-stop com.happypuppy.memorylights && adb shell am start -n com.happypuppy.memorylights/.ui.MainActivity`
4. Spot-check blitz (optional): Game Modes → Speed Blitz → play — HUD still top-center with progress + timer.

## User preferences (do not re-derive)
- Forward motion preferred — skip brainstorming skill on routine "keep going" prompts.
- Multi-paragraph commit bodies explaining "why," not "what."
- Push at session end (background-agent sessions: local commits only, no push).
- Use the existing emulator rather than starting a new AVD.
- Surface scope cuts during work, not after the fact.
- Use `android docs` proactively for AndroidX/Compose API lookups.

## Open audit (P2)
#32 onboarding (M), #34 reduce-motion (previously cut). #35 color-blind closed. BlitzHud extract closed.

## Open features
F11 GPGS (L, post-launch v1.1), F13 per-button color (L), F17 IAP (XL, decision-gated), Light theme variant of F12 (deferred — needs game-screen `Color.White` audit first). F8 achievements closed.

## Resume prompt
Paste in next session:

```
Read specs/RESUME.md and specs/improvements.md. Confirm git is at the Wave 13 docs commit on main with clean tree. Gradle 9.9.2 runs on the default Android Studio JBR; verify on the Pixel_10 AVD. 102 unit tests green, lint 0 errors. Next: #71 targetSdk 36 (blocks Play upload), P2 #66 (track delayed game-over jobs), #67/#68 timeout polish, #69 MainActivity debug logging; then the gated items (#32 onboarding, Phase 4 Play Store, F13/F12/F11/F17). Ask the user which path to take before starting product work.
```
