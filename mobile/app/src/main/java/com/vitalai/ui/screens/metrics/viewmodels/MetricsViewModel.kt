package com.vitalai.ui.screens.metrics.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.mapper.toDto
import com.vitalai.data.remote.model.BodyMetricDto
import com.vitalai.data.remote.model.BodyMetricsPeriodDto
import com.vitalai.data.remote.model.BodyMetricsSummaryDto
import com.vitalai.data.remote.model.HealthProfileDto
import com.vitalai.data.remote.model.ProgressPhotoDto
import com.vitalai.data.remote.model.UpsertBodyMetricRequest
import com.vitalai.data.repository.BodyMetricsRepository
import com.vitalai.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MetricsUiState(
    val latest: BodyMetricDto? = null,
    val periodData: BodyMetricsPeriodDto? = null,
    val summary: BodyMetricsSummaryDto? = null,
    val healthProfile: HealthProfileDto? = null,
    val selectedPeriod: String = "week",
    val isLoading: Boolean = false,
    val error: String? = null,
    val updateSuccessCount: Int = 0,
    val photos: List<ProgressPhotoDto> = emptyList(),
    val updateSheetTab: Int = 0,
    val isUploadingPhoto: Boolean = false
)

@HiltViewModel
class MetricsViewModel @Inject constructor(
    private val bodyMetricsRepository: BodyMetricsRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MetricsUiState())
    val uiState = _uiState.asStateFlow()

    private val _selectedPeriod = MutableStateFlow("week")

    init {
        observeRoomFlows()
        loadData()
    }

    private fun observeRoomFlows() {
        viewModelScope.launch {
            bodyMetricsRepository.observeLatest().collect { entity ->
                _uiState.update { it.copy(latest = entity?.toDto()) }
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
        viewModelScope.launch {
            userRepository.observeHealthProfile().collect { profile ->
                _uiState.update { it.copy(healthProfile = profile) }
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val photosDeferred = async { bodyMetricsRepository.getPhotos(10) }
            bodyMetricsRepository.refreshFromNetwork()
            userRepository.refreshHealthProfile()
            _uiState.update {
                it.copy(
                    photos = photosDeferred.await().getOrElse { emptyList() },
                    isLoading = false
                )
            }
        }
    }

    fun selectPeriod(period: String) {
        _selectedPeriod.value = period
        _uiState.update { it.copy(selectedPeriod = period) }
    }

    fun addMetric(request: UpsertBodyMetricRequest) {
        viewModelScope.launch {
            bodyMetricsRepository.addMetric(request).fold(
                onSuccess = {
                    // latest/summary/period auto-update via Room flows
                    _uiState.update { it.copy(updateSuccessCount = it.updateSuccessCount + 1) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
            )
        }
    }

    fun updateHealthProfile(profile: HealthProfileDto) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            userRepository.updateHealthProfile(profile).fold(
                onSuccess = { updated ->
                    _uiState.update {
                        it.copy(
                            healthProfile = updated,
                            updateSuccessCount = it.updateSuccessCount + 1
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
            )
        }
    }

    fun loadPhotos() {
        viewModelScope.launch {
            bodyMetricsRepository.getPhotos(10).onSuccess { photos ->
                _uiState.update { it.copy(photos = photos) }
            }
        }
    }

    fun deletePhoto(id: String) {
        viewModelScope.launch {
            bodyMetricsRepository.deletePhoto(id).onSuccess {
                _uiState.update { state ->
                    state.copy(photos = state.photos.filter { it.id != id })
                }
            }
        }
    }

    fun setUpdateTab(tab: Int) {
        _uiState.update { it.copy(updateSheetTab = tab) }
    }

    fun uploadPhoto(file: java.io.File, photoType: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingPhoto = true) }
            val metricId = _uiState.value.latest?.id
            bodyMetricsRepository.uploadPhoto(file, photoType, metricId).fold(
                onSuccess = { photo ->
                    _uiState.update { state ->
                        state.copy(
                            photos = state.photos + photo,
                            isUploadingPhoto = false
                        )
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isUploadingPhoto = false) }
                }
            )
        }
    }
}
