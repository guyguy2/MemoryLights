# Resume note for next session (written 2026-07-10, Wave 9)

Canonical roadmap: `specs/improvements.md`. This file is a tactical pointer; if it disagrees with `specs/improvements.md`, trust the spec.

## State at session end (2026-07-10, Wave 9)

- Branch `main`. Working tree clean after this wave's single commit. Local commits ahead of origin by 18 (no push during background-agent waves).
- HEAD: Wave 9 `refactor: extract CenterDiscContent from SimonGameScreen` on top of Wave 8 docs head `6dc0cf7`.
- 82/82 unit tests green (unchanged — pure UI extract, no logic moved).
- `./gradlew assembleDebug testDebugUnitTest` succeeds. Lint not re-run this wave.
- No emulator at session end — byte-equivalent inner-disc content, no behavioural surface to verify live.

## What changed this session (Wave 9)
Wave 8 (`CenterDiscCanvas`) was already on main at session start. Next clean mechanical seam:

- **`CenterDiscContent` extract** — the center-disc sound-pack chip + level/loading/game-over column + high-score footer (~180 lines of branching UI) lifts into `ui/components/CenterDiscContent.kt` (260 lines including KDoc + shared `formatBlitzTime`). Eleven explicit parameters (gameState, gameMode, level, scores, blitz times, load state, sound pack res id, sequence sizes) — no whole-UiState leak. `formatBlitzTime` moves out of the screen so GameOver blitz copy and private `BlitzHud` share one formatter.
- **`SimonGameScreen.kt` 867 → 687** (-180). Remaining private composable: `BlitzHud` only (already isolated; lift is optional).

#35 color-blind mode still skipped (needs UX: numeric labels vs distinct shapes vs both). Landing zones ready: `SimonButtonGrid` + `CenterDiscCanvas`.

## Recommended next batch
1. **#35 Color-blind mode** (M) once UX is decided.
2. Optional: lift `BlitzHud` to `ui/components/` (S, low value alone).
3. **#32 Progressive onboarding** (M, UX-blocked).
4. **Phase 4 Play Store prep** (user-action only).

## State at session end (2026-05-19, Wave 8)

- Branch `main`. Working tree clean after this wave's single commit. Local commits ahead of origin by 16 (no push during background-agent waves).
- HEAD: Wave 8 commit `refactor: extract CenterDiscCanvas from SimonGameScreen` on top of Wave 7 `d8b1ae0`.
- 82/82 unit tests green (unchanged — extraction has no logic, only Canvas draw calls).
- `./gradlew assembleDebug testDebugUnitTest` succeeds. Lint not re-run this wave.
- No emulator at session end — pure mechanical extraction with byte-equivalent arc-drawing math, no behavioural surface to verify live.

