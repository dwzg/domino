package de.wtzg.domino.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import de.wtzg.domino.engine.Difficulty
import de.wtzg.domino.engine.Match
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

/** Options for a game against the AI that aren't part of the rules. */
@Serializable
data class GameOptions(val difficulty: Difficulty = Difficulty.NORMAL, val coach: Boolean = true)

@Serializable
data class SavedGame(val match: Match, val options: GameOptions)

/** The match in progress, so you can leave the app and pick it up later. */
class SavedGameRepository(private val store: DataStore<Preferences>) {
    private val key = stringPreferencesKey("saved_game")

    val saved: Flow<SavedGame?> = store.data.map { prefs ->
        prefs[key]?.let { runCatching { AppJson.decodeFromString(SavedGame.serializer(), it) }.getOrNull() }
    }

    suspend fun load(): SavedGame? = saved.first()

    suspend fun save(game: SavedGame?) {
        store.edit { prefs ->
            if (game == null) prefs.remove(key) else prefs[key] = AppJson.encodeToString(SavedGame.serializer(), game)
        }
    }
}
