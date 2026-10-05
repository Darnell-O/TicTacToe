package com.example.tic_tac_toe

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class Scores(
    val xWins: Int = 0,
    val oWins: Int = 0,
    val draws: Int = 0
) {
    fun record(status: Status): Scores = when (status) {
        is Status.Win -> when (status.player) {
            Player.X -> copy(xWins = xWins + 1)
            Player.O -> copy(oWins = oWins + 1)
        }

        Status.Draw -> copy(draws = draws + 1)
        Status.InProgress -> this
    }
}


data class GameUiState(
    val board: List<Player?> = List(9) { null },
    val currentPlayer: Player = Player.X,
    val status: Status = Status.InProgress,
    val scores: Scores = Scores()
) {
    val isOver: Boolean
        get() = status != Status.InProgress
}


class GameViewModel : ViewModel() {

    private val game = Game()

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    fun onCellTapped(index: Int) {
        if (!game.makeMove(index)) return

        _uiState.update { state ->
            state.copy(
                board = game.board,
                currentPlayer = game.currentPlayer,
                status = game.status,
                scores = state.scores.record(game.status)
            )
        }
    }

    fun onRestart() {
        game.reset()
        _uiState.update { state ->
            GameUiState(
                board = game.board,
                currentPlayer = game.currentPlayer,
                status = game.status,
                scores = state.scores
            )
        }
    }
}



