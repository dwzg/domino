package de.wtzg.domino.feature.online

import de.wtzg.domino.data.online.KtorOnlineApi
import de.wtzg.domino.data.online.onlineHttpClient
import de.wtzg.domino.protocol.ProtocolVersion
import de.wtzg.domino.protocol.ServerInfo
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CompatibilityTest {
    @Test
    fun saysWhoShouldUpdate() {
        val now = ProtocolVersion.CURRENT
        assertNull(compatibilityProblem(ServerInfo("2.1.0", now, 1)))
        assertNotNull("app too old", compatibilityProblem(ServerInfo("9.0.0", now + 1, now + 1)))
        assertNull("newer server that still accepts this app", compatibilityProblem(ServerInfo("3.0.0", now + 1, now)))
    }

    @Test
    fun serversFromBeforeVersionInfoCountAsProtocolOne() = runBlocking {
        var sentProtocol: String? = null
        val engine = MockEngine { request ->
            sentProtocol = request.headers[ProtocolVersion.HEADER]
            respond("", HttpStatusCode.NotFound)
        }
        val info = KtorOnlineApi(onlineHttpClient(engine, appVersion = "2.1.0"), "https://old.example", token = null).serverInfo()
        assertEquals(ServerInfo("unknown", 1, 1), info)
        assertEquals(ProtocolVersion.CURRENT.toString(), sentProtocol)
    }
}
