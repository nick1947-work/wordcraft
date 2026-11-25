package com.wordcraft.game.domain

import com.wordcraft.game.data.model.CellData
import com.wordcraft.game.data.model.CellPosition
import com.wordcraft.game.data.model.Level

enum class FeedbackType {
    NONE,
    VALID,
    BONUS,
    ALREADY_FOUND,
    INVALID
}

data class GameUiState(
    val currentLevel: Level? = null,
    val gridMatrix: List<List<CellData?>> = emptyList(),
    val revealedWords: Set<String> = emptySet(),
    val bonusWordsFound: Set<String> = emptySet(),
    val hintedCells: Set<CellPosition> = emptySet(),
    val wheelLetters: List<String> = emptyList(),
    val feedbackType: FeedbackType = FeedbackType.NONE,
    val feedbackText: String = "",
    val coins: Int = 200,
    val isLevelComplete: Boolean = false,
    val allLevels: List<Level> = emptyList(),
    val highestUnlockedLevel: Int = 1,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true
) {
    val totalWordsCount: Int get() = currentLevel?.grid?.words?.size ?: 0
    val solvedWordsCount: Int get() = revealedWords.size
}

sealed interface GameUiEffect {
    data object VibrateLight : GameUiEffect
    data object VibrateMedium : GameUiEffect
    data object VibrateError : GameUiEffect
}
