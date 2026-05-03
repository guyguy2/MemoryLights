#!/usr/bin/env python3
"""
Nature sound pack generator (synthesized approximations).

Real CC0 nature samples would have required account-gated downloads from
Freesound/Pixabay, so this pack is synthesized in-house — license-clean and
reproducible. Each color targets a recognizable nature texture rather than a
faithful field recording.

    GREEN  = bird chirp        — high vibrato sine, fast decay
    RED    = water drop        — descending sine + short reverb-ish tail
    YELLOW = wind chime ding   — sine + 2 harmonics, long decay
    BLUE   = frog ribbit       — low pulsed square (two-burst)
    PURPLE = cricket           — high pulsed sine bursts
    ORANGE = thunder rumble    — low-pass filtered noise with slow envelope
    ERROR  = hawk screech      — harsh detuned descending sweep with noise

Output: 44100 Hz mono 16-bit PCM, 280–500 ms per file.

Usage:
    /tmp/mlsynth/bin/python3 tools/sounds/nature/generate.py <out_dir>
"""
import argparse
import os
import numpy as np
from scipy.io import wavfile
from scipy.signal import square

SAMPLE_RATE = 44100
PEAK_DBFS = -1.0

def time_axis(duration_s):
    n = int(duration_s * SAMPLE_RATE)
    return np.arange(n) / SAMPLE_RATE, n

def adsr(n_samples, attack=0.005, decay=0.05, sustain_level=0.5, release=0.15):
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

def lowpass(x, cutoff_hz, order=2):
    """One-pole low-pass cascaded to approximate `order`-pole roll-off."""
    rc = 1.0 / (2 * np.pi * cutoff_hz)
    dt = 1.0 / SAMPLE_RATE
    alpha = dt / (rc + dt)
    y = x.copy()
    for _ in range(order):
        out = np.zeros_like(y)
        prev = 0.0
        for i in range(len(y)):
            prev = prev + alpha * (y[i] - prev)
            out[i] = prev
        y = out
    return y

def freq_sweep_phase(f_start, f_end, t):
    freqs = np.linspace(f_start, f_end, len(t))
    return 2 * np.pi * np.cumsum(freqs) / SAMPLE_RATE

# --- generators ---

def bird_chirp():
    """Two short rising chirps."""
    duration = 0.32
    t, n = time_axis(duration)
    chirp = np.zeros(n)
    # Two chirps separated by short gap.
    chirp_n = int(0.08 * SAMPLE_RATE)
    gap_n = int(0.06 * SAMPLE_RATE)
    for start in (0, chirp_n + gap_n):
        end = min(start + chirp_n, n)
        seg = end - start
        if seg <= 0:
            break
        t_seg = np.arange(seg) / SAMPLE_RATE
        phase = freq_sweep_phase(2400, 4200, t_seg)
        # Vibrato on top.
        vibrato = 1.0 + 0.05 * np.sin(2 * np.pi * 30 * t_seg)
        seg_wave = np.sin(phase * vibrato)
        env = adsr(seg, attack=0.005, decay=0.02, sustain_level=0.5, release=0.04)
        chirp[start:end] = seg_wave * env
    return chirp

def water_drop():
    """Descending sine with short tail (cheap reverb via decaying repeats)."""
    duration = 0.34
    t, n = time_axis(duration)
    # Main "plink" — fast downward sweep on sine.
    main_n = int(0.10 * SAMPLE_RATE)
    t_main = np.arange(main_n) / SAMPLE_RATE
    phase = freq_sweep_phase(1200, 700, t_main)
    main = np.sin(phase) * adsr(main_n, attack=0.002, decay=0.02,
                                sustain_level=0.6, release=0.06)
    out = np.zeros(n)
    out[:main_n] = main
    # Add a quieter delayed copy for tail.
    delay = int(0.08 * SAMPLE_RATE)
    if delay + main_n <= n:
        out[delay:delay+main_n] += 0.35 * main
    return out

def wind_chime():
    """Bell-like sine with 2 harmonics and slow decay."""
    duration = 0.50
    t, n = time_axis(duration)
    f = 1320  # E6
    fundamental = np.sin(2 * np.pi * f * t)
    second = 0.45 * np.sin(2 * np.pi * 2 * f * t)
    third = 0.20 * np.sin(2 * np.pi * 3 * f * t)
    wave = fundamental + second + third
    # Long exponential-ish decay.
    env = adsr(n, attack=0.004, decay=0.08, sustain_level=0.45, release=0.40)
    return wave * env

def frog_ribbit():
    """Two short low-frequency square pulses."""
    duration = 0.30
    t, n = time_axis(duration)
    out = np.zeros(n)
    pulse_n = int(0.05 * SAMPLE_RATE)
    gap_n = int(0.04 * SAMPLE_RATE)
    for k, start in enumerate((0, pulse_n + gap_n)):
        end = min(start + pulse_n, n)
        seg = end - start
        if seg <= 0:
            break
        t_seg = np.arange(seg) / SAMPLE_RATE
        # Frequency drops slightly across the pulse.
        phase = freq_sweep_phase(180, 130, t_seg)
        pulse = square(phase, duty=0.4)
        env = adsr(seg, attack=0.005, decay=0.01, sustain_level=0.7, release=0.03)
        out[start:end] = pulse * env * (0.9 if k == 0 else 0.7)
    return out

def cricket():
    """Fast high-pitched amplitude bursts."""
    duration = 0.32
    t, n = time_axis(duration)
    f = 4200
    tone = np.sin(2 * np.pi * f * t)
    # Burst envelope: 80 Hz pulses (12.5 ms each, 12.5 ms gap).
    burst_rate = 80
    burst = 0.5 * (1 + square(2 * np.pi * burst_rate * t, duty=0.4))
    wave = tone * burst
    # Overall ADSR so it doesn't pop.
    wave *= adsr(n, attack=0.01, decay=0.05, sustain_level=0.6, release=0.10)
    return wave

def thunder_rumble():
    """Low-pass filtered noise with slow attack/release."""
    duration = 0.45
    t, n = time_axis(duration)
    rng = np.random.default_rng(seed=7)
    noise = rng.uniform(-1.0, 1.0, n)
    rumble = lowpass(noise, cutoff_hz=180, order=3)
    # Heavily emphasize the low end.
    rumble *= 3.0
    env = adsr(n, attack=0.06, decay=0.08, sustain_level=0.55, release=0.25)
    return rumble * env

def hawk_screech():
    """Harsh detuned descending sweep with noise."""
    duration = 0.40
    t, n = time_axis(duration)
    phase_a = freq_sweep_phase(2200, 900, t)
    phase_b = freq_sweep_phase(2210, 905, t)  # Slight detune for harshness.
    tone = 0.5 * np.sin(phase_a) + 0.5 * np.sin(phase_b)
    rng = np.random.default_rng(seed=13)
    noise = 0.25 * rng.uniform(-1.0, 1.0, n)
    wave = tone + noise
    env = adsr(n, attack=0.005, decay=0.04, sustain_level=0.7, release=0.18)
    return wave * env

GENERATORS = {
    "green":  bird_chirp,
    "red":    water_drop,
    "yellow": wind_chime,
    "blue":   frog_ribbit,
    "purple": cricket,
    "orange": thunder_rumble,
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
    print(f"Generating Nature pack into {args.out_dir}")
    for color, gen in GENERATORS.items():
        write_wav(os.path.join(args.out_dir, f"nature_{color}_tone.wav"), gen())
    write_wav(os.path.join(args.out_dir, "nature_error_tone.wav"), hawk_screech())

if __name__ == "__main__":
    main()
