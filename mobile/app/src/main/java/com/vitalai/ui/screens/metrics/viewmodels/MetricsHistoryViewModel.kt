package com.vitalai.ui.screens.metrics.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.mapper.metricDateOrNull
import com.vitalai.data.mapper.toTimelineEvent
import com.vitalai.data.remote.model.BodyMetricsSummaryDto
import com.vitalai.data.remote.model.ProgressPhotoDto
import com.vitalai.data.repository.BodyMetricsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val rawMetric: com.vitalai.data.remote.model.BodyMetricDto? = null
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

    private companion object {
        const val PAGE_SIZE = 20
    }

    private val _uiState = MutableStateFlow(MetricsHistoryUiState())
    val uiState = _uiState.asStateFlow()

    private var fromDate: String = ""
    private var toDate: String = ""
    private var currentPage: Int = 1

    init { loadInitial() }

    private fun loadInitial() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val photosDeferred = async { bodyMetricsRepository.getPhotos(10) }
            val summaryDeferred = async { bodyMetricsRepository.getSummary() }

            val summary = summaryDeferred.await().getOrNull()
            fromDate = summary?.startDate?.take(10) ?: LocalDate.now().toString()
            toDate = summary?.latestDate?.take(10) ?: LocalDate.now().toString()
            currentPage = 1

            bodyMetricsRepository.getHistory(fromDate, toDate, page = 1, limit = PAGE_SIZE).fold(
                onSuccess = { items ->
                    val photos = photosDeferred.await().getOrElse { emptyList() }
                    val photosByMetric = photos.groupBy { it.bodyMetricId }
                    val events = items
                        .sortedByDescending { it.metricDateOrNull() ?: LocalDate.MIN }
                        .map { it.toTimelineEvent(photosByMetric[it.id].orEmpty()) }
                    _uiState.update {
                        it.copy(
                            events = events,
                            photos = photos,
                            summary = summary,
                            isLoading = false,
                            hasMore = items.size == PAGE_SIZE,
                            error = null
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            events = emptyList(),
                            summary = summary,
                            isLoading = false,
                            hasMore = false,
                            error = e.message ?: "Không tải được lịch sử số liệu"
                        )
                    }
                }
            )
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return
        _uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            currentPage++
            val photosByMetric = _uiState.value.photos.groupBy { it.bodyMetricId }
            bodyMetricsRepository.getHistory(fromDate, toDate, page = currentPage, limit = PAGE_SIZE).fold(
                onSuccess = { items ->
                    val newEvents = items
                        .sortedByDescending { it.metricDateOrNull() ?: LocalDate.MIN }
                        .map { it.toTimelineEvent(photosByMetric[it.id].orEmpty()) }
                    _uiState.update {
                        it.copy(
                            events = it.events + newEvents,
                            isLoadingMore = false,
                            hasMore = items.size == PAGE_SIZE
                        )
                    }
                },
                onFailure = {
                    currentPage--
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
            )
        }
    }

    fun setFilter(type: MetricEventType?) {
        _uiState.update { it.copy(activeFilter = type) }
    }
}
