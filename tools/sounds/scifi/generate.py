#!/usr/bin/env python3
"""
Sci-Fi sound pack generator (chiptune / 8-bit retro style).

Each color gets a distinct synthesized FX:
    GREEN  = ascending square-wave bleep
    RED    = descending square-wave bleep
    YELLOW = modulated square wave (vibrato)
    BLUE   = filter-sweep saw (downward sweep)
    PURPLE = laser zap (fast downward sweep on triangle)
    ORANGE = warp / whoosh (filtered noise + rising pitch)
    ERROR  = klaxon / alarm (alternating two-tone square)

Output: 44100 Hz mono 16-bit PCM, ~280 ms per file.

Usage:
    /tmp/mlsynth/bin/python3 tools/sounds/scifi/generate.py <out_dir>
"""
import argparse
import os
import numpy as np
from scipy.io import wavfile
from scipy.signal import sawtooth, square

SAMPLE_RATE = 44100
DURATION_S = 0.28
PEAK_DBFS = -1.0

def adsr(n_samples, attack=0.005, decay=0.04, sustain_level=0.6, release=0.10):
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

def time_axis(duration_s):
    n = int(duration_s * SAMPLE_RATE)
    return np.arange(n) / SAMPLE_RATE, n

def freq_sweep_phase(f_start, f_end, t):
    """Linear frequency sweep — cumulative phase for sin/square/saw."""
    freqs = np.linspace(f_start, f_end, len(t))
    return 2 * np.pi * np.cumsum(freqs) / SAMPLE_RATE

# --- generators ---

def ascending_bleep():
    t, n = time_axis(DURATION_S)
    phase = freq_sweep_phase(440, 880, t)
    wave = square(phase)
    wave *= adsr(n)
    return wave

def descending_bleep():
    t, n = time_axis(DURATION_S)
    phase = freq_sweep_phase(880, 440, t)
    wave = square(phase)
    wave *= adsr(n)
    return wave

def modulated_square():
    t, n = time_axis(DURATION_S)
    # Vibrato: 12 Hz LFO modulating frequency around 660 Hz.
    base = 660
    vib_depth = 60
    inst_freq = base + vib_depth * np.sin(2 * np.pi * 12 * t)
    phase = 2 * np.pi * np.cumsum(inst_freq) / SAMPLE_RATE
    wave = square(phase, duty=0.5)
    wave *= adsr(n)
    return wave

def filter_sweep_saw():
    t, n = time_axis(DURATION_S)
    phase = freq_sweep_phase(620, 200, t)
    wave = sawtooth(phase)
    # Cheap "filter sweep" approximation: amplitude tilt + harmonic add.
    tilt = np.linspace(1.0, 0.6, n)
    wave = wave * tilt
    wave *= adsr(n, attack=0.005, decay=0.02, sustain_level=0.7, release=0.12)
    return wave

def laser_zap():
    """Fast, very steep downward sweep — classic 'pew'."""
    duration = 0.22
    t, n = time_axis(duration)
    phase = freq_sweep_phase(1800, 200, t)
    # Triangle = abs(saw) shifted, but build from sin of phase to keep it smooth.
    wave = np.sign(np.sin(phase)) * np.abs(np.sin(phase * 0.5))
    wave *= adsr(n, attack=0.002, decay=0.02, sustain_level=0.5, release=0.08)
    return wave

def warp_whoosh():
    t, n = time_axis(DURATION_S)
    rng = np.random.default_rng(seed=42)
    noise = rng.uniform(-1.0, 1.0, n)
    # Add a rising-pitch tone underneath.
    phase = freq_sweep_phase(120, 480, t)
    tone = 0.4 * np.sin(phase)
    # Cheap low-pass-ish: cumulative-sum smoothing kernel.
    kernel = 24
    smoothed = np.convolve(noise, np.ones(kernel)/kernel, mode="same")
    wave = smoothed + tone
    # Loudness ramp up then down for whoosh.
    ramp = np.concatenate([
        np.linspace(0.0, 1.0, n // 2),
        np.linspace(1.0, 0.2, n - n // 2)
    ])
    wave = wave * ramp
    return wave

def klaxon_alarm():
    """Alternating two-tone square — short alarm pulse."""
    duration = 0.32
    t, n = time_axis(duration)
    # Switch frequency every 60 ms.
    period_samples = int(0.06 * SAMPLE_RATE)
    phase = np.zeros(n, dtype=np.float64)
    f1, f2 = 520, 700
    cur_phase = 0.0
    for i in range(n):
        f = f1 if (i // period_samples) % 2 == 0 else f2
        cur_phase += 2 * np.pi * f / SAMPLE_RATE
        phase[i] = cur_phase
    wave = square(phase)
    wave *= adsr(n, attack=0.005, decay=0.0, sustain_level=0.85, release=0.05)
    return wave

GENERATORS = {
    "green":  ascending_bleep,
    "red":    descending_bleep,
    "yellow": modulated_square,
    "blue":   filter_sweep_saw,
    "purple": laser_zap,
    "orange": warp_whoosh,
}

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
    print(f"Generating Sci-Fi pack into {args.out_dir}")
    for color, gen in GENERATORS.items():
        write_wav(os.path.join(args.out_dir, f"scifi_{color}_tone.wav"), gen())
    write_wav(os.path.join(args.out_dir, "scifi_error_tone.wav"), klaxon_alarm())

if __name__ == "__main__":
    main()
