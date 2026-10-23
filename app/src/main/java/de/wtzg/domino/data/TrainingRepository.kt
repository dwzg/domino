package de.wtzg.domino.data

import android.content.Context

data class TrainingStats(
    val movesRated: Int = 0,
    val movesMatched: Int = 0,
    val handsWon: Int = 0,
    val handsLost: Int = 0,
    val matchesWon: Int = 0,
    val matchesLost: Int = 0,
) {
    /** Share of your rated moves that matched the coach, 0..100, or null if none yet. */
    val accuracy: Int? get() = if (movesRated == 0) null else movesMatched * 100 / movesRated
}

class TrainingRepository(context: Context) {
    private val prefs = context.getSharedPreferences("training", Context.MODE_PRIVATE)

    fun load() = TrainingStats(
        movesRated = prefs.getInt("movesRated", 0),
        movesMatched = prefs.getInt("movesMatched", 0),
        handsWon = prefs.getInt("handsWon", 0),
        handsLost = prefs.getInt("handsLost", 0),
        matchesWon = prefs.getInt("matchesWon", 0),
        matchesLost = prefs.getInt("matchesLost", 0),
    )

    fun save(s: TrainingStats) {
        prefs.edit()
            .putInt("movesRated", s.movesRated)
            .putInt("movesMatched", s.movesMatched)
            .putInt("handsWon", s.handsWon)
            .putInt("handsLost", s.handsLost)
            .putInt("matchesWon", s.matchesWon)
            .putInt("matchesLost", s.matchesLost)
            .apply()
    }
}
