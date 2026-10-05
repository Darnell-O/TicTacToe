package com.example.tic_tac_toe

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Plays the given moves in order, failing the test if any move is rejected. */
private fun Game.play(vararg moves: Int) {
    for (index in moves) {
        assertTrue("Move at $index should have been accepted", makeMove(index))
    }
}

/** Runs once per winning line, so each line shows up as its own test. */
@RunWith(Parameterized::class)
class WinningLinesTest(
    @Suppress("unused") private val name: String,
    private val line: List<Int>
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun lines(): List<Array<Any>> = listOf(
            arrayOf("top row", listOf(0, 1, 2)),
            arrayOf("middle row", listOf(3, 4, 5)),
            arrayOf("bottom row", listOf(6, 7, 8)),
            arrayOf("left column", listOf(0, 3, 6)),
            arrayOf("middle column", listOf(1, 4, 7)),
            arrayOf("right column", listOf(2, 5, 8)),
            arrayOf("main diagonal", listOf(0, 4, 8)),
            arrayOf("anti-diagonal", listOf(2, 4, 6))
        )
    }

    @Test
    fun xWinsByCompletingTheLine() {
        val game = Game()
        val elseWhere = (0..8).filter { it !in line }

        // X takes the line; O plays two cells off it (too few to win).
        game.play(line[0], elseWhere[0], line[1], elseWhere[1], line[2])

        assertEquals(Status.Win(Player.X, line), game.status)
        assertTrue(game.isOver)
    }

    @Test
    fun evaluateDetectsOnTheLine() {
        val board = MutableList<Player?>(9) { null }
        line.forEach { board[it] = Player.O }

        assertEquals(Status.Win(Player.O, line), evaluate(board))
    }
}

class GameTest {

    // --- Draws and outcomes ---

    @Test
    fun fullBoardWithNoLineIsADraw() {
        val game = Game()
        // Final board:  X O X
        //               X O O
        //               O X X
        game.play(0, 1, 2, 4, 3, 5, 7, 6, 8)

        assertEquals(Status.Draw, game.status)
        assertTrue(game.isOver)
    }

    @Test
    fun winOnTheNinthMoveCountsAsWinNotDraw() {
        val game = Game()
        // Final board:  O X X
        //               X O X
        //               O O X   (X completes the right column with the last cell)
        game.play(1, 0, 2, 4, 3, 6, 5, 7, 8)

        assertEquals(Status.Win(Player.X, listOf(2, 5, 8)), game.status)
    }

    @Test
    fun nameGameIsInProgressWithEmptyBoard() {
        val game = Game()

        assertEquals(Status.InProgress, game.status)
        assertFalse(game.isOver)
        assertTrue(game.board.all { it == null })
    }

    // --- Occupied cells ---
    @Test
    fun moveOnOccupiedCellIsRejected() {
        val game = Game()
        game.play(4) // X takes the center

        assertFalse(game.makeMove(4))
        assertEquals(Player.X, game.board[4]) // still X's mark
        assertEquals(Player.O, game.currentPlayer) // still O's turn
        assertEquals(1, game.board.count { it != null })
    }

    // --- Moves after the game ends ---

    @Test
    fun movesAfterAWinAreIgnored() {
        val game = Game()
        game.play(0,3,1,4,2) // X wins the top row
        val boardAtEnd = game.board
        val statusAtEnd = game.status

        assertFalse(game.makeMove(5))
        assertEquals(boardAtEnd, game.board)
        assertEquals(statusAtEnd, game.status)
        assertEquals(Player.X, game.currentPlayer) // turn does not advance after the end
    }

    @Test
    fun movesAfterADrawAreIgnored() {
        val game = Game()
        game.play(0, 1, 2, 4, 3, 5, 7, 6, 8)

        // Every cell is full, but the rejection should come from the game being
        // over; either way nothing may change.
        for (index in 0..8) assertFalse(game.makeMove(index))
        assertEquals(Status.Draw, game.status)
    }
    // --- Turn order ---

    @Test
    fun xMovesFirstByDefault() {
        assertEquals(Player.X, Game().currentPlayer)
    }

    @Test
    fun turnsAlternateBetweenPlayers() {
        val game = Game()
        val expectedMarks = listOf(Player.X, Player.O, Player.X, Player.O)

        listOf(0, 4, 8, 2).forEachIndexed { turn, index ->
            assertEquals(expectedMarks[turn], game.currentPlayer)
            game.play(index)
            assertEquals(expectedMarks[turn], game.board[index])
        }
        assertEquals(Player.X, game.currentPlayer)
    }

    @Test
    fun rejectedMoveDoesNotPassTheTurn() {
        val game = Game()
        game.play(0)              // X
        game.makeMove(0)          // O tries an occupied cell
        game.play(1)              // O's move should still be O's

        assertEquals(Player.O, game.board[1])
    }

    @Test
    fun customFirstPlayerIsRespected() {
        val game = Game(firstPlayer = Player.O)
        game.play(0)

        assertEquals(Player.O, game.board[0])
        assertEquals(Player.X, game.currentPlayer)
    }


    // --- Reset and input validation ---

    @Test
    fun resetClearsBoardStatusAndTurn() {
        val game = Game()
        game.play(0, 3, 1, 4, 2) // X wins

        game.reset()

        assertTrue(game.board.all { it == null })
        assertEquals(Status.InProgress, game.status)
        assertEquals(Player.X, game.currentPlayer)
        assertTrue(game.makeMove(2)) // previously occupied cell is playable again
    }

    @Test(expected = IllegalArgumentException::class)
    fun outOfRangeIndexThrows() {
        Game().makeMove(9)
    }

    @Test
    fun boardSnapshotCannotModifyGame() {
        val game = Game()
        val snapshot = game.board

        game.play(0)

        assertEquals(null, snapshot[0])
        assertEquals(Player.X, game.board[0])
    }
}