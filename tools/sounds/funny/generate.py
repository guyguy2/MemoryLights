#!/usr/bin/env python3
"""
Funny sound pack generator for Memory Lights.

Playful cartoon & comedic SFX synthesized in pure Python stdlib so each button
has a distinct, lighthearted character while remaining fast enough for sequences.

Mapping:
    GREEN  = Cartoon spring "boing" (wobbly rising vibrato)
    RED    = Rubber ducky / clown "honk" (nasal double-quack formant cluster)
    YELLOW = Slide whistle UP (smooth upward glide 420 -> 1680 Hz)
    BLUE   = Cartoon bubble "bloop" (rapid upward pop + harmonic ping)
    PURPLE = Slide whistle DOWN (playful downward glide 1450 -> 360 Hz)
    ORANGE = Clown bike horn squeeze (dual-burst brassy reed chord)
    ERROR  = Sad cartoon trombone "wah-wah-waaah" (520 ms)

Output: 44100 Hz mono 16-bit PCM, peak-normalized to -1.0 dBFS.
        280-360 ms per button tone, 520 ms for error tone.
        Zero external dependencies (Python standard library only).

Usage:
    python3 tools/sounds/funny/generate.py app/src/main/res/raw
"""

import argparse
import math
import os
import struct
import wave

SAMPLE_RATE = 44100
PEAK_DBFS = -1.0


def smooth_envelope(idx: int, total: int, attack_s: float = 0.005, release_s: float = 0.035) -> float:
    a = max(1, int(attack_s * SAMPLE_RATE))
    r = max(1, int(release_s * SAMPLE_RATE))
    if idx < a:
        return 0.5 * (1.0 - math.cos(math.pi * idx / a))
    rem = total - 1 - idx
    if rem < r:
        return 0.5 * (1.0 - math.cos(math.pi * rem / r))
    return 1.0


def synth_green_boing(duration_s: float = 0.34) -> list[float]:
    n = int(duration_s * SAMPLE_RATE)
    two_pi = 2.0 * math.pi
    phase = 0.0
    samples: list[float] = []
    for i in range(n):
        t = i / SAMPLE_RATE
        # Spring pitch rises then wobbles strongly at 22 Hz
        base_f = 280.0 + 240.0 * min(1.0, t / 0.12)
        wobble = 65.0 * math.exp(-t * 4.5) * math.sin(two_pi * 22.0 * t)
        phase += two_pi * (base_f + wobble) / SAMPLE_RATE
        val = math.sin(phase) + 0.30 * math.sin(2.0 * phase)
        env = smooth_envelope(i, n, 0.004, 0.06) * (1.0 - 0.35 * (t / duration_s))
        samples.append(val * env)
    return samples


def synth_red_honk(duration_s: float = 0.30) -> list[float]:
    n = int(duration_s * SAMPLE_RATE)
    two_pi = 2.0 * math.pi
    burst1_end = int(0.11 * SAMPLE_RATE)
    burst2_start = int(0.14 * SAMPLE_RATE)
    samples: list[float] = []
    for i in range(n):
        t = i / SAMPLE_RATE
        # Nasal formant-like reed buzz around 290 Hz
        f0 = 290.0 - 20.0 * (t / duration_s)
        reed = (
            0.35 * math.sin(two_pi * f0 * t)
            + 0.70 * math.sin(two_pi * (2.0 * f0) * t)
            + 0.85 * math.sin(two_pi * (3.0 * f0) * t)
            + 0.45 * math.sin(two_pi * (4.0 * f0) * t)
        )
        if i < burst1_end:
            gate = smooth_envelope(i, burst1_end, 0.005, 0.018)
        elif i >= burst2_start:
            gate = smooth_envelope(i - burst2_start, n - burst2_start, 0.005, 0.03)
        else:
            gate = 0.0
        samples.append(reed * gate)
    return samples


def synth_yellow_whistle_up(duration_s: float = 0.32) -> list[float]:
    n = int(duration_s * SAMPLE_RATE)
    two_pi = 2.0 * math.pi
    phase = 0.0
    samples: list[float] = []
    for i in range(n):
        frac = i / max(1, n - 1)
        f = 420.0 * ((1680.0 / 420.0) ** (frac ** 0.85))
        phase += two_pi * f / SAMPLE_RATE
        val = math.sin(phase) + 0.12 * math.sin(2.0 * phase)
        env = smooth_envelope(i, n, 0.008, 0.04)
        samples.append(val * env)
    return samples


