package com.vitalai.ui.screens.workout.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.remote.model.BodyMetricDto
import com.vitalai.data.remote.model.BodyMetricsPeriodDto
import com.vitalai.data.remote.model.BodyMetricsSummaryDto
import com.vitalai.data.remote.model.ProgressPhotoDto
import com.vitalai.data.repository.BodyMetricsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    init {
        loadData()
    }

    fun loadData() {
        val period = _uiState.value.selectedPeriod
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val latestDeferred = async { bodyMetricsRepository.getLatest() }
            val summaryDeferred = async { bodyMetricsRepository.getSummary() }
            val periodDeferred = async { bodyMetricsRepository.getPeriod(period) }
            val photosDeferred = async { bodyMetricsRepository.getPhotos(10) }

            val latest = latestDeferred.await().getOrNull()
            val summary = summaryDeferred.await().getOrNull()
            val periodData = periodDeferred.await().getOrNull()
            val photos = photosDeferred.await().getOrElse { emptyList() }

            _uiState.update {
                it.copy(
                    latest = latest,
                    summary = summary,
                    periodData = periodData,
                    photos = photos,
                    isLoading = false,
                    error = if (latest == null) "Chưa có dữ liệu chỉ số cơ thể" else null
                )
            }
        }
    }

    fun selectPeriod(period: String) {
        if (period == _uiState.value.selectedPeriod) return
        _uiState.update { it.copy(selectedPeriod = period) }
        viewModelScope.launch {
            bodyMetricsRepository.getPeriod(period).onSuccess { data ->
                _uiState.update { it.copy(periodData = data) }
            }
        }
    }
}
