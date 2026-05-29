package com.vitalai.ui.screens.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.remote.model.BodyMetricDto
import com.vitalai.data.remote.model.ProgressPhotoDto
import com.vitalai.data.repository.BodyMetricsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

enum class MetricEventType { WEIGHT, PHOTO, MEASUREMENT, WORKOUT, BADGE }

data class MetricTimelineEvent(
    val id: String,
    val type: MetricEventType,
    val title: String,
    val value: String,
    val note: String? = null,
    val photoUrl: String? = null,
    val date: String,
    val monthGroup: String,
    // Raw metric fields for detail dialog (null for PHOTO events)
    val rawMetric: com.vitalai.data.remote.model.BodyMetricDto? = null
)

data class MetricsHistoryUiState(
    val events: List<MetricTimelineEvent> = emptyList(),
    val photos: List<ProgressPhotoDto> = emptyList(),
    val currentWeightKg: Float = 0f,
    val delta90Days: Float = 0f,
    val totalEvents: Int = 0,
    val weightChange: Float = 0f,
    val totalRecords: Int = 0,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val error: String? = null,
    val selectedTab: Int = 1
)

@HiltViewModel
class MetricsHistoryViewModel @Inject constructor(
    private val bodyMetricsRepository: BodyMetricsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MetricsHistoryUiState())
    val uiState = _uiState.asStateFlow()

    init { loadInitial() }

    private fun loadInitial() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val photosResult = bodyMetricsRepository.getPhotos(10)
            bodyMetricsRepository.getPeriod("3months").fold(
                onSuccess = { period ->
                    val sorted = period.data.sortedBy { it.date }
                    val latest = sorted.lastOrNull()
                    val first = sorted.firstOrNull()
                    val weightChange = if (latest != null && first != null) latest.weightKg - first.weightKg else 0f
                    _uiState.update {
                        it.copy(
                            events = sorted.asReversed().map { metric -> metric.toTimelineEvent() },
                            photos = photosResult.getOrElse { emptyList() },
                            currentWeightKg = latest?.weightKg ?: 0f,
                            delta90Days = weightChange,
                            weightChange = weightChange,
                            totalRecords = sorted.size,
                            isLoading = false,
                            totalEvents = sorted.size,
                            hasMore = false,
                            error = null
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            events = emptyList(),
                            isLoading = false,
                            totalEvents = 0,
                            hasMore = false,
                            error = e.message ?: "Không tải được lịch sử số liệu"
                        )
                    }
                }
            )
        }
    }

    fun loadMore() {
        _uiState.update { it.copy(isLoadingMore = false, hasMore = false) }
    }

    fun setTab(tab: Int) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    private fun BodyMetricDto.toTimelineEvent(): MetricTimelineEvent {
        val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull()
        val displayDate = parsedDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("vi"))) ?: date
        val month = parsedDate?.format(DateTimeFormatter.ofPattern("'Tháng' M/yyyy", Locale("vi"))) ?: "Số liệu"

        // Determine data level: ADVANCED if any body composition field is present
        val level = when {
            bodyFatPct != null || waistCm != null || hipCm != null || armCm != null ->
                MetricEventType.MEASUREMENT  // ADVANCED
            else -> MetricEventType.WEIGHT   // BASIC
        }

        val details = listOfNotNull(
            bmi?.let { "BMI %.1f".format(it) },
            bodyFatPct?.let { "Mỡ %.1f%%".format(it) },
            waistCm?.let { "Eo %.1f cm".format(it) },
            hipCm?.let { "Hông %.1f cm".format(it) },
            notes
        ).joinToString(" · ").ifBlank { null }

        val title = when (level) {
            MetricEventType.MEASUREMENT -> "Đo lường nâng cao"
            else -> "Cập nhật cân nặng"
        }

        return MetricTimelineEvent(
            id = id,
            type = level,
            title = title,
            value = "%.1f kg".format(weightKg),
            note = details,
            photoUrl = null,
            date = displayDate,
            monthGroup = month,
            rawMetric = this
        )
    }
}