## What changed this session (Wave 8)
Single mechanical extraction matching the Wave 2/3/4 thread (#17 `SimonButtonGrid`, #65 `HeadlineOverlay`, the four Wave-4 components):

- **`CenterDiscCanvas` extract** — the inline center-disc Canvas in `SimonGameScreen` (~110 lines of arc-drawing math across two branches) lifts into `ui/components/CenterDiscCanvas.kt` (141 lines). One parameter: `memoryLightsPlusEnabled: Boolean`. The 6-button branch uses `360f / 6 = 60f` sweepAngle and per-arc `startAngle = index * 60f` over the canonical `[RED, BLUE, ORANGE, YELLOW, PURPLE, GREEN]` clockwise order; the 4-button branch hardcodes 90f sweeps with explicit `startAngle`s (GREEN 180, RED 270, BLUE 0, YELLOW 90). Byte-equivalent to the prior inline blocks — colors, stroke width (8dp), `StrokeCap.Butt`, `Offset(strokeWidth / 2, strokeWidth / 2)` topLeft, and `size.copy(width = ..., height = ...)` size all preserved verbatim.
- **`availableButtons` derivedState removed from the screen** — its only use was the `sweepAngle = 360f / availableButtons.size` divisor inside the Canvas. Since `SimonButton.getAvailableButtons(true).size == 6` and `getAvailableButtons(false).size == 4` by enum construction, the new component uses a literal `arcCount = if (memoryLightsPlusEnabled) 6 else 4` — semantically identical, fewer moving parts. The screen drops the `availableButtons` `by remember(...) { derivedStateOf { ... } }` block entirely.
- **Imports trimmed from `SimonGameScreen`**: `androidx.compose.foundation.Canvas`, `androidx.compose.ui.geometry.Offset`, `androidx.compose.ui.graphics.StrokeCap` all removed (no remaining callers). `androidx.compose.ui.graphics.drawscope.Stroke` was already fully-qualified inline so no import to remove.

File line counts: `SimonGameScreen.kt` 975 → 867 (-108). The Wave-4 doc-quoted 940 was stale — Wave-5 + Wave-7 F8 wiring (`SnackbarHost` + `unlockEvents` collection + helper composables) added ~35 lines back. New file: 141 lines.

Triage note: tree at session start had this work in-flight from a prior wave-8 agent that bailed on credits — modified `SimonGameScreen.kt` (Canvas removed + `CenterDiscCanvas` call wired) and new untracked `ui/components/CenterDiscCanvas.kt`. Inspected via `git diff` + full read of the new file. The extraction was clean and byte-equivalent; the only work this wave was the build/test verification + the log entries + the commit. No code rewrites or surgical fixes.

#35 (color-blind mode) skipped per the session brief — still needs the UX call (numeric labels vs distinct shapes vs both). `CenterDiscCanvas` is now the obvious landing zone for per-arc labels.

## State at session end (2026-05-17, Wave 7)

- Branch `main`, working tree clean. Local commits ahead of origin by 14 (no push during background-agent waves).
- HEAD: Wave 7 commit `d8b1ae0` `feat(F8): unlock toast + settings entrypoint card`. Prior F8 work in `0a59583` (stats-screen wire) + `4ec8c80` (enum + DataStore).
- 82/82 unit tests green (was 80 — +2 new `SimonGameViewModelTest` cases for the unlock-event channel).
- `./gradlew assembleDebug testDebugUnitTest` succeeds. Lint not re-run this wave.
- No emulator at session end — toast is one composable + a Channel emission; the snackbar is byte-trivial to verify live but doesn't change any game-loop behaviour.

## What changed this session (Wave 7)
**F8 polish** — the two follow-ups deliberately scoped out of the Wave 5 initial cut, now landed in a single commit:

- **Unlock toast**: New `Channel<List<Achievement>>` + `unlockEvents: Flow<List<Achievement>>` on `SimonGameViewModel`. Lives outside `SimonGameUiState` because an unlock is a one-shot event — collecting state on rotation must not re-fire a stale toast. `evaluateAndPersistAchievements` computes the delta in enum-declaration order and `trySend`s it after persisting; re-unlocks short-circuit before the send (no toast for badges already earned). Channel closes in `onCleared` so in-flight collectors complete cleanly.
- **Snackbar in `SimonGameScreen`**: New `unlockEvents: Flow<List<Achievement>>` parameter (defaulted to `emptyFlow()` for previews / non-VM callers); the screen hosts a `SnackbarHostState` and shows a Short snackbar per emission. Single-badge form uses the badge title; multi-badge collapses to "N achievements unlocked" (rare — only on first deep-progress game-over).
- **Settings entrypoint card**: New "Achievements" `NavCard` above the Statistics row on `SettingsScreen` with summary "X / 8 unlocked. Tap to view all." Fans through the existing `onStatisticsClicked` so the full list lives on exactly one route — the card is a shortcut, not a duplicated surface.
- **Wiring**: `MemoryLightsGame` passes `viewModel.unlockEvents` to the game screen and `uiState.unlockedAchievements` to the settings screen.
- **Tests**: +2 `SimonGameViewModelTest` cases — `unlockEvents emits newly unlocked achievements in enum order` (FirstWin fires for level-1 game-over when only that delta is new) + `unlockEvents does not emit when already-unlocked badge re-evaluates`. Uses `backgroundScope.launch` to collect into a list without TestScope coroutine-scope lint.
- **Strings**: 4 new in `values/strings.xml` — `achievement_unlocked_toast`, `achievement_unlocked_toast_multi`, `settings_achievements_title`, `settings_achievements_summary`.

#35 color-blind mode skipped per the brief — still needs a UX decision (numeric labels vs distinct shapes vs both). Wave 7 cap was 1 plan chunk OR ≤5 small fixes; F8 polish was one logical chunk (1 commit).

## State at session end (2026-05-17, Wave 5)

- Branch `main`, working tree clean. Local commits ahead of origin by 13 (no push during background-agent waves).
- HEAD: Wave 5 commit `0a59583` `feat(F8): wire achievements through ViewModel + Statistics row`. Prior F8 step in `4ec8c80` `feat(F8): add Achievement enum + DataStore persistence`.
- 80/80 unit tests green (was 64 — +13 F8 step 1 datastore/enum coverage, +4 F8 step 2 VM cases).
- `./gradlew assembleDebug` succeeds. Lint not re-run this wave.
- No emulator at session end — F8 is a stats-screen + persistence feature, no game-flow behaviour change beyond the new write path.

## What changed this session (Wave 5)
Single feature landed: **F8 local achievements**. The dirty tree at session start was in-flight F8 stats-screen work — inspected via `git diff`, all five files were already coherent (Achievement enum reachable, strings present, repo setter live, GameConstants.BLITZ_TARGET_LEVEL + VM `currentEpochDay` injection seam both in place). Only fix needed was a trailing newline on `StatisticsScreen.kt`. The wave was finish-and-commit, not finish-and-implement.

See `specs/improvements.md` Session 2026-05-17 entry for the full detail. Highlights:
- `SimonGameViewModel.evaluateAndPersistAchievements(...)` called from both `handleBlitzWin` (BLITZ_TARGET_LEVEL + elapsedMs so Speedrun30s sub-30s fires) and `updateGameOverState` (currentLevel + gamesPlayed+1 + dailyChallengeEnabled). Streak unlocks fire on the *next* game-over because the repo owns the previous-level cursor.
- `AchievementsSection` composable on `StatisticsScreen` — "N / 8 unlocked" header + one Card per badge (locked: Lock + grey; unlocked: CheckCircle + yellow). TalkBack `contentDescription` per card announces lock state + description.
- 4 new VM tests: FirstWin at level 1, FirstWin + DailyDone together, hydration from AppSettings on init, re-unlock no-op write.

## State at session end (2026-05-16, Wave 4)
- Branch `main`, working tree clean. Local commits ahead of origin (no push during background-agent waves — now ahead by 9 commits).
- HEAD: Wave 4 commit (PlayerTurnHalo). Recent: `f0390a3` PlayerTurnHalo -> `2630084` TimeoutCountdownRing -> `fdb4aa3` ReplayShareButtons -> `20bf82e` PausedOverlay -> Wave 3 head (`acecc4d`).
- 64 unit tests green (was 64). Debug APK builds clean.
- No emulator running at session end (Wave 4 was four mechanical extractions — byte-equivalent to the prior inline blocks).

## What changed this session (Wave 4)
Continued the Wave 2/Wave 3 extraction pattern. Four new components in `ui/components/`:

1. **`PausedOverlay`** — Full-screen Paused dim/icon/copy block (F14). Self-contained semantics + tap-to-resume. Lets the screen drop `semantics`/`contentDescription` imports.
2. **`ReplayShareButtons`** — GameOver Replay + Share row. The share-helper call stays in the screen as an `onShare` lambda so the component doesn't need context/level/high-score/6-button awareness. Drops `BorderStroke`, `OutlinedButton`, `ButtonDefaults`, `Icons.Refresh`, `Icons.Share`, `AnimatedVisibility`, `fadeIn`/`fadeOut`/`scaleIn` from the screen file.
3. **`TimeoutCountdownRing`** — PlayerRepeating inactivity ring around the center disc. Takes `timeoutMs` + `resetTick`. Drops `Animatable`, `LinearEasing`, `Size`, `lerp`, `drawscope.Stroke` imports.
4. **`PlayerTurnHalo`** — Persistent pulsing halo during PlayerRepeating. No parameters (size/curve/colors all constant). Caller-side `gameState` gate stays in the screen.

File line counts: `SimonGameScreen.kt` 1088 → 940 (-148 cumulative). Below 1000 for the first time since Speed Blitz HUD + timeout ring landed. The center-disc Canvas (4-button + 6-button arc drawing, ~100 lines), the level-display Column inside the disc, and the BlitzHud private composable are the next-largest remaining blocks — all couple to multiple state fields and can't be ripped out as cleanly as the four above.
- Branch `main`, working tree clean. Local commits ahead of origin (no push during background-agent waves).
- HEAD: Wave 3 commit (#65 — `HeadlineOverlay` extracted from `SimonGameScreen`).
- Recent commits in order: Wave 3 (#65 HeadlineOverlay) → `236d192` Wave 2 doc → `f5b5603` Phase 11 (#17 SimonButtonGrid) → `320245a` Phase 10 (#24 CoroutineScope) → `919f52c` Phase 9 roadmap → `ba682e5` F12 theme → `0e92227` #58 typography → `9b498b4` Phase 8 strings + F6 daily.
- 64 unit tests green. Lint clean. Debug APK builds clean.
- No emulator running at session end (Wave 3 was a pure structural refactor — no behavioural surface to verify live).

## What changed last session (2026-05-16, Wave 3)
Single-item background pass — mechanical extraction matching the Wave 2 pattern:

- **#65 `HeadlineOverlay` extract** — The four center-anchored text overlays above the game disc (HIGH SCORE / BEST TIME, GAME OVER / VICTORY, SPEED UP, YOUR TURN) inlined the same `AnimatedVisibility { Box(fillMax + Center) { Text(...) } }` wrapper four times. New `ui/components/HeadlineOverlay.kt` consolidates the pattern; parameters are `visible`, `text`, `color`, `style`, `alignment`, `initialScale`, `useScaleOut`. The three `OverlayBias*` constants (previously `private val` in `SimonGameScreen.kt`) moved alongside the composable as top-level `val`s so the screen and the overlay share one source of truth. YOUR TURN's plain `fadeOut()` (no `scaleOut`) is captured by `useScaleOut = false`. Variance preserved verbatim: animation curves, scale targets, text/style/color choices, and bias alignments are byte-equivalent to the previous inline blocks. `BiasAlignment` + `scaleOut` imports removed from the screen.
- File line counts: SimonGameScreen.kt 1134 → 1088 (-46). New file: 85 lines.

#35 (color-blind mode) was the originally-recommended pickup but skipped per session brief — it needs a UX decision (numeric labels vs distinct shapes vs both) before any code lands. Wave 3 picked the next mechanical seam instead.

## Recommended next batch (Wave 10+)
`CenterDiscContent` extraction closed Wave 9. Cheap mechanical seams in `SimonGameScreen` are exhausted (687 lines; only private `BlitzHud` remains).

1. **#35 Color-blind mode** (M). Pattern/icon overlay on buttons + per-arc labels on the center disc. Landing zones: `SimonButtonGrid` + `CenterDiscCanvas`. Needs UX decision first.
2. **#32 Progressive onboarding / first-run tutorial** (M). UX-blocked.
3. **Phase 4 Play Store prep wrap-up**. Keystore + Play Console submission are user-action only.

## Next: pick #35 once UX decision is made (numeric labels vs distinct shapes vs both).

## Verification flow
Standard:
1. `./gradlew test lint`
2. `./gradlew installDebug`
3. `adb shell am force-stop com.happypuppy.memorylights && adb shell am start -n com.happypuppy.memorylights/.ui.MainActivity`
4. `android screen capture -o /tmp/ml_X.png` + `android layout` for state assertions
5. Spot-check on the running Pixel_8_API_34 emulator

**Wave 3 specifically**: live emulator verification was skipped because no emulator was running and the change is byte-equivalent to the prior inline overlays (no animation/easing/scale edits, no semantics changes). If the next session wants belt-and-braces, run the standard flow above and trigger each overlay: a fresh game over (HIGH SCORE / GAME OVER), a level 5 cross with difficulty on (SPEED UP), and PlayerRepeating round 1 (YOUR TURN).

**Wave 4 specifically**: same reasoning — no emulator at session end, four mechanical extractions, sizes/curves/colors all byte-equivalent. If the next session wants live verification, trigger: a paused mid-round (PausedOverlay tap-to-resume), a game over with sequence (Replay + Share row), the inactivity ring drain after sitting still during PlayerRepeating, and the halo pulse for ≥3 rounds (after YOUR TURN stops appearing). Lint not re-run this wave — assembleDebug + testDebugUnitTest only, both green.

## User preferences (do not re-derive)
- Forward motion preferred — skip brainstorming skill on routine "keep going" prompts.
- Multi-paragraph commit bodies explaining "why," not "what."
- Push at session end (background-agent sessions: local commits only, no push).
- Use the existing emulator rather than starting a new AVD.
- Surface scope cuts during work, not after the fact.
- Use `android docs` proactively for AndroidX/Compose API lookups.

## Open audit (P2)
#32 onboarding (M), #34 reduce-motion (previously cut), #35 color-blind (M).

## Open features
F11 GPGS (L, post-launch v1.1), F13 per-button color (L), F17 IAP (XL, decision-gated), Light theme variant of F12 (deferred — needs game-screen `Color.White` audit first). F8 achievements closed Wave 5; post-MVP polish (unlock toast + settings entrypoint card) optional.

## Resume prompt
Paste in next session:

```
Read specs/RESUME.md and specs/improvements.md. Confirm git is at the Wave 9 head on main with clean tree (Wave 9 = `refactor: extract CenterDiscContent from SimonGameScreen`). Cheap mechanical SimonGameScreen extractions are exhausted (687 lines). Next major item is #35 color-blind mode, which needs a UX decision (numeric labels vs distinct shapes vs both) before any code can land. Landing zones ready: SimonButtonGrid + CenterDiscCanvas. Alternatives: #32 progressive onboarding (UX-blocked), or Phase 4 Play Store prep (user-action only). Ask the user which path to take before starting.
```
