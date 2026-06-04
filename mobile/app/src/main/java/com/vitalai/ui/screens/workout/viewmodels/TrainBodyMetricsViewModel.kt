package com.vitalai.ui.screens.workout.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.mapper.toDto
import com.vitalai.data.remote.model.BodyMetricDto
import com.vitalai.data.remote.model.BodyMetricsPeriodDto
import com.vitalai.data.remote.model.BodyMetricsSummaryDto
import com.vitalai.data.remote.model.ProgressPhotoDto
import com.vitalai.data.repository.BodyMetricsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrainBodyMetricsUiState(
    val latest: BodyMetricDto? = null,
    val summary: BodyMetricsSummaryDto? = null,
    val periodData: BodyMetricsPeriodDto? = null,
    val photos: List<ProgressPhotoDto> = emptyList(),
    val selectedPeriod: String = "month",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TrainBodyMetricsViewModel @Inject constructor(
    private val bodyMetricsRepository: BodyMetricsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(TrainBodyMetricsUiState())
    val uiState = _uiState.asStateFlow()

    private val _selectedPeriod = MutableStateFlow("month")

    init {
        observeRoomFlows()
        loadData()
    }

    private fun observeRoomFlows() {
        viewModelScope.launch {
            bodyMetricsRepository.observeLatest().collect { entity ->
                _uiState.update {
                    it.copy(
                        latest = entity?.toDto(),
                        error = if (entity == null && !it.isLoading) "Chưa có dữ liệu chỉ số cơ thể" else null
                    )
                }
            }
        }
        viewModelScope.launch {
            bodyMetricsRepository.observeSummary().collect { summary ->
                _uiState.update { it.copy(summary = summary) }
            }
        }
        viewModelScope.launch {
            _selectedPeriod.flatMapLatest { period ->
                bodyMetricsRepository.observePeriod(period)
            }.collect { periodData ->
                _uiState.update { it.copy(periodData = periodData) }
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val photos = bodyMetricsRepository.getPhotos(10).getOrElse { emptyList() }
            bodyMetricsRepository.refreshFromNetwork()
            _uiState.update { it.copy(photos = photos, isLoading = false) }
        }
    }

    fun selectPeriod(period: String) {
        if (period == _uiState.value.selectedPeriod) return
        _selectedPeriod.value = period
        _uiState.update { it.copy(selectedPeriod = period) }
    }
}
