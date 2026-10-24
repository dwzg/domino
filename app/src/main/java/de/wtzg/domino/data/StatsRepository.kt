package de.wtzg.domino.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import de.wtzg.domino.engine.GameMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

@Serializable
data class ModeStats(
    val matchesWon: Int = 0,
    val matchesLost: Int = 0,
    val handsWon: Int = 0,
    val handsLost: Int = 0,
    /** Your moves the coach rated (moves with a real choice). */
    val movesRated: Int = 0,
    /** Rated moves that were as good as the coach's pick. */
    val movesMatched: Int = 0,
    val bestMatchScore: Int = 0,
) {
    val matches: Int get() = matchesWon + matchesLost
    val accuracy: Int? get() = if (movesRated == 0) null else movesMatched * 100 / movesRated
    val winRate: Int? get() = if (matches == 0) null else matchesWon * 100 / matches
}

@Serializable
data class PuzzleStats(val solved: Int = 0, val attempted: Int = 0, val streak: Int = 0, val bestStreak: Int = 0)

@Serializable
data class Stats(
    val modes: Map<GameMode, ModeStats> = emptyMap(),
    val puzzles: PuzzleStats = PuzzleStats(),
    /** Ids of lessons you've opened. */
    val lessonsRead: Set<String> = emptySet(),
) {
    fun mode(m: GameMode): ModeStats = modes[m] ?: ModeStats()
    val total: ModeStats
        get() = modes.values.fold(ModeStats()) { a, b ->
            ModeStats(
                a.matchesWon + b.matchesWon, a.matchesLost + b.matchesLost,
                a.handsWon + b.handsWon, a.handsLost + b.handsLost,
                a.movesRated + b.movesRated, a.movesMatched + b.movesMatched,
                maxOf(a.bestMatchScore, b.bestMatchScore),
            )
        }
}

class StatsRepository(private val store: DataStore<Preferences>) {
    val stats: Flow<Stats> = store.data.map { decode(it) }

    private fun decode(prefs: Preferences): Stats =
        prefs[STATS_KEY]?.let { runCatching { AppJson.decodeFromString(Stats.serializer(), it) }.getOrNull() } ?: Stats()

    suspend fun update(transform: (Stats) -> Stats) {
        store.edit { prefs -> prefs[STATS_KEY] = AppJson.encodeToString(Stats.serializer(), transform(decode(prefs))) }
    }

    suspend fun updateMode(mode: GameMode, transform: (ModeStats) -> ModeStats) =
        update { s -> s.copy(modes = s.modes + (mode to transform(s.mode(mode)))) }

    suspend fun reset() = update { Stats(lessonsRead = it.lessonsRead) }
}
