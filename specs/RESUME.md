# Resume note for next session (written 2026-07-10, Wave 11 — BlitzHud lift)

Canonical roadmap: `specs/improvements.md`. This file is a tactical pointer; if it disagrees with `specs/improvements.md`, trust the spec.

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
Read specs/RESUME.md and specs/improvements.md. Confirm git is at the Wave 11 BlitzHud head on main with clean tree. 95 unit tests green. Game-screen private HUDs exhausted. Next: #32 onboarding (UX-blocked), Phase 4 Play Store (user-action only), or F13/F12 product work. Ask the user which path to take before starting.
```
