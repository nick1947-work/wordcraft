# WordCraft - Word Puzzle Game (Android)

A fully functional, highly polished Wordscapes-style word puzzle game built for Android using modern Jetpack Compose, Kotlin, and MVI/MVVM architecture.

---

## 🌟 Highlights & Features

- **Crossword-Style Grid**: Dynamic, auto-scaling crossword grid with smooth reveal animations, flip transitions, and hint highlights.
- **Circular Letter Wheel**: Custom Compose Canvas drawing featuring:
  - Dynamic gradient line visually tracking real-time finger movement.
  - Glowing connections between selected letters.
  - Scale bounce micro-interactions on letter selection.
  - Dynamic letter backtracking (drag back to undo the previous selection).
  - Rapid swiping and touch cancellation resilience.
- **Word Validation & Feedback**:
  - **Valid Grid Words**: Revealed with bounce animation into the crossword grid, chimes, and coin rewards.
  - **Bonus Words**: Discovered extra words reward bonus coins and are tracked in the Bonus Words vault.
  - **Invalid Words**: Interactive preview bar shake with red error tint and low tone buzz.
  - **Already Found Words**: Warning shake with amber highlight.
- **Progression & Level Dataset**:
  - **15 Handcrafted, Fully Playable Levels** loaded from structured JSON assets (`assets/levels.json`), featuring 3 to 5 letter wheels, intersecting crossword layouts, and bonus word collections.
  - Automatic level progression upon puzzle completion with confetti particle celebration and victory dialog.
- **Sound & Haptics Engine**:
  - Dynamic pentatonic chime progression (each letter connected sounds higher in pitch).
  - Procedural sound synthesis via Android `AudioTrack` (zero external asset dependencies, zero latency).
  - Tactile feedback (haptic ticks on letter hover, success pulses, error double-buzz).
  - In-game toggles for sound and haptics in the Pause menu.
- **Flawless Navigation & State Management**:
  - Complete back stack hygiene (`Home` → `Level Select` → `Gameplay` → `Pause`).
  - Game state survives configuration changes (screen rotation) and app backgrounding.
  - Persistent progression (unlocked levels, coins, bonus words) saved via `GamePreferences`.

---

## 🏛️ Architectural Choices

The project adheres to **Clean Architecture** and **Unidirectional Data Flow (UDF)** principles:

```
┌────────────────────────────────────────────────────────┐
│                        UI Layer                        │
│   (Jetpack Compose + Custom Canvas + AnimatedContent)  │
└───────────────────────▲────────────────────────────────┘
                        │ Observes StateFlow<GameUiState>
                        │ Receives SharedFlow<GameUiEffect>
┌───────────────────────┴────────────────────────────────┐
│                     ViewModel                          │
│                   GameViewModel                        │
│  - Letter selection & backtracking gesture engine       │
│  - Word validation & bonus word scoring               │
│  - Audio & Haptic event triggers                       │
│  - Navigation state & backstack hygiene                │
└───────────────────────▲────────────────────────────────┘
                        │
┌───────────────────────┴────────────────────────────────┐
│                     Data Layer                         │
│  - LevelRepository: parses structured JSON level data   │
│  - GamePreferences: persistent progress & settings     │
│  - SoundEffects: synthesized low-latency audio engine  │
└────────────────────────────────────────────────────────┘
```

### 1. Presentation & State Observation
- Single source of truth: `StateFlow<GameUiState>` in `GameViewModel`.
- UI events and gesture inputs are sent directly to the ViewModel, which produces new immutable UI state copies (`uiState.copy(...)`).
- One-off transient events (such as tactile haptic pulses) are dispatched via `SharedFlow<GameUiEffect>`.

### 2. State Retention & Lifecycle Hygiene
- The ViewModel is scoped to the activity/navigation stack, guaranteeing retention across screen rotations and configuration changes.
- System back button handling is cleanly managed via Compose's `BackHandler`:
  - From `GAMEPLAY`: opens the Pause modal or returns to `LEVEL_SELECT`.
  - From `LEVEL_SELECT`: navigates back to `HOME`.
  - From `HOME`: exits cleanly without orphaned dialogs or memory leaks.

---

## 👆 Custom Swipe & Gesture Handling

The letter wheel is rendered on a Compose `Canvas` backed by pointer gesture detection:

1. **Radial Geometry**:
   Letter positions are computed radially around the wheel center:
   $$\theta_i = -\frac{\pi}{2} + i \cdot \frac{2\pi}{N}$$
   $$(x_i, y_i) = (c_x + R \cos\theta_i, \; c_y + R \sin\theta_i)$$

2. **Hit Testing & Tolerance**:
   Each letter circle has an active hit radius ($r_{hit} \approx 42\text{dp}$) with hysteresis, preventing false un-touches during fast finger movement.

3. **Dynamic Line Tracking**:
   - For connected letters, static lines connect center-to-center with a 2-pass stroke (an outer translucent glow stroke and an inner vibrant golden stroke with `StrokeCap.Round` and `StrokeJoin.Round`).
   - A dynamic rubber-band line tracks from the last selected letter directly to the player's active finger coordinate (`touchPosition`).

4. **Backtracking Undo**:
   If the player drags their finger back over the second-to-last selected letter, the gesture engine pops the last letter, allowing smooth corrections without having to lift the finger.

5. **Rapid Swiping & Cancellation**:
   Pointer input handles `onDrag`, `onDragEnd`, and `onDragCancel` defensively, ensuring all touch positions and transient state are cleaned up gracefully.

---

## 📊 Level Data Structure

Levels are stored in `app/src/main/assets/levels.json` and parsed cleanly by `LevelRepository`:

```json
{
  "levels": [
    {
      "id": 4,
      "title": "Green Valley",
      "letters": ["P", "O", "S", "T"],
      "grid": {
        "rows": 4,
        "cols": 6,
        "words": [
          { "word": "POST", "startRow": 0, "startCol": 0, "isHorizontal": true },
          { "word": "STOP", "startRow": 0, "startCol": 2, "isHorizontal": false },
          { "word": "TOP", "startRow": 1, "startCol": 2, "isHorizontal": true }
        ]
      },
      "bonusWords": ["SPOT", "POTS", "TOPS", "SOP", "OPT", "OPTS", "SO"]
    }
  ]
}
```

- Each word specifies its exact `startRow`, `startCol`, and orientation (`isHorizontal`).
- Intersections are mathematically validated so intersecting cells share identical letters.
- `LevelRepository.buildGridMatrix(...)` transforms this schema into an indexed 2D matrix of `CellData?`, making crossword cell rendering $O(1)$ per cell.

---

## 🛠️ Building & Running

### Requirements
- Android SDK 35 / 34
- JDK 17
- Gradle 8.x / 9.x (Gradle wrapper included)

### Commands
```bash
# Run unit tests
./gradlew test

# Assemble Debug APK
./gradlew assembleDebug

# Output APK path:
# app/build/outputs/apk/debug/app-debug.apk
```
