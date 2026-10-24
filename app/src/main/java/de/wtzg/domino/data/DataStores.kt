package de.wtzg.domino.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.migrations.SharedPreferencesView
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.wtzg.domino.engine.GameMode
import kotlinx.serialization.json.Json
import androidx.datastore.preferences.SharedPreferencesMigration as PrefsMigration

/** One JSON setup for everything the app stores. Unknown keys are ignored so old data keeps loading. */
internal val AppJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/** Score keeper data. Version 1 kept the same keys in SharedPreferences "domino"; they're copied over once. */
internal val Context.scoreKeeperStore: DataStore<Preferences> by preferencesDataStore(
    name = "scorekeeper",
    produceMigrations = { context -> listOf(PrefsMigration(context, "domino")) },
)

internal val STATS_KEY = stringPreferencesKey("stats")

/** Settings, stats and the saved match. Version 1 training stats are folded into the Draw stats once. */
internal val Context.appStore: DataStore<Preferences> by preferencesDataStore(
    name = "app",
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(
                produceSharedPreferences = { context.getSharedPreferences("training", Context.MODE_PRIVATE) },
            ) { old: SharedPreferencesView, prefs: Preferences ->
                val draw = ModeStats(
                    matchesWon = old.getInt("matchesWon", 0),
                    matchesLost = old.getInt("matchesLost", 0),
                    handsWon = old.getInt("handsWon", 0),
                    handsLost = old.getInt("handsLost", 0),
                    movesRated = old.getInt("movesRated", 0),
                    movesMatched = old.getInt("movesMatched", 0),
                )
                val stats = Stats(modes = mapOf(GameMode.DRAW to draw))
                prefs.toMutablePreferences().apply { this[STATS_KEY] = AppJson.encodeToString(Stats.serializer(), stats) }
            },
        )
    },
)
