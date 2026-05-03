# Memory Lights — Play Store Listing

Drafts for fields you'll paste into the Play Console. Tweak before submission.

---

## App title (max 30 chars)

```
Memory Lights
```

## Short description (max 80 chars)

```
A memory game with seven sound packs, share-able scores, and reverse mode.
```

Alt phrasings if you want a different angle:
- `Watch the lights, repeat the pattern. Seven sound packs and reverse mode.`
- `A modern Simon. Tap colors in order, level up, share your best.`

---

## Full description (max 4000 chars)

```
Memory Lights is a polished take on the classic memory game. Watch the lights flash a pattern, then repeat it. Each round adds one more step. How far can you go?

GAME MODES
- Standard: 4 colors, classic gameplay
- Memory Lights+: 6 colors for a tougher challenge
- Reverse Mode: watch the sequence forward, repeat it backward
- Practice Mode: wrong taps replay the sequence so you can drill tough patterns
- Difficulty: speeds up as you climb

SEVEN SOUND PACKS
- Standard: classic Simon tones
- Funny: humorous sound effects
- Electronic: modern synth tones
- Retro Gaming: 8-bit chiptune
- Musical: pentatonic piano
- Nature: birds, water, chimes, and rumble
- Sci-Fi: chiptune bleeps and zaps

POLISH AND ACCESSIBILITY
- Adjustable player timeout (5, 10, 15, or 30 seconds)
- Visible countdown ring around the center disc
- Pause button mid-run
- Replay last sequence after game over
- Share a 1080x1080 score card to any messaging app
- Persistent high score and full game statistics
- Distinct screen-reader announcements for level, your turn, and game over
- All-black AMOLED-friendly theme
- 48dp minimum touch targets and content descriptions throughout
- Vibration toggle and master mute button

OFFLINE FIRST
No accounts. No ads. No tracking. No network calls. Your scores live on your device.

If you grew up on the original handheld memory toys, Memory Lights is built for the same itch — quick to play, easy to come back to, and just hard enough to keep you trying one more time.
```

Character count: ~1500 (well under 4000 limit).

---

## What's new (release notes for v1.0)

```
First release of Memory Lights.
- Standard 4-button and Memory Lights+ 6-button modes
- Reverse Mode and Practice Mode
- Seven sound packs
- Adjustable player timeout
- Pause / replay / share score card
- Statistics screen with high score, average, total, and best streak
```

---

## Categorization

- **Category:** Games > Puzzle (alt: Games > Casual or Games > Brain)
- **Tags:** memory, simon, brain training, puzzle, casual, single-player, offline

## Content rating

- Pegi 3 / ESRB Everyone — no violence, no user-generated content, no data collection
- Run the IARC questionnaire in Play Console; expect "Everyone" rating

## Pricing

- Free
- No in-app purchases (v1.0)
- No ads

## Target audience and content

- **Target age range:** 6+ (puzzle / brain training)
- **Appeals to children:** mark as "no" to avoid Designed for Families compliance unless you want it; if you do want DFF, the app already qualifies (no ads, no data collection)

---

## App content declarations (Play Console)

- **Privacy policy URL:** required — see `playstore/privacy_policy.md` for the text. You need to host it (GitHub Pages on this repo is the easiest free option). Suggested URL: `https://guyguy2.github.io/MemoryLights/privacy.html`
- **Data safety form:**
  - Data collected: NONE (no analytics, no crash reporting, no network)
  - Data shared: NONE
  - Security practices: data encrypted in transit (N/A — app makes zero network calls)
  - Permissions: VIBRATE only
- **Ads:** No ads
- **Government app:** No
- **News app:** No
- **COVID-19 contact tracing:** No

---

## Required image assets (still TODO)

| Asset | Spec | Status |
|---|---|---|
| App icon | 512x512 PNG, 32-bit, no alpha | Pull from `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png` and upscale, or commission a fresh one |
| Feature graphic | 1024x500 PNG/JPG | Not done — see `feature_graphic.md` |
| Phone screenshots | min 2, max 8, 16:9 to 9:16, min 320px | 7 screenshots in `screenshots/` |
| 7-inch tablet screenshots | optional | not done |
| 10-inch tablet screenshots | optional | not done |
| Promo video (YouTube URL) | optional | not done |

The 7 phone screenshots in `screenshots/` are 1080x2400 (Pixel 8) — well within Play Store specs.
