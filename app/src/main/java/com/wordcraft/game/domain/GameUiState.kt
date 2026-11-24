package com.wordcraft.game.domain

import androidx.compose.ui.geometry.Offset
import com.wordcraft.game.data.model.CellData
import com.wordcraft.game.data.model.CellPosition
import com.wordcraft.game.data.model.Level

enum class Screen {
    HOME,
    LEVEL_SELECT,
    GAMEPLAY
}

enum class FeedbackType {
    NONE,
    VALID,
    BONUS,
    ALREADY_FOUND,
    INVALID
}

data class GameUiState(
    val currentScreen: Screen = Screen.HOME,
    val currentLevel: Level? = null,
    val gridMatrix: List<List<CellData?>> = emptyList(),
    val revealedWords: Set<String> = emptySet(),
    val bonusWordsFound: Set<String> = emptySet(),
    val hintedCells: Set<CellPosition> = emptySet(),
    val wheelLetters: List<String> = emptyList(),
    val selectedIndices: List<Int> = emptyList(),
    val currentWord: String = "",
    val touchPosition: Offset? = null,
    val feedbackType: FeedbackType = FeedbackType.NONE,
    val feedbackText: String = "",
    val coins: Int = 200,
    val isPaused: Boolean = false,
    val isLevelComplete: Boolean = false,
    val showBonusDialog: Boolean = false,
    val allLevels: List<Level> = emptyList(),
    val highestUnlockedLevel: Int = 1,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val shuffleAnimKey: Int = 0,
    val recentlySolvedWord: String? = null,
    val celebrationTimestamp: Long = 0L
) {
    val totalWordsCount: Int get() = currentLevel?.grid?.words?.size ?: 0
    val solvedWordsCount: Int get() = revealedWords.size
    val isAllSolved: Boolean get() = totalWordsCount > 0 && solvedWordsCount >= totalWordsCount
}

sealed interface GameUiEffect {
    data object VibrateLight : GameUiEffect
    data object VibrateMedium : GameUiEffect
    data object VibrateError : GameUiEffect
}
