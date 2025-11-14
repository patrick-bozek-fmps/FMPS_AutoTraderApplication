package com.fmps.autotrader.desktop.patterns

import com.fmps.autotrader.desktop.mvvm.ViewEvent
import com.fmps.autotrader.desktop.services.PatternDetail
import com.fmps.autotrader.desktop.services.PatternPerformancePoint
import com.fmps.autotrader.desktop.services.PatternPerformanceStatus
import com.fmps.autotrader.desktop.services.PatternSummary

data class PatternAnalyticsState(
    val isLoading: Boolean = true,
    val patterns: List<PatternSummary> = emptyList(),
    val filteredPatterns: List<PatternSummary> = emptyList(),
    val selectedPatternId: String? = null,
    val selectedDetail: PatternDetail? = null,
    val filters: PatternFilters = PatternFilters(),
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

data class PatternFilters(
    val search: String = "",
    val exchange: String? = null,
    val timeframe: String? = null,
    val status: PatternPerformanceStatus? = null,
    val minSuccessRate: Double = 0.0
)

sealed interface PatternAnalyticsEvent : ViewEvent {
    data class ShowMessage(val message: String, val type: MessageType) : PatternAnalyticsEvent

    enum class MessageType { INFO, SUCCESS, ERROR }
}

