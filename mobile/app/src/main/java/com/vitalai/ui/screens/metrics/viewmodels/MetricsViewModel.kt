package com.vitalai.ui.screens.metrics.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val updateSheetTab: Int = 0,  // 0=Basic, 1=Advanced, 2=Photo
    val isUploadingPhoto: Boolean = false
)

@HiltViewModel
class MetricsViewModel @Inject constructor(
    private val bodyMetricsRepository: BodyMetricsRepository,
    private val userRepository: UserRepository
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
            val summaryDeferred = async { bodyMetricsRepository.getSummary() }
            val periodDeferred = async { bodyMetricsRepository.getPeriod(period) }
            val healthProfileDeferred = async { userRepository.getHealthProfile() }
            val photosDeferred = async { bodyMetricsRepository.getPhotos(10) }
            _uiState.update {
                it.copy(
                    latest = latestDeferred.await().getOrNull(),
                    summary = summaryDeferred.await().getOrNull(),
                    periodData = periodDeferred.await().getOrNull(),
                    healthProfile = healthProfileDeferred.await().getOrNull(),
                    photos = photosDeferred.await().getOrElse { emptyList() },
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

    fun addMetric(request: UpsertBodyMetricRequest) {
        viewModelScope.launch {
            // Không bật isLoading (giữ nội dung hiện tại) -> không nháy spinner.
            bodyMetricsRepository.addMetric(request).fold(
                onSuccess = { saved ->
                    // Hiện chỉ số mới NGAY; phần dẫn xuất (summary/chart) làm mới im lặng.
                    _uiState.update {
                        it.copy(latest = saved, updateSuccessCount = it.updateSuccessCount + 1)
                    }
                    refreshDerived()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
            )
        }
    }

    /** Làm mới summary + chart trong nền (KHÔNG bật isLoading -> không giật). */
    private fun refreshDerived() {
        val period = _uiState.value.selectedPeriod
        viewModelScope.launch {
            val summary = bodyMetricsRepository.getSummary().getOrNull()
            val periodData = bodyMetricsRepository.getPeriod(period).getOrNull()
            _uiState.update {
                it.copy(
                    summary = summary ?: it.summary,
                    periodData = periodData ?: it.periodData
                )
            }
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
