package com.fmps.autotrader.core.api

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ApiSecurityTest {

    @Test
    fun `should reject requests without api key`() = testApplication {
        environment {
            config = MapApplicationConfig().apply {
                put("security.api.enabled", "true")
                put("security.api.header", "X-API-Key")
                put("security.api.queryParam", "apiKey")
                put("security.api.key", "dev-api-key")
                put("security.api.excludedPaths.0", "/api/health")
                put("security.api.excludedPaths.1", "/api/status")
                put("security.api.excludedPaths.2", "/api/version")
            }
        }
        application {
            module()
        }

        val response: HttpResponse = client.get("/api/v1/config")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `should allow requests with valid api key`() = testApplication {
        environment {
            config = MapApplicationConfig().apply {
                put("security.api.enabled", "true")
                put("security.api.header", "X-API-Key")
                put("security.api.queryParam", "apiKey")
                put("security.api.key", "dev-api-key")
                put("security.api.excludedPaths.0", "/api/health")
                put("security.api.excludedPaths.1", "/api/status")
                put("security.api.excludedPaths.2", "/api/version")
            }
        }
        application {
            module()
        }

        val response = client.get("/api/v1/config") {
            header("X-API-Key", "dev-api-key")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `metrics endpoint requires api key`() = testApplication {
        environment {
            config = MapApplicationConfig().apply {
                put("security.api.enabled", "true")
                put("security.api.header", "X-API-Key")
                put("security.api.queryParam", "apiKey")
                put("security.api.key", "dev-api-key")
                put("security.api.excludedPaths.0", "/api/health")
                put("security.api.excludedPaths.1", "/api/status")
                put("security.api.excludedPaths.2", "/api/version")
            }
        }
        application {
            module()
        }

        val unauthorized = client.get("/metrics")
        assertEquals(HttpStatusCode.Unauthorized, unauthorized.status)

        val authorized = client.get("/metrics") {
            header("X-API-Key", "dev-api-key")
        }

        assertEquals(HttpStatusCode.OK, authorized.status)
    }
}


