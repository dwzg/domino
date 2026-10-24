package de.wtzg.domino.server

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ConcurrentHashMap

/** Tells open connections when a game changed, and tracks who has a game open right now. */
class LiveHub {
    private val _changes = MutableSharedFlow<String>(extraBufferCapacity = 256)

    /** Ids of games that changed. Each connection re-reads the game and sends its own view. */
    val changes: SharedFlow<String> = _changes.asSharedFlow()

    private val present = ConcurrentHashMap<String, ConcurrentHashMap<String, Int>>()

    fun changed(gameId: String) {
        _changes.tryEmit(gameId)
    }

    fun online(gameId: String): Set<String> = present[gameId]?.filterValues { it > 0 }?.keys ?: emptySet()

    fun connect(gameId: String, playerId: String) {
        present.computeIfAbsent(gameId) { ConcurrentHashMap() }.merge(playerId, 1, Int::plus)
        changed(gameId)
    }

    fun disconnect(gameId: String, playerId: String) {
        present[gameId]?.computeIfPresent(playerId) { _, n -> if (n <= 1) null else n - 1 }
        changed(gameId)
    }
}
