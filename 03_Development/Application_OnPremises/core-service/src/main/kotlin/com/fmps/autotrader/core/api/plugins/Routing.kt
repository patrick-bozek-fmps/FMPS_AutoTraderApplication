package com.fmps.autotrader.core.api.plugins

import com.fmps.autotrader.core.api.routes.configureAITraderRoutes
import com.fmps.autotrader.core.api.routes.configureConfigurationRoutes
import com.fmps.autotrader.core.api.routes.configureHealthRoutes
import com.fmps.autotrader.core.api.routes.configurePatternRoutes
import com.fmps.autotrader.core.api.routes.configureTradeRoutes
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Configure all API routes
 */
fun Application.configureRouting() {
    routing {
        // Root endpoint
        get("/") {
            call.respondText("FMPS AutoTrader API Server - Running")
        }
        
        // Configure health/status routes
        configureHealthRoutes()
        
        // API v1 routes
        route("/api/v1") {
            // AI Trader routes
            configureAITraderRoutes()
            
            // Trade routes
            configureTradeRoutes()
            
            // Pattern routes
            configurePatternRoutes()
            
            // Configuration routes
            configureConfigurationRoutes()
        }
    }
}


