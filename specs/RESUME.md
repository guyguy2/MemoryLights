# Resume note for next session (written 2026-11-27, Wave 18 - UX, Audio & Settings Polish)

## State at session end (2026-11-27, Wave 18 - `1.0.2 / 3`)

- Branch `main`. Wave 18 changes (`1.0.2 / versionCode 3`) + `#69` cleanup:
  - **Settings & Sound Packs:** Renamed Classic sound description to `"Classic game sounds"` (removed `"Simon"` from user-facing string); synthesized 18 color tones + standardized ~0.5 s `-1 dBFS` error tones across `Electronic`, `Retro Gaming`, and `Funny` (`tools/sounds/{electronic,retro,funny}/generate.py`, `SOUND_LICENSES.md`); added `"Coming soon"` infrastructure (`SoundPack.isAvailable`, visible disabled badge row, auto-fallback to `Standard`).
  - **Pause & Speed Blitz Clock:** Moved `Pause` to leftmost in `TopAppBar.actions` (always rendered, enabled during `PlayerRepeating`, dimmed otherwise); fixed Speed Blitz elapsed clock to exclude paused time (`BlitzHud` + `SimonGameViewModel`); hid `Replay` on Speed Blitz `GameOver`.
  - **Game Over CTA & Difficulty Default:** Added prominent filled `"New Game"` CTA above `Replay`/`Share` (responsive compact landscape `FlowRow`, center disc kept tappable with `24.dp` icon + `Role.Button`); flipped `difficultyEnabled` default to `true` (speed-up starts at level 5).
  - **Cleanup (`#69`):** Gated `MainActivity.checkSoundResources()` on `BuildConfig.DEBUG` using `SimonSoundManager.getResourceId()` across available packs; removed dead `SDK_INT < O` branch in `SimonSoundManager.vibrate()` and dead no-arg `handleGameOver()`.
- **Checks:** `123/123` JVM unit tests (`SoundPackTest` + existing suites), `7/7` instrumented tests on `Pixel_10` (`Wave18UiTest` + `ExampleInstrumentedTest`), `lintDebug` / `lintRelease` `0` errors. Play tracking lives in `../cicd/app-state.md` and `../cicd/release-log.md`.

---

## Previous session (written 2026-11-26, Wave 17 - GitHub cleanup and first Play submission)

## START HERE next session: review the Play submission with the user

Memory Lights 1.0.0 (versionCode 1) was submitted to Google Play production on 2026-11-26. Nothing about the result is known yet. Check these together, in order:

1. **Review status.** Play Console > Memory Lights > Publishing overview (app id `4975069422447709768`, account `HappyPuppyApps`). Console URL: `https://play.google.com/console/u/0/developers/8893200196755101944/app/4975069422447709768/publishing`. States: "Changes in review", approved and live, or rejected (the rejection email carries the policy reason; fix the declarations first).
2. **If live:** install it from the Play listing on a real phone and play a round. Then do the post-approval polish below.
3. **Trader status** is still undeclared (account-level, legal declaration, required by Feb 2027). The user does this; do not do it for them.
4. **Keystore password** must be in the user's password manager. The `.p12` is backed up in Dropbox (`Dev/Android/Android Dev/keystore/key_memoryLights.p12`); the password is not stored anywhere I can see.

### Post-approval polish (needs a new store-listing submission, so wait for approval)

- Change the Play Store listing title to **"Memory Lights: Sequence Game"** (28/30 chars; update Play Console main store listing and `playstore/listing.md`).
- Restyle `playstore/feature_graphic.png` (still the old plain black design) to match the new icon, then re-upload.
- The 7 phone screenshots uploaded in a shuffled order (statistics first); drag gameplay shots to the front.
- Add the Play Store link to the README and the GitHub About section. Upload a GitHub social preview image (web UI only).

### Open follow-ups

- In-app link to the privacy policy (`https://guyguy2.github.io/MemoryLights/privacy.html`); Play expects one.
- Add `debugSymbolLevel = "SYMBOL_TABLE"` to the release build type (Play warned about missing native debug symbols from AndroidX libs; harmless but easy).
- Register Memory Lights in `../cicd/apps.json` (alias `upload`, keystore `~/.config/android-cicd/keystores/key_memoryLights.p12`, package `com.happypuppy.memorylights`, base version `1.0.0`) so future releases use the shared local pipeline.
- Verify the Android 13+ themed (monochrome) icon on a device that actually applies themed icons; the Pixel_10 emulator did not.
- `CLAUDE.md` mentions a test class `ExampleUnitTest` that does not exist; it still lists stale SharedPreferences/StatisticsManager wording.
- Pre-existing dead resources: `drawable/ic_launcher_background.xml`, `drawable/ic_launcher_foreground.xml`, and the robot `mipmap-*/ic_launcher*.webp`.

### How to ship the next version

`keystore.properties` (git-ignored, repo root) holds the signing config. Bump the code every upload:
`./gradlew bundleRelease assembleRelease -PversionCode=2 -PversionName=1.0.1` then upload `app/build/outputs/bundle/release/app-release.aab` to Play Console > Production > Create new release. Keep `versionName` a clean semver (see `../cicd/AGENTS.md`). R8 is on: always smoke-test the signed release APK on Pixel_10 before uploading. Upload key SHA-256 (public): `72:48:18:1B:B3:5B:AC:F0:F6:12:FF:C3:85:4E:C6:9E:28:77:D0:C8:23:72:61:B7:A3:88:BE:62:FE:5F:B7:B8`.

### Declarations made on the user's behalf (revisit if the app changes)

- Content rating (IARC, category Game): every question answered "No". Two judgment calls: user interaction "No" (the Share button only hands a score image to the Android share sheet), and Language/Crude Humor "No" (the user confirmed the Funny sound pack has nothing crude or spoken). Re-run the questionnaire if a feature changes either.
- Target age 13-15, 16-17, 18+ (under-13 groups left out on purpose to avoid the Families policy). Data safety: no data collected or shared. Ads: none.
- Distribution: 178 countries/regions, 100% rollout, Google-generated app signing key. Google Play Games on PC: opted out (before submitting). Android XR: left active (the console default).
- Store listing copy lives in `playstore/listing.md` and matches the Console. Release name `1.0.0`.

---


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
