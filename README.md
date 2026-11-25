# WordCraft - Word Puzzle Game (Android)

A Wordscapes-style word puzzle game for Android built with **Kotlin**, **XML Views (`ViewBinding`)**, **Custom Canvas Views**, and **MVVM architecture**.

---

## 🌟 Features

- **Crossword Grid (`CrosswordGridView`)**: Custom `View` that auto-scales to any screen size, with bounce-reveal animations for newly solved words and gold indicator dots for hinted cells.
- **Circular Letter Wheel (`LetterWheelView`)**: Custom `Canvas` `View` supporting:
  - Continuous swipe gestures with a dynamic rubber-band line tracking the finger in real time.
  - Glowing dual-stroke connections between selected letters and scale-up micro-interactions.
  - Backtracking undo (drag back over the previous letter to deselect it).
  - Animated 360° shuffle rotation.
- **Word Validation & Feedback**:
  - **Valid Grid Words**: Pop-revealed in the crossword grid, +10 coins, pentatonic/chord sound FX.
  - **Bonus Words**: Extra valid words award +5 coins and are tracked in `BonusWordsDialogFragment`.
  - **Invalid / Duplicate Words**: Horizontal shake animation on the preview pill with color-coded feedback.
- **Level Dataset & Progression**:
  - **15 Playable Levels** loaded from `app/src/main/assets/levels.json` (3–5 letter wheels, intersecting crossword layouts, bonus words).
  - Confetti particle celebration (`ConfettiView`) and `LevelCompleteDialogFragment` on puzzle completion.
- **Navigation & State Retention**:
  - Single-Activity (`MainActivity`) + FragmentManager back stack (`HomeFragment` → `LevelSelectFragment` → `GameFragment` → `PauseDialogFragment`).
  - System back button handling with an Exit confirmation dialog on `HomeFragment` and Pause menu on `GameFragment`.
  - State survives configuration changes (screen rotation) via `GameViewModel` (`activityViewModels()`) and app restarts via `GamePreferences`.

---

## 🏛️ Architecture

The app follows **MVVM** with **Unidirectional Data Flow (UDF)**:

- **Data Layer**: `LevelRepository` (parses `assets/levels.json` and builds the 2D `CellData?` grid matrix) & `GamePreferences` (`SharedPreferences` persistence).
- **ViewModel Layer**: `GameViewModel` exposes immutable `StateFlow<GameUiState>` for UI state and `SharedFlow<GameUiEffect>` for one-off haptic events.
- **Audio Engine**: `SoundEffects` synthesizes pentatonic chimes, victory arpeggios, and error tones procedurally via Android `AudioTrack` (zero external audio assets).
- **UI Layer**: `MainActivity`, `HomeFragment`, `LevelSelectFragment` (`RecyclerView` + `LevelAdapter`), `GameFragment`, custom Canvas views, and `DialogFragment`s.

---

## 👆 Custom Swipe & Gesture Logic (`LetterWheelView`)

1. **Radial Layout**: Letters are positioned evenly around the wheel center:
   $$\theta_i = -\frac{\pi}{2} + i \cdot \frac{2\pi}{N}, \quad (x_i, y_i) = (c_x + R \cos\theta_i, \; c_y + R \sin\theta_i)$$
2. **Hit Testing**: `onTouchEvent` checks Euclidean distance (`hypot`) from `(event.x, event.y)` to each letter center within a `36dp` hit radius.
3. **Dynamic Line Tracking**: `onDraw` renders connected letter centers with a `Path` (outer glow + inner gold stroke) and extends the path to the active `touchPoint`.
4. **Backtracking**: Dragging back onto `selectedIndices[size - 2]` pops the last selected letter without lifting the finger.

---

## 📊 Level Data Structure

Levels live in `app/src/main/assets/levels.json`:

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

---

## 🛠️ Building & Testing

- **Playable APK**: [`WordCraft-v1.0.0.apk`](WordCraft-v1.0.0.apk) (6.4 MB)

```bash
# Run unit tests
./gradlew test

# Build debug APK
./gradlew assembleDebug
```
