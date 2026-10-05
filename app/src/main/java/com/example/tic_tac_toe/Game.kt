package com.example.tic_tac_toe

/** The two players. X moves first by default. */
enum class Player {
    X, O;

    val opponent: Player
        get() = if (this == X) O else X
}

/** The state of a game after any number of moves. */
sealed interface Status {
    data object InProgress : Status
    data class Win(val player: Player, val line: List<Int>) : Status
    data object Draw : Status
}

/**
 * Board cells are indexed 0..8, left to right, top to bottom:
 *
 *   0 | 1 | 2
 *   3 | 4 | 5
 *   6 | 7 | 8
 */
val WIN_LINES: List<List<Int>> = listOf(
    //rows
    listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
    //columns
    listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
    //diagonals
    listOf(0, 4, 8), listOf(2, 4, 6)
)

/**
 * Evaluates a board. A win is checked before a draw, so a move that fills
 * the ninth cell and completes a line counts as a win.
 */
fun evaluate(board: List<Player?>): Status {
    require(board.size == 9) { "Board needs to be 9 spaces. Only ${board.size}" }
    for (line in WIN_LINES) {
        val first = board[line[0]] ?: continue
        if (line.all { board[it] == first }) return Status.Win(first, line)
    }
    return if (board.all { it != null }) Status.Draw else Status.InProgress
}

/**
 * A single game of Tic-Tac-Toe. Has no Android dependencies, so it can be
 * unit-tested on the JVM.
 */

class Game(firstPlayer: Player = Player.X) {

    private val cells = MutableList<Player?>(9) { null }

    /** A snapshot of the board; changing it does not affect the game. */
    val board: List<Player?>
        get() = cells.toList()

    /** Whose turn it is. Once the game ends, this stays on the last mover. */
    var currentPlayer: Player = firstPlayer
        private set

    var status: Status = Status.InProgress
        private set

    val isOver: Boolean
        get() = status != Status.InProgress

    /**
     * Places the current player's mark at [index].
     *
     * @return true if the move was applied; false if the cell is occupied
     *   or the game is already over (in which case nothing changes).
     * @throws IllegalArgumentException if [index] is outside 0..8, since that
     *   indicates a programming error rather than a user action.
     */

    fun makeMove(index: Int): Boolean {
        require(index in 0..8) { "Cell index must in 0..8, but was $index" }
        if (isOver || cells[index] != null) return false

        cells[index] = currentPlayer
        status = evaluate(cells)
        if (!isOver) currentPlayer = currentPlayer.opponent
        return true
    }

    /** Clears the board and starts a new game. */
    fun reset(firstPlayer: Player = Player.X) {
        cells.fill(null)
        currentPlayer = firstPlayer
        status = Status.InProgress
    }
}
