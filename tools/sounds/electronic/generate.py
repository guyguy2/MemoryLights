#!/usr/bin/env python3
"""
Electronic sound pack generator for Memory Lights.

Modern synthesizer tones (FM plucks, detuned supersaw stabs, FM bell,
resonant sub-bass, chord arpeggio, and warm synth fifths) synthesized in pure
Python stdlib so every color has an unmistakable electronic timbre.

Mapping:
    GREEN  = Bright FM synth pluck (C5, 523.25 Hz)
    RED    = Detuned supersaw synth stab (G4/G5, 392.00 Hz)
    YELLOW = Sparkling FM chime bell (A5, 880.00 Hz, 3:1 modulator)
    BLUE   = Resonant sub-bass + octave pulse (E3 -> E2 sweep, 164.81 Hz)
    PURPLE = 3-step synth arpeggio blip (D5 -> F#5 -> A5)
    ORANGE = Warm synth fifth swell (F4 349.23 Hz + C5 523.25 Hz)
    ERROR  = Crisp electronic glitch power-down (descending detuned saw + crunch)

Output: 44100 Hz mono 16-bit PCM, peak-normalized to -1.0 dBFS.
        300-340 ms per button tone, 500 ms for error tone.
        Zero external dependencies (Python standard library only).

Usage:
    python3 tools/sounds/electronic/generate.py app/src/main/res/raw
"""

import argparse
import math
import os
import struct
import wave

SAMPLE_RATE = 44100
PEAK_DBFS = -1.0


def adsr(idx: int, total: int, attack_s: float, decay_s: float, sustain: float, release_s: float) -> float:
    a = max(1, int(attack_s * SAMPLE_RATE))
    d = max(1, int(decay_s * SAMPLE_RATE))
    r = max(1, int(release_s * SAMPLE_RATE))
    s_end = max(a + d, total - r)
    if idx < a:
        return idx / a
    if idx < a + d:
        frac = (idx - a) / d
        return 1.0 - (1.0 - sustain) * frac
    if idx < s_end:
        return sustain
    rem = max(0, total - 1 - idx)
    return sustain * (rem / max(1, r))


def bandlimited_saw(phase: float, harmonics: int = 8) -> float:
    val = 0.0
    for k in range(1, harmonics + 1):
        val += ((-1.0) ** (k + 1)) * math.sin(k * phase) / k
    return val * (2.0 / math.pi)


def synth_green_fm_pluck(duration_s: float = 0.30) -> list[float]:
    n = int(duration_s * SAMPLE_RATE)
    two_pi = 2.0 * math.pi
    f_c = 523.25  # C5
    f_m = f_c * 2.0
    samples: list[float] = []
    for i in range(n):
        t = i / SAMPLE_RATE
        mod_index = 2.8 * math.exp(-t * 14.0)
        mod = mod_index * math.sin(two_pi * f_m * t)
        carrier = math.sin(two_pi * f_c * t + mod) + 0.35 * math.sin(two_pi * (f_c * 0.5) * t)
        env = adsr(i, n, 0.004, 0.09, 0.40, 0.08)
        samples.append(carrier * env)
    return samples


def synth_red_supersaw_stab(duration_s: float = 0.30) -> list[float]:
    n = int(duration_s * SAMPLE_RATE)
    two_pi = 2.0 * math.pi
    freqs = (392.00, 394.6, 389.4, 784.00)  # G4 detuned triad + G5
    weights = (0.40, 0.30, 0.30, 0.25)
    phases = [0.0] * len(freqs)
    samples: list[float] = []
    for i in range(n):
        t = i / SAMPLE_RATE
        # Simulated low-pass brightness decay: fewer harmonics over time
        bright = 1.0 - 0.55 * (t / duration_s)
        val = 0.0
        for idx, (f, w) in enumerate(zip(freqs, weights)):
            phases[idx] += two_pi * f / SAMPLE_RATE
            val += w * (math.sin(phases[idx]) + bright * 0.45 * bandlimited_saw(phases[idx], 6))
        env = adsr(i, n, 0.005, 0.07, 0.55, 0.08)
        samples.append(val * env)
    return samples


def synth_yellow_fm_bell(duration_s: float = 0.34) -> list[float]:
    n = int(duration_s * SAMPLE_RATE)
    two_pi = 2.0 * math.pi
    f_c = 880.00  # A5
    f_m = f_c * 3.0  # Glassy chime ratio
    samples: list[float] = []
    for i in range(n):
        t = i / SAMPLE_RATE
        mod_index = 1.9 * math.exp(-t * 10.0)
        val = math.sin(two_pi * f_c * t + mod_index * math.sin(two_pi * f_m * t))
        val += 0.25 * math.sin(two_pi * (f_c * 2.0) * t) * math.exp(-t * 12.0)
        env = adsr(i, n, 0.003, 0.12, 0.30, 0.14)
        samples.append(val * env)
    return samples


