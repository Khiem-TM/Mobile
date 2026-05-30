package com.vitalai.ui.screens.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vitalai.data.remote.model.WorkoutSessionDto
import com.vitalai.data.repository.TrainingRepository
import com.vitalai.navigation.Screen
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class WorkoutHistoryUiState(
    val sessions: List<WorkoutSessionDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class WorkoutHistoryViewModel @Inject constructor(
    private val trainingRepository: TrainingRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkoutHistoryUiState())
    val uiState = _uiState.asStateFlow()

    init { loadData() }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            trainingRepository.getSessions(limit = 60).fold(
                onSuccess = { sessions ->
                    _uiState.update {
                        it.copy(
                            sessions = sessions.sortedByDescending { session -> session.sessionDate },
                            isLoading = false
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Không tải được lịch sử tập luyện") }
                }
            )
        }
    }
}

@Composable
fun WorkoutHistoryScreen(
    navController: NavController,
    viewModel: WorkoutHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    TrainScreen {
        Column(Modifier.fillMaxSize()) {
            TrainHeader(
                title = "Lịch sử tập luyện",
                right = {
                    TrainRoundIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        onClick = { navController.popBackStack() }
                    )
                }
            )
            when {
                uiState.isLoading -> LoadingState(modifier = Modifier.fillMaxSize())
                uiState.error != null -> ErrorState(
                    message = uiState.error.orEmpty(),
                    onRetry = viewModel::loadData,
                    modifier = Modifier.fillMaxSize()
                )
                uiState.sessions.isEmpty() -> {
                    TrainCard(
                        modifier = Modifier.padding(18.dp),
                        tone = TrainTone.Keylime,
                        padding = PaddingValues(22.dp)
                    ) {
                        Text("Chưa có buổi tập nào", color = TrainColors.Forest, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.sessions, key = { it.id }) { session ->
                        WorkoutHistoryRow(
                            session = session,
                            onClick = { navController.navigate(Screen.WorkoutSession(session.id, session.sessionDate)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutHistoryRow(session: WorkoutSessionDto, onClick: () -> Unit) {
    val date = runCatching { LocalDate.parse(session.sessionDate) }.getOrNull()
    val label = date?.format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", Locale("vi")))
        ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("vi")) else it.toString() }
        ?: session.sessionDate
    TrainCard(tone = TrainTone.Cream, padding = PaddingValues(16.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = TrainColors.Forest, modifier = Modifier.size(26.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    session.name ?: "Buổi tập",
                    color = TrainColors.Ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "$label · ${session.details.size} bài · ${session.totalDurationMinutes} phút",
                    color = TrainColors.Charcoal,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(session.totalCaloriesBurned.toInt().toString(), color = TrainColors.Forest, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("kcal", color = TrainColors.Charcoal, fontSize = 10.5.sp)
            }
        }
    }
}