def synth_blue_bloop(duration_s: float = 0.28) -> list[float]:
    n = int(duration_s * SAMPLE_RATE)
    two_pi = 2.0 * math.pi
    phase = 0.0
    samples: list[float] = []
    for i in range(n):
        t = i / SAMPLE_RATE
        # Bubble pop: quick exponential upward chirp at start, short resonant tail
        if t < 0.095:
            f = 220.0 * ((760.0 / 220.0) ** (t / 0.095))
            amp = smooth_envelope(i, int(0.095 * SAMPLE_RATE), 0.003, 0.025)
        else:
            f = 760.0
            amp = 0.35 * math.exp(-(t - 0.095) * 24.0)
        phase += two_pi * f / SAMPLE_RATE
        samples.append(math.sin(phase) * amp)
    return samples


def synth_purple_whistle_down(duration_s: float = 0.32) -> list[float]:
    n = int(duration_s * SAMPLE_RATE)
    two_pi = 2.0 * math.pi
    phase = 0.0
    samples: list[float] = []
    for i in range(n):
        t = i / SAMPLE_RATE
        frac = i / max(1, n - 1)
        f = 1450.0 * ((360.0 / 1450.0) ** frac) * (1.0 + 0.02 * math.sin(two_pi * 16.0 * t))
        phase += two_pi * f / SAMPLE_RATE
        val = math.sin(phase) + 0.14 * math.sin(2.0 * phase)
        env = smooth_envelope(i, n, 0.008, 0.04)
        samples.append(val * env)
    return samples


def synth_orange_clown_horn(duration_s: float = 0.32) -> list[float]:
    n = int(duration_s * SAMPLE_RATE)
    two_pi = 2.0 * math.pi
    # Brassy major-ish honk chord (G4 + B4 + D5)
    freqs = (392.00, 493.88, 587.33)
    samples: list[float] = []
    for i in range(n):
        t = i / SAMPLE_RATE
        val = 0.0
        for f in freqs:
            val += math.sin(two_pi * f * t) + 0.30 * math.sin(two_pi * 2.0 * f * t)
        env = smooth_envelope(i, n, 0.010, 0.045)
        samples.append(val * env)
    return samples


def synth_error_sad_trombone(duration_s: float = 0.52) -> list[float]:
    """Stepped descending cartoon trombone: Bb3 -> A3 -> Ab3 -> G3 slide."""
    n = int(duration_s * SAMPLE_RATE)
    two_pi = 2.0 * math.pi
    seg_boundaries = (0, int(0.11 * SAMPLE_RATE), int(0.22 * SAMPLE_RATE), int(0.33 * SAMPLE_RATE), n)
    seg_freqs = (233.08, 220.00, 207.65, 196.00)
    phase = 0.0
    samples: list[float] = []
    for i in range(n):
        t = i / SAMPLE_RATE
        seg = 3
        for s_idx in range(4):
            if seg_boundaries[s_idx] <= i < seg_boundaries[s_idx + 1]:
                seg = s_idx
                break
        f = seg_freqs[seg]
        if seg == 3:
            # Final "waaah" droops down with comedic vibrato
            local_frac = (i - seg_boundaries[3]) / max(1, n - seg_boundaries[3])
            f = f * (1.0 - 0.14 * local_frac) * (1.0 + 0.025 * math.sin(two_pi * 11.0 * t))
        phase += two_pi * f / SAMPLE_RATE
        # Brassy harmonic stack
        brass = (
            1.00 * math.sin(phase)
            + 0.60 * math.sin(2.0 * phase)
            + 0.38 * math.sin(3.0 * phase)
            + 0.20 * math.sin(4.0 * phase)
        )
        local_total = seg_boundaries[seg + 1] - seg_boundaries[seg]
        local_idx = i - seg_boundaries[seg]
        env = smooth_envelope(local_idx, local_total, 0.006, 0.025 if seg < 3 else 0.055)
        samples.append(brass * env)
    return samples


GENERATORS = {
    "green": synth_green_boing,
    "red": synth_red_honk,
    "yellow": synth_yellow_whistle_up,
    "blue": synth_blue_bloop,
    "purple": synth_purple_whistle_down,
    "orange": synth_orange_clown_horn,
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
    parser = argparse.ArgumentParser(description="Generate Funny sound pack WAVs.")
    parser.add_argument("out_dir", help="Output directory (e.g. app/src/main/res/raw)")
    args = parser.parse_args()
    os.makedirs(args.out_dir, exist_ok=True)
    print(f"Generating Funny pack into {args.out_dir}")
    for color, gen in GENERATORS.items():
        write_wav(os.path.join(args.out_dir, f"funny_{color}_tone.wav"), gen())
    write_wav(os.path.join(args.out_dir, "funny_error_tone.wav"), synth_error_sad_trombone())


if __name__ == "__main__":
    main()
