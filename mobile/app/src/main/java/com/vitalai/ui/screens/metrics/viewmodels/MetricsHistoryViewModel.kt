package com.vitalai.ui.screens.metrics.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.ui.screens.metrics.mappers.metricDateOrNull
import com.vitalai.ui.screens.metrics.mappers.toTimelineEvent
import com.vitalai.data.remote.model.BodyMetricsSummaryDto
import com.vitalai.data.remote.model.ProgressPhotoDto
import com.vitalai.data.repository.BodyMetricsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class MetricEventType { WEIGHT, PHOTO, MEASUREMENT, WORKOUT, BADGE }

data class MetricTimelineEvent(
    val id: String,
    val type: MetricEventType,
    val title: String,
    val value: String,
    val note: String? = null,
    val photoUrl: String? = null,
    val photoUrls: List<String> = emptyList(),
    val date: String,
    val monthGroup: String,
    val rawMetric: com.vitalai.data.remote.model.BodyMetricDto? = null,
    val weightDelta: Float? = null,
    val bodyFatDelta: Float? = null,
)

data class MetricsHistoryUiState(
    val events: List<MetricTimelineEvent> = emptyList(),
    val photos: List<ProgressPhotoDto> = emptyList(),
    val summary: BodyMetricsSummaryDto? = null,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val error: String? = null,
    val activeFilter: MetricEventType? = null
) {
    val filteredEvents: List<MetricTimelineEvent>
        get() = if (activeFilter == null) events else events.filter { it.type == activeFilter }
}

@HiltViewModel
class MetricsHistoryViewModel @Inject constructor(
    private val bodyMetricsRepository: BodyMetricsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MetricsHistoryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadPhotos()
        observeHistoryFromRoom()
        viewModelScope.launch { bodyMetricsRepository.refreshFromNetwork() }
    }

    private fun loadPhotos() {
        viewModelScope.launch {
            val photos = bodyMetricsRepository.getPhotos(10).getOrElse { emptyList() }
            _uiState.update { it.copy(photos = photos) }
        }
    }

    private fun observeHistoryFromRoom() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            combine(
                bodyMetricsRepository.observeHistory(),
                bodyMetricsRepository.observeSummary()
            ) { historyDtos, summary -> Pair(historyDtos, summary) }
                .collect { (historyDtos, summary) ->
                    val photosByMetric = _uiState.value.photos.groupBy { it.bodyMetricId }
                    val sorted = historyDtos.sortedByDescending { it.metricDateOrNull() ?: LocalDate.MIN }
                    val events = sorted.mapIndexed { index, dto ->
                        dto.toTimelineEvent(
                            photos = photosByMetric[dto.id].orEmpty(),
                            previousMetric = sorted.getOrNull(index + 1)
                        )
                    }
                    _uiState.update {
                        it.copy(
                            events = events,
                            summary = summary,
                            isLoading = false,
                            hasMore = false,
                            error = null
                        )
                    }
                }
        }
    }

    // Room loads all records at once — hasMore is always false, so this is never triggered from UI
    fun loadMore() = Unit

    fun setFilter(type: MetricEventType?) {
        _uiState.update { it.copy(activeFilter = type) }
    }
}
