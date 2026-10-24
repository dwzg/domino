package de.wtzg.domino.data.online

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import de.wtzg.domino.BuildConfig
import de.wtzg.domino.data.AppJson
import de.wtzg.domino.data.SettingsRepository
import de.wtzg.domino.protocol.Registration
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

/** Your online identity on one server. */
data class OnlineAccount(val serverUrl: String, val registration: Registration?)

/**
 * Online play: which server, who you are on it, and an [OnlineApi] to talk to it.
 * Identities are kept per server, so switching servers doesn't lose your games.
 */
class OnlineRepository(
    private val store: DataStore<Preferences>,
    settings: SettingsRepository,
    private val client: HttpClient,
    private val defaultServer: String = BuildConfig.SERVER_URL,
) {
    private val identitiesKey = stringPreferencesKey("online_identities")
    private val notifiedKey = stringPreferencesKey("online_notified")
    private val identities = MapSerializer(String.serializer(), Registration.serializer())
    private val notifiedMap = MapSerializer(String.serializer(), Int.serializer())

    val account: Flow<OnlineAccount> = combine(settings.settings, store.data) { s, prefs ->
        val url = KtorOnlineApi.normalize(s.serverUrl.ifBlank { defaultServer })
        val ids = prefs[identitiesKey]?.let { runCatching { AppJson.decodeFromString(identities, it) }.getOrNull() } ?: emptyMap()
        OnlineAccount(url, ids[url])
    }

    suspend fun api(): OnlineApi {
        val a = account.first()
        if (a.serverUrl.isBlank()) throw OnlineException("No server set up yet.")
        return KtorOnlineApi(client, a.serverUrl, a.registration?.token)
    }

    suspend fun register(name: String): Registration {
        val a = account.first()
        val reg = api().register(name)
        saveIdentity(a.serverUrl, reg)
        return reg
    }

    suspend fun rename(name: String) {
        val a = account.first()
        val reg = a.registration ?: return
        api().rename(name)
        saveIdentity(a.serverUrl, reg.copy(name = name.trim()))
    }

    private suspend fun saveIdentity(url: String, reg: Registration) {
        store.edit { p ->
            val ids = p[identitiesKey]?.let { runCatching { AppJson.decodeFromString(identities, it) }.getOrNull() } ?: emptyMap()
            p[identitiesKey] = AppJson.encodeToString(identities, ids + (url to reg))
        }
    }

    /** Remembers which game versions you've already been notified about. */
    suspend fun notified(): Map<String, Int> =
        store.data.map { p -> p[notifiedKey]?.let { runCatching { AppJson.decodeFromString(notifiedMap, it) }.getOrNull() } ?: emptyMap() }
            .first()

    suspend fun setNotified(map: Map<String, Int>) {
        store.edit { p -> p[notifiedKey] = AppJson.encodeToString(notifiedMap, map) }
    }
}
