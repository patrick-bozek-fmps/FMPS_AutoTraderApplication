package com.fmps.autotrader.core.api.websocket

import com.fmps.autotrader.core.api.module
import com.fmps.autotrader.core.api.routes.TelemetryClientDisconnectResponse
import com.fmps.autotrader.core.telemetry.TelemetryCollector
import com.fmps.autotrader.core.traders.AITraderMetrics
import com.fmps.autotrader.core.traders.AITraderState
import com.fmps.autotrader.shared.dto.ApiResponse
import com.typesafe.config.ConfigFactory
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

class TelemetryRouteTest {

    private val json = Json { ignoreUnknownKeys = true }

    @BeforeEach
    fun resetTelemetry() {
        TelemetryCollector.reset()
    }

    private suspend fun DefaultClientWebSocketSession.awaitMessage(
        timeout: Duration = Duration.ofSeconds(5),
        predicate: (TelemetryServerMessage) -> Boolean
    ): TelemetryServerMessage? {
        var result: TelemetryServerMessage? = null
        withTimeout(timeout.toMillis()) {
            while (result == null) {
                val frame = incoming.receiveCatching().getOrNull() ?: break
                if (frame is Frame.Text) {
                    val message = this@TelemetryRouteTest.json.decodeFromString(
                        TelemetryServerMessage.serializer(),
                        frame.readText()
                    )
                    println("TelemetryRouteTest received: ${message.type} / ${message.channel} replay=${message.replay}")
                    if (predicate(message)) {
                        result = message
                    }
                }
            }
        }
        return result
    }

    private fun ApplicationTestBuilder.configureTestEnvironment() {
        environment {
            val baseConfig = ConfigFactory.parseString(
                """
                security {
                  api {
                    enabled = true
                    keys = ["test-api-key"]
                  }
                }
                telemetry {
                  heartbeatIntervalSeconds = 1
                  heartbeatTimeoutSeconds = 3
                  rateLimitPerSecond = 100
                  replayLimit = 10
                }
                """.trimIndent()
            ).withFallback(ConfigFactory.load("application-test.conf"))
            config = HoconApplicationConfig(baseConfig)
        }
        application {
            module()
        }
    }

    @Test
    fun `should reject websocket connection without api key`() = testApplication {
        configureTestEnvironment()

        val client = createClient {
            install(WebSockets)
        }

        val result = runCatching {
            client.webSocket("/ws/telemetry") {
                incoming.receiveCatching()
            }
        }
        assertTrue(result.isFailure)
    }

    @Test
    fun `should receive trader status event after subscribing`() = testApplication {
        configureTestEnvironment()

        val client = createClient {
            install(WebSockets)
        }

        val clientId = "subscribe-test"

        client.webSocket("/ws/telemetry?clientId=$clientId", request = {
            header("X-API-Key", "test-api-key")
        }) {
            send(Frame.Text("""{"action":"subscribe","channels":["trader-status"],"replay":false}"""))

            awaitMessage { it.type == "welcome" }

            withTimeout(Duration.ofSeconds(5).toMillis()) {
                while (TelemetryHub.clients().none { it.id == clientId && it.subscribedChannels.contains("trader-status") }) {
                    delay(25)
                }
            }

            TelemetryCollector.publishTraderStatus(
                traderId = "42",
                name = "Test Trader",
                state = AITraderState.RUNNING,
                exchange = "BINANCE",
                symbol = "BTCUSDT",
                strategy = "TREND_FOLLOWING",
                reason = "TEST",
                metrics = AITraderMetrics.empty()
            )

            val receivedEvent = awaitMessage { it.type == "event" && it.channel == "trader-status" }

            assertEquals("event", receivedEvent?.type)
            assertEquals("trader-status", receivedEvent?.channel)
            assertTrue(receivedEvent?.replay != true)
        }
    }

    @Test
    fun `should replay cached events when requested`() = testApplication {
        configureTestEnvironment()

        TelemetryCollector.publishTraderStatus(
            traderId = "100",
            name = "Replay Trader",
            state = AITraderState.IDLE,
            exchange = "BITGET",
            symbol = "ETHUSDT",
            strategy = "MEAN_REVERSION",
            reason = "SNAPSHOT",
            metrics = AITraderMetrics.empty()
        )

        val client = createClient { install(WebSockets) }

        val clientId = "replay-test"

        client.webSocket("/ws/telemetry?channels=trader-status&replay=true&clientId=$clientId", request = {
            header("X-API-Key", "test-api-key")
        }) {
            awaitMessage { it.type == "welcome" }

            withTimeout(Duration.ofSeconds(5).toMillis()) {
                while (TelemetryHub.clients().none { it.id == clientId && it.subscribedChannels.contains("trader-status") }) {
                    delay(25)
                }
            }

            val replayEvent = awaitMessage { it.type == "event" && it.channel == "trader-status" }

            assertEquals("event", replayEvent?.type)
            assertEquals("trader-status", replayEvent?.channel)
            assertTrue(replayEvent?.replay == true)
        }
    }

    @Test
    fun `should list active websocket clients`() = testApplication {
        configureTestEnvironment()

        val client = createClient { install(WebSockets) }
        var clientId: String? = null

        client.webSocket("/ws/telemetry?clientId=test-client", request = {
            header("X-API-Key", "test-api-key")
        }) {
            clientId = "test-client"

            awaitMessage { it.type == "welcome" }

            val response: HttpResponse = client.get("/api/v1/websocket/clients") {
                header("X-API-Key", "test-api-key")
            }
            val payload = response.bodyAsText()
            val decoded = json.decodeFromString(
                ApiResponse.serializer(ListSerializer(TelemetryClientInfo.serializer())),
                payload
            )
            val data = decoded.data

            assertTrue(data.any { element -> element.id == clientId })
        }
    }

    @Test
    fun `should allow disconnecting clients via admin endpoint`() = testApplication {
        configureTestEnvironment()

        val client = createClient { install(WebSockets) }
        val clientId = "disconnect-client"

        client.webSocket("/ws/telemetry?clientId=$clientId", request = {
            header("X-API-Key", "test-api-key")
        }) {
            delay(100)
            val response = client.delete("/api/v1/websocket/clients/$clientId") {
                header("X-API-Key", "test-api-key")
            }
            assertEquals(HttpStatusCode.OK, response.status)

            val decoded = json.decodeFromString(
                ApiResponse.serializer(TelemetryClientDisconnectResponse.serializer()),
                response.bodyAsText()
            )
            assertTrue(decoded.data.disconnected)

            withTimeout(Duration.ofSeconds(2).toMillis()) {
                while (TelemetryHub.clients().any { it.id == clientId }) {
                    delay(50)
                }
            }
        }
    }
}
