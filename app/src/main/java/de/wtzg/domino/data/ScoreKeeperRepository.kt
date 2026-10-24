package de.wtzg.domino.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import de.wtzg.domino.feature.scorekeeper.ScoreGame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

/** Score keeper games for real-life play: the running game, finished games and last used names. */
class ScoreKeeperRepository(private val store: DataStore<Preferences>) {
    // Same keys as version 1 so migrated data is found.
    private val currentKey = stringPreferencesKey("current_game")
    private val historyKey = stringPreferencesKey("history")
    private val playersKey = stringPreferencesKey("last_players")
    private val gameList = ListSerializer(ScoreGame.serializer())
    private val nameList = ListSerializer(String.serializer())

    val current: Flow<ScoreGame?> = store.data.map { p ->
        p[currentKey]?.let { runCatching { AppJson.decodeFromString(ScoreGame.serializer(), it) }.getOrNull() }
    }
    val history: Flow<List<ScoreGame>> = store.data.map { p ->
        p[historyKey]?.let { runCatching { AppJson.decodeFromString(gameList, it) }.getOrNull() } ?: emptyList()
    }
    val lastPlayers: Flow<List<String>> = store.data.map { p ->
        p[playersKey]?.let { runCatching { AppJson.decodeFromString(nameList, it) }.getOrNull() } ?: emptyList()
    }

    suspend fun saveCurrent(game: ScoreGame?) = store.edit { p ->
        if (game == null) p.remove(currentKey) else p[currentKey] = AppJson.encodeToString(ScoreGame.serializer(), game)
    }

    suspend fun saveHistory(games: List<ScoreGame>) = store.edit { p -> p[historyKey] = AppJson.encodeToString(gameList, games) }

    suspend fun saveLastPlayers(names: List<String>) = store.edit { p -> p[playersKey] = AppJson.encodeToString(nameList, names) }
}
