# Sound Pack Licensing

All Memory Lights sound packs are synthesized in-house via deterministic Python
generators. There are no third-party samples, so there are no upstream license
terms to comply with.

## Synthesized packs

Generated from the Python scripts under `tools/sounds/{standard,electronic,retro,funny,musical,scifi,nature}/`.
Output WAVs live in `app/src/main/res/raw/`. Re-running the scripts is
deterministic — same inputs yield identical bytes.

| Pack         | Generator                              | Method                                                                      |
|--------------|----------------------------------------|-----------------------------------------------------------------------------|
| Standard     | `tools/sounds/standard/generate.py`    | warm classic 1978 square waves (209/252/310/415/494/622 Hz) + buzzer        |
| Electronic   | `tools/sounds/electronic/generate.py`  | FM pluck, detuned supersaw, FM bell, sub-bass pulse, arp blip, glitch error |
| Retro Gaming | `tools/sounds/retro/generate.py`       | 8-bit pulse duty waves, 4-bit NES triangle, arcade arps, LFSR rattle error  |
| Funny        | `tools/sounds/funny/generate.py`       | spring boing, duck honk, slide whistles, bubble bloop, sad trombone error   |
| Musical      | `tools/sounds/musical/generate.py`     | sine + 2 harmonics + ADSR; pentatonic piano-style tones                     |
| Sci-Fi       | `tools/sounds/scifi/generate.py`       | chiptune square / saw with frequency sweeps; klaxon error                   |
| Nature       | `tools/sounds/nature/generate.py`      | nature-flavored synthesis: chirp / drop / chime / ribbit / cricket          |

## Notes

- Nature is a synth approximation rather than field recordings. Real CC0
  samples (Pixabay / Freesound) require account-gated downloads and per-file
  license auditing, so we kept all packs license-clean and reproducible.
- All synthesized output is 44100 Hz mono 16-bit PCM, peak-normalized to −1 dBFS.
- `standard`, `electronic`, `retro`, and `funny` use Python standard library only;
  `musical`, `scifi`, and `nature` require `numpy` + `scipy`:
  ```
  python3 tools/sounds/standard/generate.py   app/src/main/res/raw
  python3 tools/sounds/electronic/generate.py app/src/main/res/raw
  python3 tools/sounds/retro/generate.py      app/src/main/res/raw
  python3 tools/sounds/funny/generate.py      app/src/main/res/raw
  python3 tools/sounds/musical/generate.py    app/src/main/res/raw
  python3 tools/sounds/scifi/generate.py      app/src/main/res/raw
  python3 tools/sounds/nature/generate.py     app/src/main/res/raw
  ```
