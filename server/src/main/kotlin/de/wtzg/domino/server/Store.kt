package de.wtzg.domino.server

import de.wtzg.domino.protocol.ProtocolJson
import java.sql.Connection
import java.sql.DriverManager
import java.util.concurrent.ConcurrentHashMap

/** Where players and games live. */
interface Store {
    fun addPlayer(p: PlayerRecord)
    fun player(id: String): PlayerRecord?
    fun playerByTokenHash(hash: String): PlayerRecord?
    fun renamePlayer(id: String, name: String)
    fun saveGame(g: GameRecord)
    fun game(id: String): GameRecord?
    fun gameByCode(code: String): GameRecord?
    fun gamesOf(playerId: String): List<GameRecord>
    fun deleteGame(id: String)
}

class InMemoryStore : Store {
    private val players = ConcurrentHashMap<String, PlayerRecord>()
    private val games = ConcurrentHashMap<String, GameRecord>()

    override fun addPlayer(p: PlayerRecord) { players[p.id] = p }
    override fun player(id: String) = players[id]
    override fun playerByTokenHash(hash: String) = players.values.firstOrNull { it.tokenHash == hash }
    override fun renamePlayer(id: String, name: String) { players.computeIfPresent(id) { _, p -> p.copy(name = name) } }
    override fun saveGame(g: GameRecord) { games[g.id] = g }
    override fun game(id: String) = games[id]
    override fun gameByCode(code: String) = games.values.firstOrNull { it.code == code }
    override fun gamesOf(playerId: String) = games.values.filter { playerId in it.playerIds }
    override fun deleteGame(id: String) { games.remove(id) }
}

/**
 * SQLite on disk. Games are stored as JSON next to a small index of who plays in them.
 * One connection, used under a lock: plenty for a hobby server.
 */
class SqliteStore(path: String) : Store {
    private val conn: Connection = DriverManager.getConnection("jdbc:sqlite:$path")

    init {
        conn.createStatement().use { st ->
            st.executeUpdate("PRAGMA journal_mode=WAL")
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS players (id TEXT PRIMARY KEY, name TEXT NOT NULL, " +
                    "token_hash TEXT NOT NULL UNIQUE, created_at INTEGER NOT NULL)",
            )
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS games (id TEXT PRIMARY KEY, code TEXT NOT NULL UNIQUE, " +
                    "data TEXT NOT NULL, updated_at INTEGER NOT NULL)",
            )
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS game_players (game_id TEXT NOT NULL, player_id TEXT NOT NULL, " +
                    "PRIMARY KEY (game_id, player_id))",
            )
            st.executeUpdate("CREATE INDEX IF NOT EXISTS game_players_player ON game_players(player_id)")
        }
    }

    private fun <T> locked(block: Connection.() -> T): T = synchronized(conn) { conn.block() }

    private fun readPlayer(rs: java.sql.ResultSet) =
        PlayerRecord(rs.getString("id"), rs.getString("name"), rs.getString("token_hash"), rs.getLong("created_at"))

    override fun addPlayer(p: PlayerRecord) = locked {
        prepareStatement("INSERT INTO players (id, name, token_hash, created_at) VALUES (?, ?, ?, ?)").use {
            it.setString(1, p.id); it.setString(2, p.name); it.setString(3, p.tokenHash); it.setLong(4, p.createdAt)
            it.executeUpdate()
        }
        Unit
    }

    override fun player(id: String) = locked {
        prepareStatement("SELECT * FROM players WHERE id = ?").use {
            it.setString(1, id)
            it.executeQuery().use { rs -> if (rs.next()) readPlayer(rs) else null }
        }
    }

    override fun playerByTokenHash(hash: String) = locked {
        prepareStatement("SELECT * FROM players WHERE token_hash = ?").use {
            it.setString(1, hash)
            it.executeQuery().use { rs -> if (rs.next()) readPlayer(rs) else null }
        }
    }

    override fun renamePlayer(id: String, name: String) = locked {
        prepareStatement("UPDATE players SET name = ? WHERE id = ?").use {
            it.setString(1, name); it.setString(2, id); it.executeUpdate()
        }
        Unit
    }

    override fun saveGame(g: GameRecord) = locked {
        autoCommit = false
        try {
            prepareStatement(
                "INSERT INTO games (id, code, data, updated_at) VALUES (?, ?, ?, ?) " +
                    "ON CONFLICT(id) DO UPDATE SET data = excluded.data, updated_at = excluded.updated_at",
            ).use {
                it.setString(1, g.id); it.setString(2, g.code)
                it.setString(3, ProtocolJson.encodeToString(GameRecord.serializer(), g)); it.setLong(4, g.updatedAt)
                it.executeUpdate()
            }
            prepareStatement("DELETE FROM game_players WHERE game_id = ?").use { it.setString(1, g.id); it.executeUpdate() }
            prepareStatement("INSERT OR IGNORE INTO game_players (game_id, player_id) VALUES (?, ?)").use { st ->
                g.playerIds.forEach { pid -> st.setString(1, g.id); st.setString(2, pid); st.executeUpdate() }
            }
            commit()
        } catch (e: Exception) {
            rollback()
            throw e
        } finally {
            autoCommit = true
        }
    }

    private fun decode(data: String) = ProtocolJson.decodeFromString(GameRecord.serializer(), data)

    override fun game(id: String) = locked {
        prepareStatement("SELECT data FROM games WHERE id = ?").use {
            it.setString(1, id)
            it.executeQuery().use { rs -> if (rs.next()) decode(rs.getString(1)) else null }
        }
    }

    override fun gameByCode(code: String) = locked {
        prepareStatement("SELECT data FROM games WHERE code = ?").use {
            it.setString(1, code)
            it.executeQuery().use { rs -> if (rs.next()) decode(rs.getString(1)) else null }
        }
    }

    override fun gamesOf(playerId: String) = locked {
        prepareStatement(
            "SELECT g.data FROM games g JOIN game_players gp ON gp.game_id = g.id WHERE gp.player_id = ? ORDER BY g.updated_at DESC",
        ).use {
            it.setString(1, playerId)
            it.executeQuery().use { rs -> buildList { while (rs.next()) add(decode(rs.getString(1))) } }
        }
    }

    override fun deleteGame(id: String) = locked {
        prepareStatement("DELETE FROM game_players WHERE game_id = ?").use { it.setString(1, id); it.executeUpdate() }
        prepareStatement("DELETE FROM games WHERE id = ?").use { it.setString(1, id); it.executeUpdate() }
        Unit
    }
}
