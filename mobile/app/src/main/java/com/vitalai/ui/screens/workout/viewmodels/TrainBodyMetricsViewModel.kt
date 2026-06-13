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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

data class TrainBodyMetricsUiState(
    val latest: BodyMetricDto? = null,
    val summary: BodyMetricsSummaryDto? = null,
    val periodData: BodyMetricsPeriodDto? = null,
    val photos: List<ProgressPhotoDto> = emptyList(),
    val selectedPeriod: String = "month",
    val isLoading: Boolean = false,
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
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
                        isInitialLoading = entity == null && it.isRefreshing,
                        isLoading = entity == null && it.isRefreshing,
                        error = if (entity == null && !it.isRefreshing) "Chưa có dữ liệu chỉ số cơ thể" else null
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
            _uiState.update {
                val initial = it.latest == null
                it.copy(
                    isRefreshing = true,
                    isInitialLoading = initial,
                    isLoading = initial,
                    error = null
                )
            }
            val photos = supervisorScope {
                val photosDeferred = async { bodyMetricsRepository.getPhotos(10).getOrElse { emptyList() } }
                launch { bodyMetricsRepository.refreshFromNetwork() }
                photosDeferred.await()
            }
            _uiState.update {
                it.copy(
                    photos = photos,
                    isRefreshing = false,
                    isInitialLoading = false,
                    isLoading = false
                )
            }
        }
    }

    fun selectPeriod(period: String) {
        if (period == _uiState.value.selectedPeriod) return
        _selectedPeriod.value = period
        _uiState.update { it.copy(selectedPeriod = period) }
    }
}
