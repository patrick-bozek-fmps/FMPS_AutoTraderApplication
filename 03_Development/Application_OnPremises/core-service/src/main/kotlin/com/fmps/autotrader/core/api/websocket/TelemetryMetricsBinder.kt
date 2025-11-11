package com.fmps.autotrader.core.api.websocket

import com.fmps.autotrader.core.telemetry.TelemetryChannel
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.prometheus.PrometheusMeterRegistry
import java.util.EnumMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Binds telemetry-specific metrics to the shared Prometheus registry.
 *
 * This binder exposes gauges and counters for active connections,
 * messages sent/dropped, and per-channel measurements so that
 * observability tooling can monitor WebSocket health.
 */
object TelemetryMetricsBinder {
    private val registered = AtomicBoolean(false)
    private val sentCounters = EnumMap<TelemetryChannel, Counter>(TelemetryChannel::class.java)
    private val droppedCounters = EnumMap<TelemetryChannel, Counter>(TelemetryChannel::class.java)

    fun register(registry: PrometheusMeterRegistry) {
        if (!registered.compareAndSet(false, true)) {
            return
        }

        Gauge.builder("autotrader.telemetry.active_connections") {
            TelemetryHub.activeConnectionCount().toDouble()
        }.register(registry)

        Gauge.builder("autotrader.telemetry.messages.sent.total") {
            TelemetryHub.messagesSentTotal().toDouble()
        }.register(registry)

        Gauge.builder("autotrader.telemetry.messages.dropped.total") {
            TelemetryHub.messagesDroppedTotal().toDouble()
        }.register(registry)

        TelemetryChannel.values().forEach { channel ->
            Gauge.builder("autotrader.telemetry.messages.sent.channel") {
                TelemetryHub.messagesSentForChannel(channel).toDouble()
            }
                .tag("channel", channel.wireName)
                .register(registry)

            Gauge.builder("autotrader.telemetry.messages.dropped.channel") {
                TelemetryHub.messagesDroppedForChannel(channel).toDouble()
            }
                .tag("channel", channel.wireName)
                .register(registry)

            sentCounters[channel] = registry.counter(
                "autotrader.telemetry.messages.sent.counter",
                "channel",
                channel.wireName
            )
            droppedCounters[channel] = registry.counter(
                "autotrader.telemetry.messages.dropped.counter",
                "channel",
                channel.wireName
            )
        }
    }

    fun recordMessage(channel: TelemetryChannel) {
        sentCounters[channel]?.increment()
    }

    fun recordDropped(channel: TelemetryChannel) {
        droppedCounters[channel]?.increment()
    }
}

