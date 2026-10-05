package com.example.tic_tac_toe

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test

class GameViewModelTest {

    private lateinit var viewModel: GameViewModel

    private val state : GameUiState
        get() = viewModel.uiState.value

    private fun tap(vararg cells: Int) = cells.forEach(viewModel::onCellTapped)

    // X takes 0, 1, 2 (top row); O takes 3, 4.
    private fun playXWin() = tap(0, 3, 1, 4, 2)

    // X takes 0, 1, 8; O takes 3, 4, 5 (middle row).
    private fun playOWin() = tap(0, 3, 1, 4, 8, 5)

    // Fills the board with no line completed.
    private fun playDraw() = tap(0, 1, 2, 4, 3, 5, 7, 6, 8)

    @Before
    fun setUp() {
        viewModel = GameViewModel()
    }

    // --- Initial state ---

    @Test
    fun initialStateIsEmptyBoardXToMoveNoScores() {
        assertTrue(state.board.all { it == null })
        assertEquals(Player.X, state.currentPlayer)
        assertEquals(Status.InProgress, state.status)
        assertEquals(Scores(), state.scores)
        assertFalse(state.isOver)
    }

    // --- Taps ---

    @Test
    fun tapPlacesMarkAndPassesTurn() {
        tap(4)

        assertEquals(Player.X, state.board[4])
        assertEquals(Player.O, state.currentPlayer)
    }

    @Test
    fun tapOnOccupiedCellLeavesStateUnchanged() {
        tap(4)
        val before = state

        tap(4)

        assertEquals(before, state)
    }

    @Test
    fun tapsAfterGameEndsAreIgnored() {
        playXWin()
        val before = state

        tap(5, 6, 7, 8)

        assertEquals(before, state)
    }

    // --- Outcomes and scores ---

    @Test
    fun xWinIsReportedAndScored() {
        playXWin()

        assertEquals(Status.Win(Player.X, listOf(0, 1, 2)), state.status)
        assertTrue(state.isOver)
        assertEquals(Scores(xWins = 1), state.scores)
    }

    @Test
    fun oWinIsReportedAndScored() {
        playOWin()

        assertEquals(Status.Win(Player.O, listOf(3, 4, 5)), state.status)
        assertEquals(Scores(oWins = 1), state.scores)
    }

    @Test
    fun drawIsReportedAndScored() {
        playDraw()

        assertEquals(Status.Draw, state.status)
        assertEquals(Scores(draws = 1), state.scores)
    }

    @Test
    fun scoreIsNotCountedTwiceForRepeatedTapsAfterWin() {
        playXWin()
        repeat(3) { tap(8) }

        assertEquals(Scores(xWins = 1), state.scores)
    }

    // --- Restart ---

    @Test
    fun restartClearsBoardButKeepsScores() {
        playXWin()

        viewModel.onRestart()

        assertTrue(state.board.all { it == null })
        assertEquals(Player.X, state.currentPlayer)
        assertEquals(Status.InProgress, state.status)
        assertEquals(Scores(xWins = 1), state.scores)
    }

    @Test
    fun restartMidGameDoesNotChangeScores() {
        tap(0, 4)

        viewModel.onRestart()

        assertTrue(state.board.all { it == null })
        assertEquals(Scores(), state.scores)
    }

    @Test
    fun scoresAccumulateAcrossRounds() {
        playXWin()
        viewModel.onRestart()
        playOWin()
        viewModel.onRestart()
        playDraw()
        viewModel.onRestart()
        playXWin()

        assertEquals(Scores(xWins = 2, oWins = 1, draws = 1), state.scores)
    }

    @Test
    fun boardIsPlayableAgainAfterRestart() {
        playXWin()
        viewModel.onRestart()

        tap(0)

        assertEquals(Player.X, state.board[0])
        assertEquals(Player.O, state.currentPlayer)
    }
}
