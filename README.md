# Simon Says - Classic Memory Game

A modern Android implementation of the classic Simon Says electronic memory game, built with Jetpack Compose and Material Design 3.

## Overview

Simon Says is an engaging memory game where players must repeat increasingly complex sequences of colored lights and sounds. Each round adds a new random element to the sequence, progressively challenging the player's memory and concentration.

## Features

### Core Gameplay
- **Classic Simon Mechanics**: Four colored buttons (Green, Red, Yellow, Blue) arranged in a 2x2 grid
- **Progressive Difficulty**: Each successful round adds a new element to the sequence
- **Time-Limited Gameplay**: 10-second timeout for player responses
- **High Score Tracking**: Persistent high score across game sessions
- **Visual Feedback**: 3D button animations with spring physics and gradient effects

### Sound System
- **Multiple Sound Packs**: Choose from 7 different sound themes:
  - Standard (classic Simon tones)
  - Funny
  - Electronic
  - Retro
  - Musical
  - Nature
  - Sci-Fi
- **Sound Control**: Mute/unmute toggle in top bar
- **Error Feedback**: Distinct error tone for incorrect sequences

### Customization
- **Vibration Toggle**: Optional haptic feedback (100ms) on button presses
- **Settings Screen**: Easy access to all game preferences
- **Dark Theme**: Material Design 3 dark theme support

### Technical Features
- **Lifecycle-Aware**: Proper handling of app pause/resume states
- **State Preservation**: Game state maintained during background transitions
- **Responsive Layout**: Adapts to different screen sizes and orientations
- **Modern UI**: Jetpack Compose with Material Design 3 components

## Technical Stack

### Core Technologies
- **Language**: Kotlin 2.1.10
- **UI Framework**: Jetpack Compose (BOM 2025.03.01)
- **Architecture**: MVVM with StateFlow
- **Dependency Injection**: Koin 4.0.3
- **Build System**: Gradle (Kotlin DSL)

### Android SDK
- **Minimum SDK**: 31 (Android 12)
- **Target SDK**: 35 (Android 15)
- **Compile SDK**: 35

### Key Libraries
- `androidx.core:core-ktx` - Android KTX extensions
- `androidx.activity:activity-compose` - Compose activity integration
- `androidx.lifecycle:lifecycle-runtime-ktx` - Lifecycle components
- `androidx.compose.material3:material3` - Material Design 3
- `io.insert-koin:koin-android` - Dependency injection

## Project Structure

```
MyApplication2/
├── app/src/main/
│   ├── java/com/guy/myapplication/
│   │   ├── SimonApp.kt              # Application class with DI setup
│   │   ├── ui/
│   │   │   ├── MainActivity.kt      # Main activity entry point
│   │   │   ├── screens/
│   │   │   │   ├── SimonGameScreen.kt    # Main game UI
│   │   │   │   └── SettingsScreen.kt     # Settings UI
│   │   │   ├── components/
│   │   │   │   └── SimonPanel.kt         # Colored button component
│   │   │   ├── viewmodels/
│   │   │   │   └── SimonGameViewModel.kt # Game logic controller
│   │   │   └── theme/                     # Material 3 theme
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   ├── SimonGameState.kt     # Game state sealed class
│   │   │   │   └── SimonGameUiState.kt   # UI state data class
│   │   │   └── enums/
│   │   │       ├── SimonButton.kt        # Button enum
│   │   │       └── SoundPack.kt          # Sound pack enum
│   │   ├── data/
│   │   │   └── manager/
│   │   │       └── SimonSoundManager.kt  # Audio & vibration
│   │   └── di/
│   │       └── AppModule.kt              # Koin DI module
│   └── res/
│       ├── raw/                           # Audio files
│       ├── drawable/                      # Icons & graphics
│       └── values/                        # Resources
└── gradle/
    └── libs.versions.toml                 # Version catalog
```

## Getting Started

### Prerequisites
- Android Studio Ladybug or later
- JDK 11 or higher
- Android SDK 31+
- Gradle 8.9.1+

### Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd MyApplication2
```

2. Open the project in Android Studio

3. Sync Gradle dependencies

4. Run the application:
```bash
./gradlew installDebug
```

## Build Commands

### Development
```bash
# Build the project
./gradlew build

# Install debug APK
./gradlew installDebug

# Clean build artifacts
./gradlew clean
```

### Testing
```bash
# Run unit tests
./gradlew test

# Run specific test
./gradlew test --tests "com.guy.myapplication.ExampleUnitTest"

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run lint checks
./gradlew lint
```

### Release
```bash
# Build release APK
./gradlew assembleRelease

# Build signed release bundle
./gradlew bundleRelease
```

## Architecture

### MVVM Pattern

The application follows the Model-View-ViewModel pattern with Jetpack Compose:

- **View Layer**: Composable functions (`SimonGameScreen`, `SettingsScreen`, `SimonPanel`)
- **ViewModel Layer**: `SimonGameViewModel` manages game logic and UI state
- **Data Layer**: `SimonSoundManager` handles audio/vibration, SharedPreferences for persistence
- **Domain Layer**: Models and enums defining business logic

### State Management

- **Centralized State**: `SimonGameUiState` data class holds complete UI state
- **Reactive Updates**: StateFlow exposes state to UI layer
- **Immutable State**: State updates using `.update { it.copy() }` pattern
- **Lifecycle-Aware**: `viewModelScope` ensures coroutines respect lifecycle

### Game States

The game operates in five distinct states:

1. **WaitingToStart**: Initial state, ready to begin
2. **ShowingSequence**: Computer displays the sequence
3. **PlayerRepeating**: Waiting for player input with timeout
4. **GameOver**: Game ended, show play again button
5. **Settings**: Settings screen active

## Game Logic Flow

```
WaitingToStart
    ↓ (tap center button)
ShowingSequence (play sequence with animations/sounds)
    ↓
PlayerRepeating (10-second timeout)
    ↓ (correct sequence)
Generate new button → ShowingSequence
    ↓ (wrong button or timeout)
GameOver (flash buttons, update high score)
    ↓ (tap play again)
StartNewGame → WaitingToStart
```

## Configuration

### Permissions

The app requires the following permission:
- `android.permission.VIBRATE` - For haptic feedback

### Audio Configuration

- Audio stream: `STREAM_MUSIC`
- SoundPool max streams: 10
- Optimized for short game sounds

### Theme

- Material Design 3 with dark theme
- Edge-to-edge display
- Gradient effects on buttons
- Spring-based animations

## Code Style Guidelines

Following the project's CLAUDE.md guidelines:

- **Architecture**: MVVM with ViewModels and StateFlow
- **Naming**: PascalCase for classes, camelCase for functions, SCREAMING_SNAKE_CASE for constants
- **Documentation**: KDoc comments for public APIs
- **Error Handling**: Try/catch with detailed logging
- **Asynchronous**: Kotlin Coroutines with viewModelScope
- **State Management**: Immutable state with StateFlow
- **Logging**: TAG constant with Log.d/e/w

## Features in Development

Currently implemented sound packs:
- Standard (complete)
- Funny (complete)

Planned sound packs:
- Electronic
- Retro
- Musical
- Nature
- Sci-Fi

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature-name`
3. Follow the code style guidelines in CLAUDE.md
4. Write unit tests for new features
5. Commit your changes: `git commit -m 'Add some feature'`
6. Push to the branch: `git push origin feature/your-feature-name`
7. Submit a pull request

## Testing

The project includes:
- Unit tests with JUnit 4
- Instrumented tests with Espresso
- Compose UI tests

Run tests before submitting changes:
```bash
./gradlew test lint
```

## License

This project is available under the MIT License (or your chosen license).

## Acknowledgments

- Inspired by the classic Simon electronic game by Ralph H. Baer and Howard J. Morrison
- Built with modern Android development best practices
- Uses Material Design 3 components

---

**Version**: 1.0.0
**Build**: See `app/build.gradle.kts` for current version details
