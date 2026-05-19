package com.vitalai.ui.screens.scan

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScanUiState(
    val results: List<ClassificationResult> = emptyList(),
    val isAnalyzing: Boolean = false,
    val inferenceTimeMs: Long = 0L
)

@HiltViewModel
class ScanViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val classifier by lazy { FoodClassifierHelper(context) }

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    fun classify(bitmap: Bitmap) {
        if (_uiState.value.isAnalyzing) return
        _uiState.value = _uiState.value.copy(isAnalyzing = true)
        viewModelScope.launch(Dispatchers.Default) {
            val start = System.currentTimeMillis()
            val results = classifier.classify(bitmap)
            val elapsed = System.currentTimeMillis() - start
            _uiState.value = ScanUiState(
                results = results,
                isAnalyzing = false,
                inferenceTimeMs = elapsed
            )
        }
    }

    override fun onCleared() {
        classifier.close()
        super.onCleared()
    }
}
