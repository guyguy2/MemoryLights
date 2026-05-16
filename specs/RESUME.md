# Resume note for next session (written 2026-05-16)

Canonical roadmap: `specs/improvements.md`. This file is a tactical pointer; if it disagrees with `specs/improvements.md`, trust the spec.

## State at session end (2026-05-16)
- Branch `main`, working tree clean.
- HEAD: phase 10 commit (audit #24 — `CoroutineScope` cleanup in `StatisticsRepository` + `DataStoreSettingsRepository`).
- Recent feature/audit commits in order: Phase 10 (#24 scope cleanup) → `919f52c` Phase 9 roadmap update → `ba682e5` F12 theme → `0e92227` #58 typography → `9b498b4` Phase 8 strings + F6 daily → `57e3378` Phase 7 roadmap → `59ca898` F3 + F4.
- 64 unit tests green (was 60; +4 covering owned/injected scope branches per repo).
- Lint clean, debug build clean.
- Pixel_8_API_34 emulator usually running. User asked to use the running one rather than start a new AVD.

## What changed this session
Phase 10 — single audit item, background pass:

- **#24 `CoroutineScope` cancellation** — Both `StatisticsRepository` and `DataStoreSettingsRepository` previously held a `CoroutineScope(Dispatchers.IO)` that was never cancelled. The fix uses the "ownership flag on `internal` constructor" pattern: `fromContext` factories build the scope and pass `ownsScope = true`; tests use the public 2-arg constructor and retain ownership (existing `TestScope` cancellation in `@After` is untouched). `SettingsRepository` interface gains `close()`. Koin DI wires `single { ... }.withOptions { onClose { it?.close() } }` (Koin 4.x DSL — `withOptions { onClose }`, not the older `onClose` directly on the definition). `SimonApp.onTerminate()` calls `GlobalContext.getOrNull()?.close()` for emulator/instrumented-test cleanup; on real devices process death handles it.

## Recommended next batch
Pick one — all isolated:

1. **#17 SimonButtonGrid extract** (M). `SimonGameScreen` is ~750 lines. Pure composable extraction; low risk. Unblocks future feature work that touches button rendering.
2. **#32 Progressive onboarding / first-run tutorial** (M). Useful pre-launch but needs a UX decision (overlay tour vs guided first level vs both).
3. **#35 Color-blind mode** (M). Pattern/icon overlay on buttons. Needs design call: numeric labels vs distinct shapes vs both.
4. **F8 Local achievements / badges** (M). Pure DataStore key-set; no backend. Pairs naturally with the already-shipped statistics screen.
5. **Phase 4 Play Store prep wrap-up**. Keystore + Play Console submission are user-action only — confirm whether launch is the active goal before pulling more polish.

## Verification flow
Standard:
1. `./gradlew test lint`
2. `./gradlew installDebug`
3. `adb shell am force-stop com.happypuppy.memorylights && adb shell am start -n com.happypuppy.memorylights/.ui.MainActivity`
4. `android screen capture -o /tmp/ml_X.png` + `android layout` for state assertions
5. Spot-check on the running Pixel_8_API_34 emulator

## User preferences (do not re-derive)
- Forward motion preferred — skip brainstorming skill on routine "keep going" prompts.
- Multi-paragraph commit bodies explaining "why," not "what."
- Push at session end (background-agent sessions: local commits only, no push).
- Use the existing emulator rather than starting a new AVD.
- Surface scope cuts during work, not after the fact.
- Use `android docs` proactively for AndroidX/Compose API lookups.

## Open audit (P2)
#17 SimonButtonGrid extract (M), #20 partial / depends on #17, #32 onboarding (M), #34 reduce-motion (previously cut), #35 color-blind (M).

## Open features
F8 achievements (M), F11 GPGS (L, post-launch v1.1), F13 per-button color (L), F17 IAP (XL, decision-gated), Light theme variant of F12 (deferred — needs game-screen `Color.White` audit first).

## Resume prompt
Paste in next session:

```
Read specs/RESUME.md and specs/improvements.md. Confirm git is at the Phase 10 head on main with clean tree. Then pick the next batch from RESUME's recommendation list — default to #17 SimonButtonGrid extract unless context has changed. Stop after the work lands + tests + verifies on emulator + commits. If anything blocks, prefer to roll back rather than power through.
```
