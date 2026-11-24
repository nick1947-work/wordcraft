package com.wordcraft.game.data.repository

import android.content.Context
import com.wordcraft.game.data.model.CellData
import com.wordcraft.game.data.model.CellPosition
import com.wordcraft.game.data.model.GridData
import com.wordcraft.game.data.model.GridWord
import com.wordcraft.game.data.model.Level
import org.json.JSONObject
import java.io.InputStreamReader

class LevelRepository(private val context: Context) {

    private var cachedLevels: List<Level>? = null

    fun getLevels(): List<Level> {
        cachedLevels?.let { return it }

        val levels = mutableListOf<Level>()
        try {
            val jsonString = context.assets.open("levels.json").use { stream ->
                InputStreamReader(stream).readText()
            }
            val root = JSONObject(jsonString)
            val levelsArray = root.getJSONArray("levels")

            for (i in 0 until levelsArray.length()) {
                val levelObj = levelsArray.getJSONObject(i)
                val id = levelObj.getInt("id")
                val title = levelObj.getString("title")

                val lettersArray = levelObj.getJSONArray("letters")
                val letters = (0 until lettersArray.length()).map { lettersArray.getString(it) }

                val gridObj = levelObj.getJSONObject("grid")
                val rows = gridObj.getInt("rows")
                val cols = gridObj.getInt("cols")
                val wordsArray = gridObj.getJSONArray("words")

                val words = (0 until wordsArray.length()).map { wIdx ->
                    val wordObj = wordsArray.getJSONObject(wIdx)
                    GridWord(
                        word = wordObj.getString("word"),
                        startRow = wordObj.getInt("startRow"),
                        startCol = wordObj.getInt("startCol"),
                        isHorizontal = wordObj.getBoolean("isHorizontal")
                    )
                }

                val bonusWordsArray = levelObj.optJSONArray("bonusWords")
                val bonusWords = if (bonusWordsArray != null) {
                    (0 until bonusWordsArray.length()).map { bonusWordsArray.getString(it) }
                } else {
                    emptyList()
                }

                levels.add(
                    Level(
                        id = id,
                        title = title,
                        letters = letters,
                        grid = GridData(rows, cols, words),
                        bonusWords = bonusWords
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        cachedLevels = levels
        return levels
    }

    fun getLevelById(id: Int): Level? {
        return getLevels().find { it.id == id } ?: getLevels().firstOrNull()
    }

    /**
     * Constructs a 2D matrix (rows x cols) of CellData?
     * null indicates an empty, inactive space in the crossword puzzle.
     */
    fun buildGridMatrix(
        level: Level,
        revealedWords: Set<String>,
        hintedCells: Set<CellPosition>
    ): List<List<CellData?>> {
        val rows = level.grid.rows
        val cols = level.grid.cols

        val matrix = MutableList(rows) { MutableList<CellData?>(cols) { null } }

        // Populate cells from grid words
        for (gridWord in level.grid.words) {
            val isWordRevealed = revealedWords.contains(gridWord.word)
            val word = gridWord.word

            for (i in 0 until gridWord.length) {
                val r = if (gridWord.isHorizontal) gridWord.startRow else gridWord.startRow + i
                val c = if (gridWord.isHorizontal) gridWord.startCol + i else gridWord.startCol
                val char = word[i]
                val pos = CellPosition(r, c)
                val isHinted = hintedCells.contains(pos)
                val isRevealed = isWordRevealed || isHinted

                val existing = matrix[r][c]
                val revealedState = existing?.isRevealed == true || isRevealed
                val hintedState = existing?.isHinted == true || isHinted

                matrix[r][c] = CellData(
                    letter = char,
                    row = r,
                    col = c,
                    isRevealed = revealedState,
                    isHinted = hintedState,
                    isPartOfWord = true
                )
            }
        }

        return matrix
    }
}
