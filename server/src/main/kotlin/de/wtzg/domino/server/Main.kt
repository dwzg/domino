package de.wtzg.domino.server

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.io.File

/**
 * Runs the server. Environment:
 * - PORT: port to listen on (default 8080)
 * - DATABASE: SQLite file (default ./domino.db); use a persistent volume in production
 */
fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val db = System.getenv("DATABASE") ?: "domino.db"
    File(db).absoluteFile.parentFile?.mkdirs()
    val service = GameService(SqliteStore(db))
    val hub = LiveHub()
    embeddedServer(Netty, port = port, host = "0.0.0.0") { dominoServer(service, hub) }.start(wait = true)
}
