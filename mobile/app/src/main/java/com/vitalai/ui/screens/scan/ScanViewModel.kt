package com.vitalai.ui.screens.scan

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.repository.FoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.vitalai.util.ClassificationResult
import com.vitalai.util.FoodClassifierHelper
import javax.inject.Inject

sealed interface ScanNavTarget {
    data class Detail(val foodId: String) : ScanNavTarget
    data class Create(val name: String) : ScanNavTarget
}

data class ScanUiState(
    val results: List<ClassificationResult> = emptyList(),
    val isAnalyzing: Boolean = false,
    val inferenceTimeMs: Long = 0L,
    val frozen: Boolean = false,
    val isResolving: Boolean = false,
    val navTarget: ScanNavTarget? = null
)

@HiltViewModel
class ScanViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val foodRepository: FoodRepository
) : ViewModel() {

    private val classifier by lazy { FoodClassifierHelper(context) }

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    fun classify(bitmap: Bitmap) {
        val state = _uiState.value
        if (state.frozen || state.isResolving || state.isAnalyzing) return
        _uiState.value = state.copy(isAnalyzing = true)
        viewModelScope.launch(Dispatchers.Default) {
            val start = System.currentTimeMillis()
            val results = classifier.classify(bitmap)
            val elapsed = System.currentTimeMillis() - start
            // Bỏ kết quả nếu trong lúc inference người dùng đã bấm chụp.
            val current = _uiState.value
            if (current.frozen || current.isResolving) {
                _uiState.value = current.copy(isAnalyzing = false)
                return@launch
            }
            _uiState.value = current.copy(
                results = results,
                isAnalyzing = false,
                inferenceTimeMs = elapsed
            )
        }
    }

    fun captureAndResolve() {
        val top = _uiState.value.results.firstOrNull() ?: return
        if (top.confidence < 0.5f) return
        _uiState.value = _uiState.value.copy(frozen = true, isResolving = true)
        viewModelScope.launch {
            val target = foodRepository.searchFoods(top.label, limit = 1)
                .map { page -> page.items.firstOrNull() }
                .getOrNull()
                ?.let { ScanNavTarget.Detail(it.id) }
                ?: ScanNavTarget.Create(top.label)
            _uiState.value = _uiState.value.copy(isResolving = false, navTarget = target)
        }
    }

    fun consumeNav() {
        _uiState.value = _uiState.value.copy(
            navTarget = null,
            frozen = false,
            results = emptyList()
        )
    }

    override fun onCleared() {
        classifier.close()
        super.onCleared()
    }
}
