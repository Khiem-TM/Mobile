package com.vitalai.ui.screens.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.remote.model.BodyMetricDto
import com.vitalai.data.remote.model.BodyMetricsPeriodDto
import com.vitalai.data.repository.BodyMetricsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MetricsUiState(
    val latest: BodyMetricDto? = null,
    val periodData: BodyMetricsPeriodDto? = null,
    val selectedPeriod: String = "1W",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MetricsViewModel @Inject constructor(
    private val bodyMetricsRepository: BodyMetricsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MetricsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        val period = _uiState.value.selectedPeriod
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val latestDeferred = async { bodyMetricsRepository.getLatest() }
            val periodDeferred = async { bodyMetricsRepository.getPeriod(period) }
            _uiState.update {
                it.copy(
                    latest = latestDeferred.await().getOrNull(),
                    periodData = periodDeferred.await().getOrNull(),
                    isLoading = false
                )
            }
        }
    }

    fun selectPeriod(period: String) {
        _uiState.update { it.copy(selectedPeriod = period) }
        viewModelScope.launch {
            bodyMetricsRepository.getPeriod(period).onSuccess { data ->
                _uiState.update { it.copy(periodData = data) }
            }
        }
    }
}
