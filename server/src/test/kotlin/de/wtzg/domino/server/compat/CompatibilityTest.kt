package de.wtzg.domino.server.compat

import de.wtzg.domino.protocol.GameView
import de.wtzg.domino.protocol.ProtocolJson
import de.wtzg.domino.protocol.ProtocolVersion
import de.wtzg.domino.protocol.ServerInfo
import de.wtzg.domino.server.GameRecord
import de.wtzg.domino.server.GameService
import de.wtzg.domino.server.InMemoryStore
import de.wtzg.domino.server.LiveHub
import de.wtzg.domino.server.SqliteStore
import de.wtzg.domino.server.dominoServer
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.sql.DriverManager

/**
 * Keeps old apps and old data working. For every protocol version ever released, the
 * messages it wrote must still be read by the current code, and the current code must
 * write them back with every field the old app expects, under the same names and values.
 */
class CompatibilityTest {
    private val root = File("src/test/resources/compat")

    private fun versions(): List<File> =
        root.listFiles { f -> f.isDirectory && f.name.startsWith("v") }!!.sortedBy { it.name }

    @Test
    fun referenceFilesExistForEveryReleasedProtocol() {
        if (System.getenv("WRITE_COMPAT_FIXTURES") == "1" && !File(root, "v${ProtocolVersion.CURRENT}").exists()) {
            writeCompatFixtures(root)
        }
        val have = versions().map { it.name.removePrefix("v").toInt() }
        assertEquals("Reference files for protocol 1..${ProtocolVersion.CURRENT}", (1..ProtocolVersion.CURRENT).toList(), have)
        versions().forEach { dir ->
            COMPAT_FILES.keys.forEach { name -> assertTrue("${dir.name}/$name missing", File(dir, name).exists()) }
        }
    }

    @Test
    fun oldMessagesStillReadAndWriteBackTheSame() {
        for (dir in versions()) for ((name, serializer) in COMPAT_FILES) {
            val text = File(dir, name).readText()
            val old = ProtocolJson.parseToJsonElement(text)
            @Suppress("UNCHECKED_CAST")
            val s = serializer as KSerializer<Any?>
            val decoded = try {
                ProtocolJson.decodeFromString(s, text)
            } catch (e: Exception) {
                throw AssertionError("${dir.name}/$name no longer loads: ${e.message}", e)
            }
            val again = ProtocolJson.encodeToJsonElement(s, decoded)
            assertCovers(old, again, "${dir.name}/$name")
        }
    }

    /** Everything in [old] is still in [new], with the same names and values. New fields are fine. */
    private fun assertCovers(old: JsonElement, new: JsonElement, path: String) {
        when (old) {
            is JsonObject -> {
                if (new !is JsonObject) fail("$path: was an object, now $new")
                new as JsonObject
                for ((k, v) in old) {
                    val n = new[k] ?: throw AssertionError("$path.$k: field missing (old apps need it)")
                    assertCovers(v, n, "$path.$k")
                }
            }
            is JsonArray -> {
                if (new !is JsonArray) fail("$path: was a list, now $new")
                new as JsonArray
                assertEquals("$path: list length", old.size, new.size)
                old.indices.forEach { assertCovers(old[it], new[it], "$path[$it]") }
            }
            is JsonNull -> Unit // a value where there was none is fine
            is JsonPrimitive -> assertEquals("$path: value changed", old, new)
        }
    }

    @Test
    fun appsFromProtocolOneStillPlay() = testApplication {
        application { dominoServer(GameService(InMemoryStore()), LiveHub()) }
        val v1 = File(root, "v1")
        // Exactly what a first-release app sends: no protocol header.
        suspend fun send(path: String, file: String?, token: String? = null): io.ktor.client.statement.HttpResponse =
            client.post(path) {
                token?.let { header("Authorization", "Bearer $it") }
                contentType(ContentType.Application.Json)
                setBody(file?.let { File(v1, it).readText() } ?: "{}")
            }
        val reg = send("/players", "register-request.json")
        assertEquals(HttpStatusCode.Created, reg.status)
        val token = ProtocolJson.parseToJsonElement(reg.bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content
        val created = send("/games", "create-game-request.json", token)
        assertEquals(HttpStatusCode.Created, created.status)
        val view = ProtocolJson.decodeFromString(GameView.serializer(), created.bodyAsText())
        assertEquals(HttpStatusCode.OK, send("/games/${view.id}/start", "start-request.json", token).status)
        val games = client.get("/games") { header("Authorization", "Bearer $token") }
        assertEquals(HttpStatusCode.OK, games.status)
        assertTrue(games.bodyAsText().contains(view.id))
        val leave = File(v1, "action-leave.json").readText()
        val left = client.post("/games/${view.id}/actions") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(leave)
        }
        assertTrue(left.status == HttpStatusCode.OK || left.status == HttpStatusCode.NoContent)
    }

    @Test
    fun serverSaysWhichProtocolsItSpeaks() = testApplication {
        application { dominoServer(GameService(InMemoryStore()), LiveHub()) }
        val info = ProtocolJson.decodeFromString(ServerInfo.serializer(), client.get("/version").bodyAsText())
        assertEquals(ProtocolVersion.CURRENT, info.protocol)
        assertEquals(ProtocolVersion.MIN_SUPPORTED, info.minProtocol)
        // Too old: a clear message instead of confusing errors.
        if (ProtocolVersion.MIN_SUPPORTED > 1) {
            val r = client.get("/games") { header(ProtocolVersion.HEADER, (ProtocolVersion.MIN_SUPPORTED - 1).toString()) }
            assertEquals(HttpStatusCode.UpgradeRequired, r.status)
        }
    }

    @Test
    fun databasesFromTheFirstReleaseStillOpen() {
        val file = kotlin.io.path.createTempFile("domino-v1", ".db").toFile()
        try {
            // A database as the first release created it: no schema version, one game.
            val record = File(root, "v1/game-record.json").readText()
            DriverManager.getConnection("jdbc:sqlite:${file.path}").use { c ->
                c.createStatement().use { st ->
                    st.executeUpdate("CREATE TABLE players (id TEXT PRIMARY KEY, name TEXT NOT NULL, token_hash TEXT NOT NULL UNIQUE, created_at INTEGER NOT NULL)")
                    st.executeUpdate("CREATE TABLE games (id TEXT PRIMARY KEY, code TEXT NOT NULL UNIQUE, data TEXT NOT NULL, updated_at INTEGER NOT NULL)")
                    st.executeUpdate("CREATE TABLE game_players (game_id TEXT NOT NULL, player_id TEXT NOT NULL, PRIMARY KEY (game_id, player_id))")
                }
                val g = ProtocolJson.decodeFromString(GameRecord.serializer(), record)
                c.prepareStatement("INSERT INTO games VALUES (?, ?, ?, ?)").use {
                    it.setString(1, g.id); it.setString(2, g.code); it.setString(3, record); it.setLong(4, g.updatedAt)
                    it.executeUpdate()
                }
            }
            val store = SqliteStore(file.path)
            val g = ProtocolJson.decodeFromString(GameRecord.serializer(), record)
            assertEquals(g, store.game(g.id))
            // Opening it again (already migrated) is fine too.
            assertEquals(g, SqliteStore(file.path).gameByCode(g.code))
        } finally {
            file.delete()
        }
    }
}
