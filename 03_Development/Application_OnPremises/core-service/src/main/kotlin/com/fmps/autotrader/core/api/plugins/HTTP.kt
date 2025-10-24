package com.fmps.autotrader.core.api.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*

/**
 * Configure HTTP features (CORS, headers, etc.)
 */
fun Application.configureHTTP() {
    // Install CORS for local development and future web UI
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Accept)
        
        // Allow localhost for development
        allowHost("localhost:8080")
        allowHost("127.0.0.1:8080")
        
        // Allow credentials
        allowCredentials = true
    }
    
    // Install default headers
    install(DefaultHeaders) {
        header("X-API-Version", "1.0.0")
        header("X-Engine", "Ktor")
    }
}


