# Resume note for next session (written 2026-05-03)

Canonical roadmap: `specs/improvements.md`. This file is a tactical pointer; if it disagrees with `specs/improvements.md`, trust the spec.

## State at session end
- Branch `main`, working tree clean, all pushed to `origin/main`.
- HEAD: `5d80306` — drop F2 (already shipped as mute button).
- Last 4 feature commits: `cc75590` F9, `522e309` F16, `f5c93a5` F14, `30e53ef` F15. Phase 6 step 1–4 all shipped.
- Recent housekeeping: `4f00320` defer #36, `a83e1fa` drop #37, `5d80306` drop F2.
- 52 unit tests green, lint clean, release APK ~7.5 MB.
- Pixel_8_API_34 emulator usually running. User asked me to use the running one rather than start a new AVD.

## Recommended next batch (one commit, S-effort cluster)
Cluster: **F1 Reverse Mode + F7 Win-streak + #34 Reduce-motion + F5 Smoother difficulty curve.** All isolated, all small, fits one logical commit ("Phase 6 step 5: P2/feature S-effort cluster").

Per item:

1. **F1 Reverse Mode** — `practiceModeEnabled`-shaped pattern: `reverseModeEnabled` DataStore key + AppSettings field + UiState field + setter + Settings toggle card. In `checkSequenceMatch`, compare against `sequence.reversed()[index]` when flag set. Edge: only invert during `PlayerRepeating`; sequence display still plays forward (player watches forward, repeats backward — that's the puzzle).
2. **F7 Win-streak** — already a `bestStreak: Int` slot in `GameStatistics`. Currently never updated. Decide definition: "consecutive runs that beat your previous best level." Update in `StatisticsRepository.recordGameResult(level, sequenceLength)` — track previous level, increment streak if level > previous, else reset to 0; `bestStreak = max(bestStreak, currentStreak)`. Render in StatisticsScreen (already wired).
3. **#34 Reduce-motion** — `reduceMotionEnabled` DataStore key + AppSettings + UiState + Settings toggle. Gate the major animations: WaitingToStart pulse, PlayerRepeating halo pulse, `litProgress` ramp, "Speed Up!" scaleIn, countdown ring. When enabled, snap rather than tween. Skip the lit-color lerp — instant on/off.
4. **F5 Smoother difficulty curve** — `domain/SequenceTiming.kt` already extracted (Phase 2 step 1). Currently linear: `(currentLevel - 1) / DIFFICULTY_INTERVAL` reduces lit + pause durations by 20% per tier. Replace with logarithmic: e.g. `reductionFactor = 1 - (ln(level) / ln(maxLevel)) * 0.7` clamped to floors (`MIN_LIT_DURATION_MS`, `MIN_PAUSE_DURATION_MS`). Adjust `SequenceTimingTest` expectations.

Each gets a unit test where reasonable (F1 + F5 are easy to test; F7 + #34 trickier — F7 needs StatisticsRepository test, #34 is a UI gate).

## Verification flow
Standard:
1. `./gradlew test lint`
2. `./gradlew installDebug`
3. `adb shell am force-stop com.happypuppy.memorylights && adb shell am start -n com.happypuppy.memorylights/.ui.MainActivity`
4. `android screen capture -o /tmp/ml_X.png` + `android layout` for state assertions
5. Spot-check each new toggle on the running Pixel_8_API_34 emulator

## Alternative paths (user asked me to surface)
- **Phase 4 Play Store prep** — the actual launch step. Signed AAB, listing copy, screenshots, privacy policy. Highest leverage if launch is the real goal. User has not committed to "ship now" vs "polish more." Ask if context changed.
- **#58 Typography + F12 theme toggle pair** — bigger single deliverable; #58 is mechanical migration; F12 is trivial once #58 done.
- **F10 Share score card** — M effort, high marketing/organic-acquisition value. Pairs with already-shipped #46.

## User preferences (do not re-derive)
- Forward motion preferred — skip brainstorming skill on routine "keep going" prompts.
- Multi-paragraph commit bodies explaining "why," not "what."
- Push at session end.
- Use the existing emulator rather than starting a new AVD.
- Caveman mode (full) is active by default in this project's session — skip articles/filler in chat, but commits/code/security stay normal English.

## Open audit (P2)
#17 SimonButtonGrid extract (M), #20 partial / depends on #17, #24 CoroutineScope cancel (M), #32 onboarding (M), #34 reduce-motion (S), #35 color-blind (M), #48 strings.xml (M), #58 Typography (M).

## Open features
F1 reverse (S), F3 audio-only (M), F4 blitz (M), F5 difficulty curve (S), F6 daily (M), F7 streak (S), F8 achievements (M), F10 share (M), F11 GPGS (L, post-launch), F12 theme (S, blocked on #58), F13 per-button color (L), F17 IAP (XL, decision-gated).

## Resume prompt
Paste in next session:

```
Read specs/RESUME.md and specs/improvements.md. Confirm git is at HEAD 5d80306 on main with clean tree. Then start Phase 6 step 5: F1 Reverse Mode + F7 Win-streak + #34 Reduce-motion + F5 Smoother difficulty curve as one S-effort cluster. Stop after the cluster lands + tests + verifies on emulator + commits + pushes. If anything blocks, prefer to roll back rather than power through.
```
