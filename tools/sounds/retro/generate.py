#!/usr/bin/env python3
"""
Retro Gaming (8-bit arcade / NES) sound pack generator for Memory Lights.

Authentic 8-bit pulse waves (12.5% / 25% / 50% duty), quantized 4-bit NES
triangle waves, rapid arcade arpeggios, and LFSR-style pseudo-noise error buzz.

Mapping:
    GREEN  = 8-bit coin pickup (B5 -> E6 25% pulse)
    RED    = 8-bit jump upward sweep (220 -> 660 Hz 50% square)
    YELLOW = 8-bit power-up arpeggio (C5 -> E5 -> G5 -> C6 12.5% pulse)
    BLUE   = NES 4-bit triangle bass boop (G3 196 Hz -> D3 146.83 Hz)
    PURPLE = 8-bit star trill (F#5 / A5 / D6 alternating pulse)
    ORANGE = Arcade staircase laser-blip (C6 -> G5 -> D5 -> A4)
    ERROR  = Classic 8-bit descending game-over buzz + LFSR rattle (500 ms)

Output: 44100 Hz mono 16-bit PCM, peak-normalized to -1.0 dBFS.
        260-320 ms per button tone, 500 ms for error tone.
        Zero external dependencies (Python standard library only).

Usage:
    python3 tools/sounds/retro/generate.py app/src/main/res/raw
"""

import argparse
import math
import os
import struct
import wave

SAMPLE_RATE = 44100
PEAK_DBFS = -1.0


def envelope(idx: int, total: int, attack_s: float = 0.003, release_s: float = 0.030) -> float:
    a = max(1, int(attack_s * SAMPLE_RATE))
    r = max(1, int(release_s * SAMPLE_RATE))
    if idx < a:
        return idx / a
    rem = total - 1 - idx
    if rem < r:
        return max(0.0, rem / r)
    return 1.0


def pulse_wave(phase: float, duty: float = 0.5) -> float:
    frac = (phase / (2.0 * math.pi)) % 1.0
    # Zero-mean asymmetric pulse (2 * ((1 - duty) vs -duty)): keeps 50% duty at +/-1.0
    # while eliminating DC offset on 12.5% and 25% duty waveforms.
    return 2.0 * ((1.0 - duty) if frac < duty else -duty)


def nes_triangle_4bit(phase: float) -> float:
    """Quantized 16-step (4-bit) NES triangle wave."""
    frac = (phase / (2.0 * math.pi)) % 1.0
    step = int(frac * 32)  # 0..31 -> 16 levels up, 16 levels down
    tri = step if step < 16 else 31 - step
    return (tri / 7.5) - 1.0


def synth_green_coin(duration_s: float = 0.28) -> list[float]:
    n = int(duration_s * SAMPLE_RATE)
    two_pi = 2.0 * math.pi
    switch_idx = int(0.075 * SAMPLE_RATE)
    phase = 0.0
    samples: list[float] = []
    for i in range(n):
        f = 987.77 if i < switch_idx else 1318.51  # B5 -> E6
        phase += two_pi * f / SAMPLE_RATE
        decay = 1.0 if i < switch_idx else math.exp(-(i - switch_idx) / (0.14 * SAMPLE_RATE))
        val = pulse_wave(phase, duty=0.25) * decay * envelope(i, n, 0.002, 0.025)
        samples.append(val)
    return samples


def synth_red_jump(duration_s: float = 0.26) -> list[float]:
    n = int(duration_s * SAMPLE_RATE)
    two_pi = 2.0 * math.pi
    phase = 0.0
    samples: list[float] = []
    for i in range(n):
        frac = i / max(1, n - 1)
        f = 220.0 + 440.0 * (frac ** 0.75)
        phase += two_pi * f / SAMPLE_RATE
        val = pulse_wave(phase, duty=0.5) * envelope(i, n, 0.003, 0.03)
        samples.append(val)
    return samples


