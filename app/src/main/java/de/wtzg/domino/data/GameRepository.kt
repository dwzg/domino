package de.wtzg.domino.data

import android.content.Context
import de.wtzg.domino.model.Game
import org.json.JSONArray
import org.json.JSONObject

/** Stores the running game, finished games and the last used player names on the device. */
class GameRepository(context: Context) {
    private val prefs = context.getSharedPreferences("domino", Context.MODE_PRIVATE)

    fun loadCurrent(): Game? = runCatching {
        prefs.getString(KEY_CURRENT, null)?.let { Game.fromJson(JSONObject(it)) }
    }.getOrNull()

    fun saveCurrent(game: Game?) {
        prefs.edit().apply {
            if (game == null) remove(KEY_CURRENT) else putString(KEY_CURRENT, game.toJson().toString())
        }.apply()
    }

    fun loadHistory(): List<Game> = runCatching {
        val arr = JSONArray(prefs.getString(KEY_HISTORY, "[]"))
        List(arr.length()) { Game.fromJson(arr.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    fun saveHistory(games: List<Game>) {
        val arr = JSONArray().also { a -> games.forEach { a.put(it.toJson()) } }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    fun loadLastPlayers(): List<String> = runCatching {
        val arr = JSONArray(prefs.getString(KEY_LAST_PLAYERS, "[]"))
        List(arr.length()) { arr.getString(it) }
    }.getOrDefault(emptyList())

    fun saveLastPlayers(players: List<String>) {
        prefs.edit().putString(KEY_LAST_PLAYERS, JSONArray(players).toString()).apply()
    }

    private companion object {
        const val KEY_CURRENT = "current_game"
        const val KEY_HISTORY = "history"
        const val KEY_LAST_PLAYERS = "last_players"
    }
}
