package com.wordcraft.game

import com.wordcraft.game.data.model.CellPosition
import com.wordcraft.game.data.model.GridData
import com.wordcraft.game.data.model.GridWord
import com.wordcraft.game.data.model.Level
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelValidationTest {

    @Test
    fun testWordCellCoordinates() {
        val word = GridWord(
            word = "CAT",
            startRow = 0,
            startCol = 0,
            isHorizontal = true
        )
        val cells = word.cells()
        assertEquals(3, cells.size)
        assertEquals(Pair(0, 0), cells[0])
        assertEquals(Pair(0, 1), cells[1])
        assertEquals(Pair(0, 2), cells[2])
    }

    @Test
    fun testVerticalWordCellCoordinates() {
        val word = GridWord(
            word = "ACT",
            startRow = 0,
            startCol = 1,
            isHorizontal = false
        )
        val cells = word.cells()
        assertEquals(3, cells.size)
        assertEquals(Pair(0, 1), cells[0])
        assertEquals(Pair(1, 1), cells[1])
        assertEquals(Pair(2, 1), cells[2])
    }

    @Test
    fun testCrosswordIntersection() {
        val cat = GridWord("CAT", 0, 0, isHorizontal = true)
        val act = GridWord("ACT", 0, 1, isHorizontal = false)

        val catCells = cat.cells()
        val actCells = act.cells()

        // Cell (0, 1) is shared
        val intersection = catCells.intersect(actCells.toSet())
        assertEquals(1, intersection.size)
        assertEquals(Pair(0, 1), intersection.first())

        // The letter at intersection must match:
        // In "CAT", index 1 is 'A'. In "ACT", index 0 is 'A'.
        assertEquals('A', cat.word[1])
        assertEquals('A', act.word[0])
    }

    @Test
    fun testLevelCompletionLogic() {
        val level = Level(
            id = 1,
            title = "Test Level",
            letters = listOf("C", "A", "T"),
            grid = GridData(
                rows = 3,
                cols = 3,
                words = listOf(
                    GridWord("CAT", 0, 0, true),
                    GridWord("ACT", 0, 1, false)
                )
            ),
            bonusWords = listOf("AT")
        )

        val revealedWords = mutableSetOf<String>()
        assertFalse(revealedWords.size >= level.grid.words.size)

        revealedWords.add("CAT")
        assertFalse(revealedWords.size >= level.grid.words.size)

        revealedWords.add("ACT")
        assertTrue(revealedWords.size >= level.grid.words.size)
    }

    @Test
    fun testBonusWordDetection() {
        val bonusWords = listOf("SPOT", "POTS", "TOPS", "SO")
        assertTrue(bonusWords.contains("SPOT"))
        assertTrue(bonusWords.contains("SO"))
        assertFalse(bonusWords.contains("RANDOM"))
    }
}
