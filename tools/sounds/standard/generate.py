#!/usr/bin/env python3
"""
Standard (default) sound pack generator for Memory Lights.

Warm classic 1978 Milton Bradley Simon square-wave tones synthesized using
band-limited odd harmonics (Fourier series with gentle high-harmonic roll-off)
so each beep sounds unmistakably like the classic electronic game without
piercing aliasing on phone speakers.

Mapping (1978 Milton Bradley Simon hardware frequencies + octave extensions
for 6-button Memory Lights+ mode):
    BLUE   = 209.0 Hz (~G#3)
    YELLOW = 252.0 Hz (~B3)
    RED    = 310.0 Hz (~D#4)
    GREEN  = 415.0 Hz (~G#4)
    PURPLE = 494.0 Hz (B4  — octave above Yellow)
    ORANGE = 622.0 Hz (D#5 — octave above Red)
    ERROR  = Low dissonant dual-frequency razz buzzer (110 Hz + 117.5 Hz
             with 55 Hz sub-octave & warm harmonics; audible on phone speakers)

Output: 44100 Hz mono 16-bit PCM, peak-normalized to -1.0 dBFS.
        300 ms per button tone, 550 ms for error buzzer.
        Zero external dependencies (Python standard library only).

Usage:
    python3 tools/sounds/standard/generate.py app/src/main/res/raw
"""

import argparse
import math
import os
import struct
import wave

SAMPLE_RATE = 44100
BUTTON_DURATION_S = 0.30
ERROR_DURATION_S = 0.55
PEAK_DBFS = -1.0

# Classic 1978 Simon hardware pitches (4-button) + consonant octave extensions (6-button).
NOTES_HZ = {
    "blue": 209.0,
    "yellow": 252.0,
    "red": 310.0,
    "green": 415.0,
    "purple": 494.0,
    "orange": 622.0,
}

# Odd harmonics of a square wave (1/k) softened with a warm roll-off filter.
WARM_SQUARE_HARMONICS = (
    (1, 1.00),
    (3, 0.24),
    (5, 0.10),
    (7, 0.045),
    (9, 0.020),
)


def smooth_envelope(idx: int, total: int, attack_s: float, release_s: float) -> float:
    """Click-free raised-cosine attack and release envelope."""
    attack_n = max(1, int(attack_s * SAMPLE_RATE))
    release_n = max(1, int(release_s * SAMPLE_RATE))
    if idx < attack_n:
        return 0.5 * (1.0 - math.cos(math.pi * idx / attack_n))
    rem = total - 1 - idx
    if rem < release_n:
        return 0.5 * (1.0 - math.cos(math.pi * rem / release_n))
    return 1.0


def synthesize_button_tone(freq_hz: float, duration_s: float = BUTTON_DURATION_S) -> list[float]:
    """Generate a warm classic Simon square-wave beep at freq_hz."""
    n = int(duration_s * SAMPLE_RATE)
    two_pi = 2.0 * math.pi
    samples: list[float] = []
    for i in range(n):
        t = i / SAMPLE_RATE
        # Slight natural decay over the sustain portion keeps short beeps punchy.
        decay = 1.0 - 0.12 * (t / duration_s)
        env = smooth_envelope(i, n, attack_s=0.005, release_s=0.035) * decay
        val = 0.0
        for harmonic, weight in WARM_SQUARE_HARMONICS:
            val += weight * math.sin(two_pi * freq_hz * harmonic * t)
        samples.append(val * env)
    return samples


def synthesize_error_tone(duration_s: float = ERROR_DURATION_S) -> list[float]:
    """Generate a low dissonant classic Simon 'razz' buzzer audible on phone speakers."""
    n = int(duration_s * SAMPLE_RATE)
    two_pi = 2.0 * math.pi
    f_sub = 55.0
    f1 = 110.0
    f2 = 117.5  # ~7.5 Hz beating creates the classic raspy buzzer rattle
    buzz_harmonics = (
        (1, 1.00),
        (2, 0.35),
        (3, 0.42),
        (4, 0.16),
        (5, 0.20),
        (7, 0.09),
    )
    samples: list[float] = []
    for i in range(n):
        t = i / SAMPLE_RATE
        env = smooth_envelope(i, n, attack_s=0.006, release_s=0.045)
        val = 0.30 * math.sin(two_pi * f_sub * t)
        for harmonic, weight in buzz_harmonics:
            val += weight * 0.5 * (
                math.sin(two_pi * f1 * harmonic * t)
                + math.sin(two_pi * f2 * harmonic * t)
            )
        samples.append(val * env)
    return samples


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
    parser = argparse.ArgumentParser(description="Generate Standard Simon sound pack WAVs.")
    parser.add_argument("out_dir", help="Output directory (e.g. app/src/main/res/raw)")
    args = parser.parse_args()
    os.makedirs(args.out_dir, exist_ok=True)
    print(f"Generating Standard pack into {args.out_dir}")
    for color, freq in NOTES_HZ.items():
        write_wav(
            os.path.join(args.out_dir, f"standard_{color}_tone.wav"),
            synthesize_button_tone(freq),
        )
    write_wav(
        os.path.join(args.out_dir, "standard_error_tone.wav"),
        synthesize_error_tone(),
    )


if __name__ == "__main__":
    main()
