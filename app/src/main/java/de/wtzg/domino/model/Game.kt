package de.wtzg.domino.model

import org.json.JSONArray
import org.json.JSONObject

/** How a game is won. */
enum class GameMode {
    /** Highest score wins once someone reaches [Game.limit] points (Block, Draw, All Fives, ...). */
    FIRST_TO_TARGET,

    /** Lowest total wins after [Game.limit] rounds (Mexican Train, ...). */
    LOWEST_AFTER_ROUNDS,
}

data class Game(
    val id: Long,
    val players: List<String>,
    val mode: GameMode,
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
            val best = if (mode == GameMode.FIRST_TO_TARGET) t.max() else t.min()
            return t.indexOf(best).takeIf { t.count { it == best } == 1 }
        }

    /**
     * Index of the winner, or null while the game is still running.
     * A tie for first place means another round must be played.
     */
    val winnerIndex: Int?
        get() {
            val over = when (mode) {
                GameMode.FIRST_TO_TARGET -> totals.any { it >= limit }
                GameMode.LOWEST_AFTER_ROUNDS -> rounds.size >= limit
            }
            return if (over) leaderIndex else null
        }

    val isFinished: Boolean get() = winnerIndex != null

    fun addRound(scores: List<Int>): Game = copy(rounds = rounds + listOf(normalize(scores)))

    fun updateRound(index: Int, scores: List<Int>): Game =
        copy(rounds = rounds.toMutableList().also { it[index] = normalize(scores) })

    fun deleteRound(index: Int): Game =
        copy(rounds = rounds.toMutableList().also { it.removeAt(index) })

    private fun normalize(scores: List<Int>): List<Int> =
        players.indices.map { scores.getOrElse(it) { 0 } }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("players", JSONArray(players))
        put("mode", mode.name)
        put("limit", limit)
        put("rounds", JSONArray().also { arr -> rounds.forEach { arr.put(JSONArray(it)) } })
        put("startedAt", startedAt)
        finishedAt?.let { put("finishedAt", it) }
    }

    companion object {
        fun fromJson(o: JSONObject): Game {
            val players = o.getJSONArray("players").let { a -> List(a.length()) { a.getString(it) } }
            val rounds = o.getJSONArray("rounds").let { a ->
                List(a.length()) { i -> a.getJSONArray(i).let { r -> List(r.length()) { r.getInt(it) } } }
            }
            return Game(
                id = o.getLong("id"),
                players = players,
                mode = GameMode.valueOf(o.getString("mode")),
                limit = o.getInt("limit"),
                rounds = rounds,
                startedAt = o.getLong("startedAt"),
                finishedAt = if (o.has("finishedAt")) o.getLong("finishedAt") else null,
            )
        }
    }
}
