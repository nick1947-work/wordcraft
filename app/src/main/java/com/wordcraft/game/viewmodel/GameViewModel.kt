package com.wordcraft.game.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wordcraft.game.audio.SoundEffects
import com.wordcraft.game.data.model.CellPosition
import com.wordcraft.game.data.model.Level
import com.wordcraft.game.data.repository.GamePreferences
import com.wordcraft.game.data.repository.LevelRepository
import com.wordcraft.game.domain.FeedbackType
import com.wordcraft.game.domain.GameUiEffect
import com.wordcraft.game.domain.GameUiState
import com.wordcraft.game.domain.Screen
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LevelRepository(application)
    private val preferences = GamePreferences(application)
    private val sounds = SoundEffects()

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _uiEffects = MutableSharedFlow<GameUiEffect>(extraBufferCapacity = 64)
    val uiEffects: SharedFlow<GameUiEffect> = _uiEffects.asSharedFlow()

    private var feedbackJob: Job? = null

    init {
        val allLevels = repository.getLevels()
        val highestUnlocked = preferences.highestUnlockedLevel
        val coins = preferences.coins
        val soundOn = preferences.soundEnabled
        val hapticsOn = preferences.hapticsEnabled
        sounds.isEnabled = soundOn

        _uiState.update {
            it.copy(
                allLevels = allLevels,
                highestUnlockedLevel = highestUnlocked,
                coins = coins,
                soundEnabled = soundOn,
                hapticsEnabled = hapticsOn
            )
        }

        // Start with the current saved level
        val initialLevelId = preferences.currentLevelId.coerceIn(1, (allLevels.size).coerceAtLeast(1))
        loadLevel(initialLevelId, targetScreen = Screen.HOME)
    }

    // ----------------------------------------------------
    // Navigation
    // ----------------------------------------------------

    fun navigateTo(screen: Screen) {
        _uiState.update { it.copy(currentScreen = screen, isPaused = false) }
    }

    fun handleBackPress(): Boolean {
        val state = _uiState.value
        return when (state.currentScreen) {
            Screen.GAMEPLAY -> {
                if (state.isPaused) {
                    _uiState.update { it.copy(isPaused = false) }
                    true
                } else if (state.isLevelComplete) {
                    navigateTo(Screen.LEVEL_SELECT)
                    true
                } else {
                    _uiState.update { it.copy(isPaused = true) }
                    true
                }
            }
            Screen.LEVEL_SELECT -> {
                navigateTo(Screen.HOME)
                true
            }
            Screen.HOME -> {
                false
            }
        }
    }

    fun selectLevel(levelId: Int) {
        loadLevel(levelId, targetScreen = Screen.GAMEPLAY)
    }

    fun restartCurrentLevel() {
        val currentId = _uiState.value.currentLevel?.id ?: 1
        loadLevel(currentId, targetScreen = Screen.GAMEPLAY)
    }

    fun nextLevel() {
        val currentId = _uiState.value.currentLevel?.id ?: 1
        val nextId = currentId + 1
        val all = _uiState.value.allLevels
        if (nextId <= all.size) {
            loadLevel(nextId, targetScreen = Screen.GAMEPLAY)
        } else {
            navigateTo(Screen.LEVEL_SELECT)
        }
    }

    fun pauseGame() {
        _uiState.update { it.copy(isPaused = true) }
    }

    fun resumeGame() {
        _uiState.update { it.copy(isPaused = false) }
    }

    fun toggleBonusDialog(show: Boolean) {
        _uiState.update { it.copy(showBonusDialog = show) }
    }

    fun toggleSound() {
        val newSound = !_uiState.value.soundEnabled
        preferences.soundEnabled = newSound
        sounds.isEnabled = newSound
        _uiState.update { it.copy(soundEnabled = newSound) }
    }

    fun toggleHaptics() {
        val newHaptics = !_uiState.value.hapticsEnabled
        preferences.hapticsEnabled = newHaptics
        _uiState.update { it.copy(hapticsEnabled = newHaptics) }
    }

    private fun loadLevel(levelId: Int, targetScreen: Screen) {
        val level = repository.getLevelById(levelId) ?: return
        preferences.currentLevelId = levelId

        val bonusFound = preferences.getBonusWordsFound(levelId)
        val matrix = repository.buildGridMatrix(level, emptySet(), emptySet())
        val initialLetters = level.letters.shuffled()

        _uiState.update {
            it.copy(
                currentScreen = targetScreen,
                currentLevel = level,
                gridMatrix = matrix,
                revealedWords = emptySet(),
                bonusWordsFound = bonusFound,
                hintedCells = emptySet(),
                wheelLetters = initialLetters,
                selectedIndices = emptyList(),
                currentWord = "",
                feedbackType = FeedbackType.NONE,
                feedbackText = "",
                isPaused = false,
                isLevelComplete = false,
                recentlySolvedWord = null
            )
        }
    }

    // ----------------------------------------------------
    // Gesture & Touch Event Handling
    // ----------------------------------------------------

    fun onLetterTouch(letterIndexInSelection: Int) {
        sounds.playLetterSelect(letterIndexInSelection)
        emitHaptic(GameUiEffect.VibrateLight)
    }

    fun onTouchDown(letterIndex: Int, pos: Any? = null) {
        onLetterTouch(0)
    }

    fun onTouchMove(pos: Any?, hoveredIndex: Int?) {
        // Handled within LetterWheelView
    }

    fun onTouchUp(word: String? = null) {
        val finalWord = word ?: _uiState.value.currentWord
        submitWord(finalWord)
    }

    fun submitWord(rawWord: String) {
        val word = rawWord.trim().uppercase()
        val level = _uiState.value.currentLevel ?: return
        if (word.isEmpty()) return

        validateWord(word, level)
    }

    private fun validateWord(word: String, level: Level) {
        val state = _uiState.value
        val isGridWord = level.grid.words.any { it.word.equals(word, ignoreCase = true) }
        val isBonusWord = level.bonusWords.any { it.equals(word, ignoreCase = true) }

        feedbackJob?.cancel()

        when {
            isGridWord -> {
                if (state.revealedWords.contains(word)) {
                    showFeedback(FeedbackType.ALREADY_FOUND, "Already Found!")
                    emitHaptic(GameUiEffect.VibrateMedium)
                } else {
                    // Newly found grid word!
                    val newRevealed = state.revealedWords + word
                    val updatedMatrix = repository.buildGridMatrix(level, newRevealed, state.hintedCells)
                    val newCoins = state.coins + 10
                    preferences.coins = newCoins

                    sounds.playWordValid()
                    emitHaptic(GameUiEffect.VibrateMedium)

                    _uiState.update {
                        it.copy(
                            revealedWords = newRevealed,
                            gridMatrix = updatedMatrix,
                            coins = newCoins,
                            recentlySolvedWord = word,
                            feedbackType = FeedbackType.VALID,
                            feedbackText = "Brilliant!"
                        )
                    }

                    // Check if all words are solved
                    val isComplete = newRevealed.size >= level.grid.words.size
                    if (isComplete) {
                        handleLevelComplete(level)
                    } else {
                        scheduleClearFeedback(1200)
                    }
                }
            }
            isBonusWord -> {
                if (state.bonusWordsFound.contains(word)) {
                    showFeedback(FeedbackType.ALREADY_FOUND, "Bonus Already Found!")
                    emitHaptic(GameUiEffect.VibrateLight)
                } else {
                    val newBonusFound = state.bonusWordsFound + word
                    preferences.addBonusWord(level.id, word)
                    val newCoins = state.coins + 5
                    preferences.coins = newCoins

                    sounds.playBonusWord()
                    emitHaptic(GameUiEffect.VibrateMedium)

                    _uiState.update {
                        it.copy(
                            bonusWordsFound = newBonusFound,
                            coins = newCoins,
                            feedbackType = FeedbackType.BONUS,
                            feedbackText = "Bonus Word +5 🪙"
                        )
                    }
                    scheduleClearFeedback(1400)
                }
            }
            else -> {
                if (word.length >= 2) {
                    showFeedback(FeedbackType.INVALID, "Not in Puzzle")
                    emitHaptic(GameUiEffect.VibrateError)
                    sounds.playWordInvalid()
                }
            }
        }
    }

    private fun handleLevelComplete(level: Level) {
        preferences.setLevelCompleted(level.id, true)
        val nextLevelId = level.id + 1
        if (nextLevelId > preferences.highestUnlockedLevel) {
            preferences.highestUnlockedLevel = nextLevelId
        }

        val bonusReward = 50
        val finalCoins = preferences.coins + bonusReward
        preferences.coins = finalCoins

        viewModelScope.launch {
            delay(500)
            sounds.playLevelComplete()
            _uiState.update {
                it.copy(
                    isLevelComplete = true,
                    coins = finalCoins,
                    highestUnlockedLevel = preferences.highestUnlockedLevel,
                    celebrationTimestamp = System.currentTimeMillis(),
                    feedbackType = FeedbackType.NONE
                )
            }
        }
    }

    private fun showFeedback(type: FeedbackType, text: String) {
        _uiState.update { it.copy(feedbackType = type, feedbackText = text) }
        scheduleClearFeedback(1200)
    }

    private fun scheduleClearFeedback(delayMs: Long) {
        feedbackJob?.cancel()
        feedbackJob = viewModelScope.launch {
            delay(delayMs)
            _uiState.update {
                it.copy(feedbackType = FeedbackType.NONE, feedbackText = "", recentlySolvedWord = null)
            }
        }
    }

    // ----------------------------------------------------
    // Actions: Shuffle & Hint
    // ----------------------------------------------------

    fun shuffleWheel() {
        val currentLetters = _uiState.value.wheelLetters
        if (currentLetters.size <= 1) return

        var shuffled = currentLetters.shuffled()
        var attempts = 0
        while (shuffled == currentLetters && attempts < 5) {
            shuffled = currentLetters.shuffled()
            attempts++
        }

        sounds.playShuffle()
        emitHaptic(GameUiEffect.VibrateLight)

        _uiState.update {
            it.copy(
                wheelLetters = shuffled,
                shuffleAnimKey = it.shuffleAnimKey + 1
            )
        }
    }

    fun useHint() {
        val state = _uiState.value
        val level = state.currentLevel ?: return
        val hintCost = 25

        if (state.coins < hintCost) {
            showFeedback(FeedbackType.INVALID, "Need 25 Coins!")
            emitHaptic(GameUiEffect.VibrateError)
            return
        }

        // Determine currently unrevealed cells directly from board matrix
        val currentMatrix = repository.buildGridMatrix(level, state.revealedWords, state.hintedCells)
        val unrevealedCells = mutableListOf<CellPosition>()
        for (r in 0 until level.grid.rows) {
            for (c in 0 until level.grid.cols) {
                val cell = currentMatrix[r][c]
                if (cell != null && !cell.isRevealed) {
                    unrevealedCells.add(CellPosition(r, c))
                }
            }
        }

        if (unrevealedCells.isEmpty()) return

        val targetCell = unrevealedCells.random()
        val newHinted = state.hintedCells + targetCell
        val updatedCoins = state.coins - hintCost
        preferences.coins = updatedCoins

        val updatedMatrix = repository.buildGridMatrix(level, state.revealedWords, newHinted)

        // Auto-complete words whose cells are now all revealed
        val newlySolvedWords = level.grid.words.filter { gridWord ->
            gridWord.cells().all { (r, c) -> updatedMatrix[r][c]?.isRevealed == true }
        }.map { it.word }.toSet()
        val allRevealedWords = state.revealedWords + newlySolvedWords

        sounds.playHint()
        emitHaptic(GameUiEffect.VibrateMedium)

        _uiState.update {
            it.copy(
                hintedCells = newHinted,
                revealedWords = allRevealedWords,
                coins = updatedCoins,
                gridMatrix = updatedMatrix,
                feedbackType = FeedbackType.BONUS,
                feedbackText = "Letter Revealed!"
            )
        }
        scheduleClearFeedback(1200)

        if (allRevealedWords.size >= level.grid.words.size && !_uiState.value.isLevelComplete) {
            handleLevelComplete(level)
        }
    }

    private fun emitHaptic(effect: GameUiEffect) {
        if (_uiState.value.hapticsEnabled) {
            _uiEffects.tryEmit(effect)
        }
    }
}
