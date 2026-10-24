package de.wtzg.domino.feature.scorekeeper

import kotlinx.serialization.Serializable

/** How a real-life game tracked in the score keeper is won. */
@Serializable
enum class ScoreMode {
    /** Highest score wins once someone reaches [ScoreGame.limit] points (Block, Draw, All Fives, ...). */
    FIRST_TO_TARGET,

    /** Lowest total wins after [ScoreGame.limit] rounds (Mexican Train, ...). */
    LOWEST_AFTER_ROUNDS,
}

/**
 * A real-life game in the score keeper. Field names match the version 1 JSON so saved
 * games keep loading.
 */
@Serializable
data class ScoreGame(
    val id: Long,
    val players: List<String>,
    val mode: ScoreMode,
    val limit: Int,
    val rounds: List<List<Int>> = emptyList(),
    val startedAt: Long,
    val finishedAt: Long? = null,
) {
    val totals: List<Int>
        get() = players.indices.map { p -> rounds.sumOf { it.getOrElse(p) { 0 } } }

    /** Index of the player currently in the lead, or null when tied for the lead. */
    val leaderIndex: Int?
        get() {
            if (rounds.isEmpty()) return null
            val t = totals
            val best = if (mode == ScoreMode.FIRST_TO_TARGET) t.max() else t.min()
            return t.indexOf(best).takeIf { t.count { it == best } == 1 }
        }

    /**
     * Index of the winner, or null while the game is still running.
     * A tie for first place means another round must be played.
     */
    val winnerIndex: Int?
        get() {
            val over = when (mode) {
                ScoreMode.FIRST_TO_TARGET -> totals.any { it >= limit }
                ScoreMode.LOWEST_AFTER_ROUNDS -> rounds.size >= limit
            }
            return if (over) leaderIndex else null
        }

    val isFinished: Boolean get() = winnerIndex != null

    fun addRound(scores: List<Int>): ScoreGame = copy(rounds = rounds + listOf(normalize(scores)))

    fun updateRound(index: Int, scores: List<Int>): ScoreGame =
        copy(rounds = rounds.toMutableList().also { it[index] = normalize(scores) })

    fun deleteRound(index: Int): ScoreGame =
        copy(rounds = rounds.toMutableList().also { it.removeAt(index) })

    private fun normalize(scores: List<Int>): List<Int> =
        players.indices.map { scores.getOrElse(it) { 0 } }
}
