# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Memory Lights - a memory game built with Jetpack Compose and Material 3, targeting Android SDK 31+. Players repeat increasingly complex sequences of colored button presses.

**Package**: `com.happypuppy.memorylights`

## Build Commands

- Build: `./gradlew build`
- Run app: `./gradlew installDebug`
- Unit tests: `./gradlew test`
- Single test: `./gradlew test --tests "com.happypuppy.memorylights.ExampleUnitTest"`
- Instrumented tests: `./gradlew connectedAndroidTest`
- Lint checks: `./gradlew lint`
- Clean: `./gradlew clean`

## Architecture

### MVVM Pattern with Koin DI

- **SimonGameViewModel**: Core game logic with StateFlow, lifecycle-aware via DefaultLifecycleObserver
- **SimonSoundManager**: Audio playback (SoundPool) and vibration management
- **StatisticsManager**: Persists game statistics via SharedPreferences
- **Single Activity**: MainActivity with edge-to-edge support

### Package Structure

```
com.happypuppy.memorylights/
├── data/manager/          # SimonSoundManager, StatisticsManager
├── di/                    # Koin AppModule
├── domain/
│   ├── enums/            # SimonButton (6 colors), SoundPack (7 themes)
│   └── model/            # SimonGameState, SimonGameUiState, GameStatistics
└── ui/
    ├── components/       # SimonPanel, ParticleEffect
    ├── screens/          # SimonGameScreen, SettingsScreen, StatisticsScreen
    ├── theme/            # Color, Theme, Type
    └── viewmodels/       # SimonGameViewModel
```

### State Management

- **GameState**: WaitingToStart, ShowingSequence, PlayerRepeating, GameOver, Settings
- **ScreenState**: Game, Settings, Statistics
- StateFlow with immutable state updates via `.update { it.copy(...) }`

## Game Modes

- **Standard (4-button)**: GREEN, RED, YELLOW, BLUE
- **Memory Lights+ (6-button)**: Adds PURPLE, ORANGE

## Technical Stack

- **Kotlin**: 2.4.21 (built into AGP 9.5.3; no `kotlin-android` plugin)
- **Compose BOM**: 2026.11.01
- **Koin**: 4.2.2
- **Gradle**: 9.9.2
- **Compile SDK**: 37.1, **Target SDK**: 36, **Min SDK**: 31

## Code Style

- Use TAG constant with Log.d/e/w for debugging
- Resource naming: `{soundpack}_{color}_tone.wav` (e.g., `standard_green_tone.wav`)
- Use viewModelScope for coroutine operations
- Immutable UI state via StateFlow

## Mobile Testing (MCP)

- `mobile_list_elements_on_screen` - Get UI elements with coordinates (use instead of estimating from screenshots)
- `mobile_take_screenshot` - Capture current screen
- Clear logs before testing: `adb logcat -c`
- Monitor app logs: `adb logcat | grep com.happypuppy.memorylights`

## Future Development

- More levels
- More sounds (Musical, Nature, Sci-Fi packs currently use Standard sounds as fallback)
- Upload to Play Store
