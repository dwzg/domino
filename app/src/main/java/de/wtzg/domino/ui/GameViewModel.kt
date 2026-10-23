package de.wtzg.domino.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import de.wtzg.domino.data.GameRepository
import de.wtzg.domino.model.Game
import de.wtzg.domino.model.GameMode

class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GameRepository(app)

    var current by mutableStateOf(repo.loadCurrent())
        private set
    var history by mutableStateOf(repo.loadHistory())
        private set
    val lastPlayers: List<String> get() = repo.loadLastPlayers()

    fun startGame(players: List<String>, mode: GameMode, limit: Int) {
        repo.saveLastPlayers(players)
        commit(
            Game(
                id = System.currentTimeMillis(),
                players = players,
                mode = mode,
                limit = limit,
                startedAt = System.currentTimeMillis(),
            )
        )
    }

    fun rematch() {
        val g = current ?: return
        startGame(g.players, g.mode, g.limit)
    }

    fun addRound(scores: List<Int>) = current?.let { commit(it.addRound(scores)) }
    fun updateRound(index: Int, scores: List<Int>) = current?.let { commit(it.updateRound(index, scores)) }
    fun deleteRound(index: Int) = current?.let { commit(it.deleteRound(index)) }
    fun undoLastRound() = current?.let { if (it.rounds.isNotEmpty()) commit(it.deleteRound(it.rounds.lastIndex)) }

    /** Leaves the current game. Finished games stay in the history. */
    fun closeGame() {
        current = null
        repo.saveCurrent(null)
    }

    fun deleteFromHistory(id: Long) = storeHistory(history.filterNot { it.id == id })
    fun clearHistory() = storeHistory(emptyList())

    private fun commit(game: Game) {
        // Keep the history in sync: a game is listed there exactly while it is finished,
        // so undoing the winning round takes it out again.
        val updated = when {
            game.isFinished -> game.copy(finishedAt = game.finishedAt ?: System.currentTimeMillis())
            else -> game.copy(finishedAt = null)
        }
        current = updated
        repo.saveCurrent(updated)
        val others = history.filterNot { it.id == updated.id }
        storeHistory(if (updated.isFinished) listOf(updated) + others else others)
    }

    private fun storeHistory(games: List<Game>) {
        history = games.sortedByDescending { it.finishedAt ?: it.startedAt }
        repo.saveHistory(history)
    }
}
