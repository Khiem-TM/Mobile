package com.vitalai.ui.screens.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.repository.BodyMetricsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    val monthGroup: String
)

data class MetricsHistoryUiState(
    val events: List<MetricTimelineEvent> = emptyList(),
    val currentWeightKg: Float = 72.5f,
    val delta90Days: Float = -2.3f,
    val totalEvents: Int = 0,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null,
    val selectedTab: Int = 1
)

@HiltViewModel
class MetricsHistoryViewModel @Inject constructor(
    private val bodyMetricsRepository: BodyMetricsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MetricsHistoryUiState())
    val uiState = _uiState.asStateFlow()

    private val allMockEvents = generateMockEvents()
    private var currentPage = 0
    private val pageSize = 5

    init { loadInitial() }

    private fun loadInitial() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            delay(600)
            val initial = allMockEvents.take(pageSize * 3)
            currentPage = 3
            _uiState.update {
                it.copy(
                    events = initial,
                    isLoading = false,
                    totalEvents = allMockEvents.size,
                    hasMore = currentPage * pageSize < allMockEvents.size
                )
            }
            // Also try to get real latest metrics
            bodyMetricsRepository.getLatest().fold(
                onSuccess = { metric ->
                    _uiState.update { it.copy(currentWeightKg = metric.weightKg) }
                },
                onFailure = {}
            )
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return
        _uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            delay(800)
            val start = currentPage * pageSize
            val end = minOf(start + pageSize, allMockEvents.size)
            val newItems = allMockEvents.subList(start, end)
            currentPage++
            _uiState.update { s ->
                s.copy(
                    events = s.events + newItems,
                    isLoadingMore = false,
                    hasMore = end < allMockEvents.size
                )
            }
        }
    }

    fun setTab(tab: Int) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    private fun generateMockEvents(): List<MetricTimelineEvent> {
        val events = mutableListOf<MetricTimelineEvent>()
        val months = listOf("Tháng 5/2026", "Tháng 4/2026", "Tháng 3/2026", "Tháng 2/2026", "Tháng 1/2026")
        val types = MetricEventType.entries.toList()
        var id = 1

        months.forEach { month ->
            repeat(24) { i ->
                val type = types[(id * 13 + i * 7) % types.size]
                val day = ((id * 7 + i * 3) % 28) + 1
                val dateStr = "${"%02d".format(day)}/${month.takeLast(7)}"
                events.add(
                    MetricTimelineEvent(
                        id = "${id++}",
                        type = type,
                        title = when (type) {
                            MetricEventType.WEIGHT -> "Cập nhật cân nặng"
                            MetricEventType.PHOTO -> "Ảnh tiến độ"
                            MetricEventType.MEASUREMENT -> "Đo số đo cơ thể"
                            MetricEventType.WORKOUT -> "Hoàn thành buổi tập"
                            MetricEventType.BADGE -> "Nhận huy hiệu mới"
                        },
                        value = when (type) {
                            MetricEventType.WEIGHT -> "${(700 + (id * 3) % 50) / 10f} kg"
                            MetricEventType.PHOTO -> "Tuần ${(id % 12) + 1}"
                            MetricEventType.MEASUREMENT -> "V: ${(80 + id % 10)} cm"
                            MetricEventType.WORKOUT -> "${30 + id % 60} phút"
                            MetricEventType.BADGE -> "🏆 ${listOf("Streak 7 ngày", "10kg đầu tiên", "Tháng đầu tiên")[id % 3]}"
                        },
                        note = if (id % 4 == 0) "Cảm thấy tốt hơn hôm qua" else null,
                        photoUrl = if (type == MetricEventType.PHOTO) "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?w=200" else null,
                        date = dateStr,
                        monthGroup = month
                    )
                )
            }
        }
        return events
    }
}
