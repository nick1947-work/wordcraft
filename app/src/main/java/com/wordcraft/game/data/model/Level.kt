package com.wordcraft.game.data.model

data class GridWord(
    val word: String,
    val startRow: Int,
    val startCol: Int,
    val isHorizontal: Boolean
) {
    val length: Int get() = word.length

    fun cells(): List<Pair<Int, Int>> {
        return (0 until length).map { i ->
            if (isHorizontal) {
                Pair(startRow, startCol + i)
            } else {
                Pair(startRow + i, startCol)
            }
        }
    }
}

data class GridData(
    val rows: Int,
    val cols: Int,
    val words: List<GridWord>
)

data class Level(
    val id: Int,
    val title: String,
    val letters: List<String>,
    val grid: GridData,
    val bonusWords: List<String>
) {
    val allWords: List<String> get() = grid.words.map { it.word }
}

data class CellPosition(
    val row: Int,
    val col: Int
)

data class CellData(
    val letter: Char,
    val row: Int,
    val col: Int,
    val isRevealed: Boolean,
    val isHinted: Boolean,
    val isPartOfWord: Boolean
)