def synth_blue_sub_bass(duration_s: float = 0.32) -> list[float]:
    n = int(duration_s * SAMPLE_RATE)
    two_pi = 2.0 * math.pi
    phase = 0.0
    samples: list[float] = []
    for i in range(n):
        t = i / SAMPLE_RATE
        # Pitch punch at attack (330 Hz -> 164.81 Hz E3), audible on phone speakers
        inst_f = 164.81 + 165.0 * math.exp(-t * 28.0)
        phase += two_pi * inst_f / SAMPLE_RATE
        val = (
            0.75 * math.sin(phase)
            + 0.45 * math.sin(2.0 * phase)
            + 0.22 * math.sin(3.0 * phase) * math.exp(-t * 8.0)
        )
        env = adsr(i, n, 0.004, 0.08, 0.65, 0.08)
        samples.append(val * env)
    return samples


def synth_purple_arp_blip(duration_s: float = 0.30) -> list[float]:
    n = int(duration_s * SAMPLE_RATE)
    two_pi = 2.0 * math.pi
    steps = (587.33, 739.99, 880.00)  # D5 -> F#5 -> A5 major arp
    step_len = max(1, n // len(steps))
    phase = 0.0
    samples: list[float] = []
    for i in range(n):
        step_idx = min(len(steps) - 1, i // step_len)
        local_i = i - step_idx * step_len
        f = steps[step_idx]
        phase += two_pi * f / SAMPLE_RATE
        local_env = adsr(local_i, step_len, 0.003, 0.03, 0.60, 0.02)
        val = 0.7 * math.sin(phase) + 0.35 * bandlimited_saw(phase, 5)
        samples.append(val * local_env)
    return samples


def synth_orange_fifth_swell(duration_s: float = 0.32) -> list[float]:
    n = int(duration_s * SAMPLE_RATE)
    two_pi = 2.0 * math.pi
    f1 = 349.23  # F4
    f2 = 523.25  # C5
    samples: list[float] = []
    for i in range(n):
        t = i / SAMPLE_RATE
        vib = 1.0 + 0.006 * math.sin(two_pi * 6.5 * t)
        val = (
            0.6 * math.sin(two_pi * f1 * vib * t)
            + 0.5 * math.sin(two_pi * f2 * vib * t)
            + 0.2 * math.sin(two_pi * (2.0 * f1) * t)
        )
        env = adsr(i, n, 0.025, 0.08, 0.70, 0.09)
        samples.append(val * env)
    return samples


def synth_error_glitch(duration_s: float = 0.50) -> list[float]:
    n = int(duration_s * SAMPLE_RATE)
    two_pi = 2.0 * math.pi
    phase1 = 0.0
    phase2 = 0.0
    samples: list[float] = []
    for i in range(n):
        t = i / SAMPLE_RATE
        frac = t / duration_s
        f1 = 240.0 * ((75.0 / 240.0) ** frac)
        f2 = f1 * 1.09  # Dissonant detuned ratio
        phase1 += two_pi * f1 / SAMPLE_RATE
        phase2 += two_pi * f2 / SAMPLE_RATE
        ring = 0.65 + 0.35 * math.sin(two_pi * 38.0 * t)
        val = 0.5 * (bandlimited_saw(phase1, 7) + bandlimited_saw(phase2, 7)) * ring
        env = adsr(i, n, 0.004, 0.06, 0.80, 0.07)
        samples.append(val * env)
    return samples


GENERATORS = {
    "green": synth_green_fm_pluck,
    "red": synth_red_supersaw_stab,
    "yellow": synth_yellow_fm_bell,
    "blue": synth_blue_sub_bass,
    "purple": synth_purple_arp_blip,
    "orange": synth_orange_fifth_swell,
}


def normalize_peak(samples: list[float], peak_dbfs: float = PEAK_DBFS) -> list[float]:
    peak_target = 10.0 ** (peak_dbfs / 20.0)
    peak = max((abs(s) for s in samples), default=0.0)
    if peak < 1e-9:
        return samples
    scale = peak_target / peak
    return [s * scale for s in samples]


def write_wav(path: str, samples: list[float]) -> None:
    normalized = normalize_peak(samples)
    pcm = bytearray()
    for s in normalized:
        clamped = max(-1.0, min(1.0, s))
        pcm.extend(struct.pack("<h", int(round(clamped * 32767.0))))
    with wave.open(path, "wb") as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(SAMPLE_RATE)
        wf.writeframes(pcm)
    print(f"  wrote {os.path.basename(path)} ({len(samples) / SAMPLE_RATE * 1000:.0f} ms)")


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate Electronic sound pack WAVs.")
    parser.add_argument("out_dir", help="Output directory (e.g. app/src/main/res/raw)")
    args = parser.parse_args()
    os.makedirs(args.out_dir, exist_ok=True)
    print(f"Generating Electronic pack into {args.out_dir}")
    for color, gen in GENERATORS.items():
        write_wav(os.path.join(args.out_dir, f"electronic_{color}_tone.wav"), gen())
    write_wav(os.path.join(args.out_dir, "electronic_error_tone.wav"), synth_error_glitch())


if __name__ == "__main__":
    main()
