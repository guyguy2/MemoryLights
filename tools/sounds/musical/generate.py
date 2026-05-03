#!/usr/bin/env python3
"""
Musical sound pack generator.

Pentatonic piano-style tones using sine + 2 harmonics + ADSR envelope.
Designed to stay consonant in any random sequence.

Mapping:
    GREEN  = C5  (523.25 Hz)
    RED    = E5  (659.25 Hz)
    YELLOW = G5  (783.99 Hz)
    BLUE   = A5  (880.00 Hz)
    PURPLE = D5  (587.33 Hz)
    ORANGE = F#5 (739.99 Hz)
    ERROR  = C5 + Db5 minor-2nd cluster (dissonant)

Output: 44100 Hz mono 16-bit PCM, ~360 ms per file.

Usage:
    /tmp/mlsynth/bin/python3 tools/sounds/musical/generate.py <out_dir>
"""
import argparse
import os
import numpy as np
from scipy.io import wavfile

SAMPLE_RATE = 44100
DURATION_S = 0.36
PEAK_DBFS = -1.0

NOTES = {
    "green":  523.25,
    "red":    659.25,
    "yellow": 783.99,
    "blue":   880.00,
    "purple": 587.33,
    "orange": 739.99,
}

def adsr(n_samples, attack=0.01, decay=0.08, sustain_level=0.45, release=0.20):
    """Piano-like envelope: fast attack, exponential-ish decay, long release."""
    env = np.zeros(n_samples, dtype=np.float64)
    a = int(attack * SAMPLE_RATE)
    d = int(decay * SAMPLE_RATE)
    r = int(release * SAMPLE_RATE)
    s = max(0, n_samples - a - d - r)
    idx = 0
    if a > 0:
        env[idx:idx+a] = np.linspace(0.0, 1.0, a, endpoint=False)
        idx += a
    if d > 0:
        env[idx:idx+d] = np.linspace(1.0, sustain_level, d, endpoint=False)
        idx += d
    if s > 0:
        env[idx:idx+s] = sustain_level
        idx += s
    if r > 0 and idx < n_samples:
        rem = n_samples - idx
        env[idx:idx+rem] = np.linspace(sustain_level, 0.0, rem, endpoint=True)
    return env

def piano_tone(freq, duration_s=DURATION_S):
    n = int(duration_s * SAMPLE_RATE)
    t = np.arange(n) / SAMPLE_RATE
    # Fundamental + 2nd + 3rd harmonics with falling amplitudes (piano-like spectrum).
    fundamental = np.sin(2 * np.pi * freq * t)
    second = 0.4 * np.sin(2 * np.pi * 2 * freq * t)
    third = 0.18 * np.sin(2 * np.pi * 3 * freq * t)
    wave = fundamental + second + third
    wave *= adsr(n)
    return wave

def error_tone(duration_s=DURATION_S):
    """C5 + Db5 minor-2nd cluster — deliberately dissonant."""
    n = int(duration_s * SAMPLE_RATE)
    t = np.arange(n) / SAMPLE_RATE
    c5 = 523.25
    db5 = 554.37
    wave = (
        0.6 * np.sin(2 * np.pi * c5 * t) +
        0.6 * np.sin(2 * np.pi * db5 * t) +
        0.25 * np.sin(2 * np.pi * 2 * c5 * t)
    )
    # Sharper, more abrupt envelope to emphasize the error feel.
    wave *= adsr(n, attack=0.005, decay=0.05, sustain_level=0.55, release=0.22)
    return wave

def normalize_peak(wave, peak_dbfs=PEAK_DBFS):
    peak_target = 10 ** (peak_dbfs / 20.0)
    peak = np.max(np.abs(wave))
    if peak < 1e-9:
        return wave
    return wave * (peak_target / peak)

def to_int16(wave):
    return np.clip(wave * 32767.0, -32768, 32767).astype(np.int16)

def write_wav(path, wave):
    wave = normalize_peak(wave)
    wavfile.write(path, SAMPLE_RATE, to_int16(wave))
    print(f"  wrote {os.path.basename(path)} ({len(wave)/SAMPLE_RATE*1000:.0f} ms)")

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("out_dir")
    args = ap.parse_args()
    os.makedirs(args.out_dir, exist_ok=True)
    print(f"Generating Musical pack into {args.out_dir}")
    for color, freq in NOTES.items():
        write_wav(os.path.join(args.out_dir, f"musical_{color}_tone.wav"),
                  piano_tone(freq))
    write_wav(os.path.join(args.out_dir, "musical_error_tone.wav"),
              error_tone())

if __name__ == "__main__":
    main()
