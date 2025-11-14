package com.fmps.autotrader.desktop.patterns

import com.fmps.autotrader.desktop.mvvm.BaseViewModel
import com.fmps.autotrader.desktop.mvvm.DispatcherProvider
import com.fmps.autotrader.desktop.patterns.PatternAnalyticsEvent.MessageType
import com.fmps.autotrader.desktop.services.PatternAnalyticsService
import com.fmps.autotrader.desktop.services.PatternPerformanceStatus
import kotlinx.coroutines.flow.collectLatest

class PatternAnalyticsViewModel(
    dispatcherProvider: DispatcherProvider,
    private val analyticsService: PatternAnalyticsService
) : BaseViewModel<PatternAnalyticsState, PatternAnalyticsEvent>(PatternAnalyticsState(), dispatcherProvider) {

    init {
        observePatterns()
    }

    private fun observePatterns() {
        launchIO {
            analyticsService.patternSummaries().collectLatest { summaries ->
                setState { state ->
                    val filtered = applyFilters(summaries, state.filters)
                    state.copy(
                        isLoading = false,
                        patterns = summaries,
                        filteredPatterns = filtered
                    )
                }
            }
        }
    }

    fun updateSearch(query: String) {
        updateFilters { it.copy(search = query) }
    }

    fun updateExchange(exchange: String?) {
        updateFilters { it.copy(exchange = exchange) }
    }

    fun updateTimeframe(timeframe: String?) {
        updateFilters { it.copy(timeframe = timeframe) }
    }

    fun updateStatus(status: PatternPerformanceStatus?) {
        updateFilters { it.copy(status = status) }
    }

    fun updateSuccessThreshold(threshold: Double) {
        updateFilters { it.copy(minSuccessRate = threshold) }
    }

    private fun updateFilters(transform: (PatternFilters) -> PatternFilters) {
        val newFilters = transform(state.value.filters)
        val filtered = applyFilters(state.value.patterns, newFilters)
        setState { it.copy(filters = newFilters, filteredPatterns = filtered) }
        if (state.value.selectedPatternId != null && filtered.none { it.id == state.value.selectedPatternId }) {
            setState { it.copy(selectedPatternId = null, selectedDetail = null) }
        }
    }

    fun selectPattern(patternId: String) {
        if (patternId == state.value.selectedPatternId) return
        setState { it.copy(selectedPatternId = patternId, selectedDetail = null, errorMessage = null) }
        launchIO {
            try {
                val detail = analyticsService.patternDetail(patternId)
                setState { it.copy(selectedDetail = detail) }
            } catch (ex: Exception) {
                publishEvent(PatternAnalyticsEvent.ShowMessage(ex.message ?: "Unable to load pattern detail", MessageType.ERROR))
                setState { it.copy(errorMessage = ex.message) }
            }
        }
    }

    fun refresh() {
        setState { it.copy(isRefreshing = true) }
        launchIO {
            val result = analyticsService.refresh()
            result.onSuccess {
                publishEvent(PatternAnalyticsEvent.ShowMessage("Pattern analytics refreshed", MessageType.SUCCESS))
            }.onFailure {
                publishEvent(PatternAnalyticsEvent.ShowMessage(it.message ?: "Refresh failed", MessageType.ERROR))
            }
            setState { it.copy(isRefreshing = false) }
        }
    }

    fun archiveSelected() {
        val id = state.value.selectedPatternId ?: return
        launchIO {
            val result = analyticsService.archivePattern(id)
            result.onSuccess {
                publishEvent(PatternAnalyticsEvent.ShowMessage("Pattern archived", MessageType.INFO))
            }.onFailure {
                publishEvent(PatternAnalyticsEvent.ShowMessage(it.message ?: "Archive failed", MessageType.ERROR))
            }
        }
    }

    fun deleteSelected() {
        val id = state.value.selectedPatternId ?: return
        launchIO {
            val result = analyticsService.deletePattern(id)
            result.onSuccess {
                publishEvent(PatternAnalyticsEvent.ShowMessage("Pattern deleted", MessageType.INFO))
                setState { it.copy(selectedPatternId = null, selectedDetail = null) }
            }.onFailure {
                publishEvent(PatternAnalyticsEvent.ShowMessage(it.message ?: "Delete failed", MessageType.ERROR))
            }
        }
    }

    private fun applyFilters(patterns: List<com.fmps.autotrader.desktop.services.PatternSummary>, filters: PatternFilters): List<com.fmps.autotrader.desktop.services.PatternSummary> {
        return patterns.filter { pattern ->
            val matchesSearch = filters.search.isBlank() ||
                pattern.name.contains(filters.search, ignoreCase = true) ||
                pattern.symbol.contains(filters.search, ignoreCase = true)
            val matchesExchange = filters.exchange?.let { pattern.exchange.equals(it, ignoreCase = true) } ?: true
            val matchesTimeframe = filters.timeframe?.let { pattern.timeframe == it } ?: true
            val matchesStatus = filters.status?.let { pattern.status == it } ?: true
            val matchesSuccess = pattern.successRate >= filters.minSuccessRate
            matchesSearch && matchesExchange && matchesTimeframe && matchesStatus && matchesSuccess
        }
    }
}

