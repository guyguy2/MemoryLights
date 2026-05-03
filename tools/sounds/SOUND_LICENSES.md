# Sound Pack Licensing

All Memory Lights sound packs are either pre-existing in-house assets or
synthesized in-house. There are no third-party samples, so there are no
upstream license terms to comply with.

## In-house assets (already in repository)

| Pack       | Source                          |
|------------|----------------------------------|
| Standard   | Original recordings              |
| Funny      | Original recordings              |
| Electronic | Original recordings              |
| Retro      | Original recordings              |

## Synthesized packs (added 2026-05-03, this commit)

Generated from the Python scripts under `tools/sounds/{musical,scifi,nature}/`.
Output WAVs live in `app/src/main/res/raw/`. Re-running the scripts is
deterministic — same inputs yield identical bytes.

| Pack    | Generator                        | Method                                                              |
|---------|----------------------------------|---------------------------------------------------------------------|
| Musical | `tools/sounds/musical/generate.py` | sine + 2 harmonics + ADSR; pentatonic piano-style tones            |
| Sci-Fi  | `tools/sounds/scifi/generate.py`   | chiptune square / saw with frequency sweeps; klaxon error          |
| Nature  | `tools/sounds/nature/generate.py`  | nature-flavored synthesis: chirp / drop / chime / ribbit / cricket  |

## Notes

- Nature is a synth approximation rather than field recordings. Real CC0
  samples (Pixabay / Freesound) require account-gated downloads and per-file
  license auditing, so we kept the pack license-clean and reproducible.
- All output is 44100 Hz mono 16-bit PCM, peak-normalized to −1 dBFS,
  matching the existing `standard_*` pack format.
- Regenerate with the venv at `/tmp/mlsynth` or any Python with `numpy` +
  `scipy` installed:
  ```
  python3 tools/sounds/musical/generate.py app/src/main/res/raw
  python3 tools/sounds/scifi/generate.py   app/src/main/res/raw
  python3 tools/sounds/nature/generate.py  app/src/main/res/raw
  ```