def synth_yellow_powerup(duration_s: float = 0.30) -> list[float]:
    n = int(duration_s * SAMPLE_RATE)
    two_pi = 2.0 * math.pi
    notes = (523.25, 659.25, 783.99, 1046.50)  # C5 E5 G5 C6
    step = max(1, n // len(notes))
    phase = 0.0
    samples: list[float] = []
    for i in range(n):
        f = notes[min(len(notes) - 1, i // step)]
        phase += two_pi * f / SAMPLE_RATE
        val = pulse_wave(phase, duty=0.125) * envelope(i, n, 0.002, 0.03)
        samples.append(val)
    return samples


def synth_blue_triangle_boop(duration_s: float = 0.28) -> list[float]:
    n = int(duration_s * SAMPLE_RATE)
    two_pi = 2.0 * math.pi
    half = n // 2
    phase = 0.0
    samples: list[float] = []
    for i in range(n):
        f = 196.00 if i < half else 146.83  # G3 -> D3
        phase += two_pi * f / SAMPLE_RATE
        # Add subtle octave harmonic so 4-bit triangle reads clearly on small speakers
        val = (0.8 * nes_triangle_4bit(phase) + 0.25 * math.sin(2.0 * phase)) * envelope(i, n, 0.004, 0.035)
        samples.append(val)
    return samples


def synth_purple_star_trill(duration_s: float = 0.30) -> list[float]:
    n = int(duration_s * SAMPLE_RATE)
    two_pi = 2.0 * math.pi
    notes = (739.99, 880.00, 1174.66)  # F#5, A5, D6
    trill_samples = int(0.035 * SAMPLE_RATE)
    phase = 0.0
    samples: list[float] = []
    for i in range(n):
        f = notes[(i // trill_samples) % len(notes)]
        phase += two_pi * f / SAMPLE_RATE
        val = pulse_wave(phase, duty=0.25) * envelope(i, n, 0.002, 0.03)
        samples.append(val)
    return samples


def synth_orange_staircase(duration_s: float = 0.28) -> list[float]:
    n = int(duration_s * SAMPLE_RATE)
    two_pi = 2.0 * math.pi
    notes = (1046.50, 783.99, 587.33, 440.00)  # C6 -> G5 -> D5 -> A4
    step = max(1, n // len(notes))
    phase = 0.0
    samples: list[float] = []
    for i in range(n):
        f = notes[min(len(notes) - 1, i // step)]
        phase += two_pi * f / SAMPLE_RATE
        val = pulse_wave(phase, duty=0.25) * envelope(i, n, 0.002, 0.03)
        samples.append(val)
    return samples


def synth_error_retro(duration_s: float = 0.50) -> list[float]:
    n = int(duration_s * SAMPLE_RATE)
    two_pi = 2.0 * math.pi
    steps = (196.00, 164.81, 130.81, 98.00)  # Descending G3 -> E3 -> C3 -> G2
    step_len = max(1, n // len(steps))
    phase = 0.0
    lfsr = 0xACE1
    noise_hold = 1.0
    samples: list[float] = []
    for i in range(n):
        f = steps[min(len(steps) - 1, i // step_len)]
        phase += two_pi * f / SAMPLE_RATE
        if i % 16 == 0:
            bit = ((lfsr >> 0) ^ (lfsr >> 2) ^ (lfsr >> 3) ^ (lfsr >> 5)) & 1
            lfsr = (lfsr >> 1) | (bit << 15)
            noise_hold = 1.0 if (lfsr & 1) else -1.0
        val = (0.75 * pulse_wave(phase, duty=0.5) + 0.25 * noise_hold) * envelope(i, n, 0.003, 0.045)
        samples.append(val)
    return samples


GENERATORS = {
    "green": synth_green_coin,
    "red": synth_red_jump,
    "yellow": synth_yellow_powerup,
    "blue": synth_blue_triangle_boop,
    "purple": synth_purple_star_trill,
    "orange": synth_orange_staircase,
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
    parser = argparse.ArgumentParser(description="Generate Retro Gaming sound pack WAVs.")
    parser.add_argument("out_dir", help="Output directory (e.g. app/src/main/res/raw)")
    args = parser.parse_args()
    os.makedirs(args.out_dir, exist_ok=True)
    print(f"Generating Retro Gaming pack into {args.out_dir}")
    for color, gen in GENERATORS.items():
        write_wav(os.path.join(args.out_dir, f"retro_{color}_tone.wav"), gen())
    write_wav(os.path.join(args.out_dir, "retro_error_tone.wav"), synth_error_retro())


if __name__ == "__main__":
    main()
