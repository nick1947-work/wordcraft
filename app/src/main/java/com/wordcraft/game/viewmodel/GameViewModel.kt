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
        val soundOn = preferences.soundEnabled
        sounds.isEnabled = soundOn

        _uiState.update {
            it.copy(
                allLevels = allLevels,
                highestUnlockedLevel = preferences.highestUnlockedLevel,
                coins = preferences.coins,
                soundEnabled = soundOn,
                hapticsEnabled = preferences.hapticsEnabled
            )
        }

        val initialLevelId = preferences.currentLevelId.coerceIn(1, allLevels.size.coerceAtLeast(1))
        loadLevel(initialLevelId)
    }

    fun selectLevel(levelId: Int) = loadLevel(levelId)

    fun restartCurrentLevel() = loadLevel(_uiState.value.currentLevel?.id ?: 1)

    fun nextLevel() {
        val nextId = (_uiState.value.currentLevel?.id ?: 1) + 1
        if (nextId <= _uiState.value.allLevels.size) loadLevel(nextId)
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

    private fun loadLevel(levelId: Int) {
        val level = repository.getLevelById(levelId) ?: return
        preferences.currentLevelId = levelId

        _uiState.update {
            it.copy(
                currentLevel = level,
                gridMatrix = repository.buildGridMatrix(level, emptySet(), emptySet()),
                revealedWords = emptySet(),
                bonusWordsFound = preferences.getBonusWordsFound(levelId),
                hintedCells = emptySet(),
                wheelLetters = level.letters.shuffled(),
                feedbackType = FeedbackType.NONE,
                feedbackText = "",
                isLevelComplete = false
            )
        }
    }

    fun onLetterTouch(selectionOrder: Int) {
        sounds.playLetterSelect(selectionOrder)
        emitHaptic(GameUiEffect.VibrateLight)
    }

    fun submitWord(rawWord: String) {
        val word = rawWord.trim().uppercase()
        val level = _uiState.value.currentLevel ?: return
        if (word.length < 2) return

        val state = _uiState.value
        val matchedGridWord = level.grid.words.find { it.word.equals(word, ignoreCase = true) }?.word
        val matchedBonusWord = level.bonusWords.find { it.equals(word, ignoreCase = true) }

        feedbackJob?.cancel()

        when {
            matchedGridWord != null -> {
                if (state.revealedWords.contains(matchedGridWord)) {
                    showFeedback(FeedbackType.ALREADY_FOUND, "Already Found!")
                    emitHaptic(GameUiEffect.VibrateMedium)
                } else {
                    val newRevealed = state.revealedWords + matchedGridWord
                    val newCoins = state.coins + 10
                    preferences.coins = newCoins

                    sounds.playWordValid()
                    emitHaptic(GameUiEffect.VibrateMedium)

                    _uiState.update {
                        it.copy(
                            revealedWords = newRevealed,
                            gridMatrix = repository.buildGridMatrix(level, newRevealed, state.hintedCells),
                            coins = newCoins,
                            feedbackType = FeedbackType.VALID,
                            feedbackText = "Brilliant!"
                        )
                    }

                    if (newRevealed.size >= level.grid.words.size) {
                        handleLevelComplete(level)
                    } else {
                        scheduleClearFeedback(1200)
                    }
                }
            }
            matchedBonusWord != null -> {
                if (state.bonusWordsFound.contains(matchedBonusWord)) {
                    showFeedback(FeedbackType.ALREADY_FOUND, "Bonus Already Found!")
                    emitHaptic(GameUiEffect.VibrateLight)
                } else {
                    val newBonus = state.bonusWordsFound + matchedBonusWord
                    preferences.addBonusWord(level.id, matchedBonusWord)
                    val newCoins = state.coins + 5
                    preferences.coins = newCoins

                    sounds.playBonusWord()
                    emitHaptic(GameUiEffect.VibrateMedium)

                    _uiState.update {
                        it.copy(
                            bonusWordsFound = newBonus,
                            coins = newCoins,
                            feedbackType = FeedbackType.BONUS,
                            feedbackText = "Bonus Word +5 🪙"
                        )
                    }
                    scheduleClearFeedback(1400)
                }
            }
            else -> {
                showFeedback(FeedbackType.INVALID, "Not in Puzzle")
                emitHaptic(GameUiEffect.VibrateError)
                sounds.playWordInvalid()
            }
        }
    }

    private fun handleLevelComplete(level: Level) {
        preferences.setLevelCompleted(level.id, true)
        if (level.id + 1 > preferences.highestUnlockedLevel) {
            preferences.highestUnlockedLevel = level.id + 1
        }
        val finalCoins = preferences.coins + 50
        preferences.coins = finalCoins

        viewModelScope.launch {
            delay(500)
            sounds.playLevelComplete()
            _uiState.update {
                it.copy(
                    isLevelComplete = true,
                    coins = finalCoins,
                    highestUnlockedLevel = preferences.highestUnlockedLevel,
                    feedbackType = FeedbackType.NONE,
                    feedbackText = ""
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
            _uiState.update { it.copy(feedbackType = FeedbackType.NONE, feedbackText = "") }
        }
    }

    fun shuffleWheel() {
        val current = _uiState.value.wheelLetters
        if (current.size <= 1) return

        var shuffled = current.shuffled()
        var attempts = 0
        while (shuffled == current && attempts < 5) {
            shuffled = current.shuffled()
            attempts++
        }

        sounds.playShuffle()
        emitHaptic(GameUiEffect.VibrateLight)
        _uiState.update { it.copy(wheelLetters = shuffled) }
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

        val currentMatrix = repository.buildGridMatrix(level, state.revealedWords, state.hintedCells)
        val unrevealedCells = buildList {
            for (r in 0 until level.grid.rows) {
                for (c in 0 until level.grid.cols) {
                    if (currentMatrix[r][c]?.isRevealed == false) add(CellPosition(r, c))
                }
            }
        }
        if (unrevealedCells.isEmpty()) return

        val newHinted = state.hintedCells + unrevealedCells.random()
        val updatedCoins = state.coins - hintCost
        preferences.coins = updatedCoins

        val updatedMatrix = repository.buildGridMatrix(level, state.revealedWords, newHinted)
        val newlySolvedWords = level.grid.words.filter { gridWord ->
            gridWord.cells().all { (r, c) -> updatedMatrix[r][c]?.isRevealed == true }
        }.map { it.word }.toSet()
        val allRevealed = state.revealedWords + newlySolvedWords

        sounds.playHint()
        emitHaptic(GameUiEffect.VibrateMedium)

        _uiState.update {
            it.copy(
                hintedCells = newHinted,
                revealedWords = allRevealed,
                coins = updatedCoins,
                gridMatrix = updatedMatrix,
                feedbackType = FeedbackType.BONUS,
                feedbackText = "Letter Revealed!"
            )
        }
        scheduleClearFeedback(1200)

        if (allRevealed.size >= level.grid.words.size && !_uiState.value.isLevelComplete) {
            handleLevelComplete(level)
        }
    }

    private fun emitHaptic(effect: GameUiEffect) {
        if (_uiState.value.hapticsEnabled) _uiEffects.tryEmit(effect)
    }
}
