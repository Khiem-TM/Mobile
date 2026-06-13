package com.vitalai.ui.screens.metrics.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vitalai.navigation.Screen
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.screens.metrics.calculateBmi
import com.vitalai.ui.screens.metrics.components.*
import com.vitalai.ui.screens.metrics.viewmodels.MetricsViewModel
import com.vitalai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricsScreen(
    navController: NavController,
    viewModel: MetricsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showUpdateSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.updateSuccessCount) {
        if (uiState.updateSuccessCount > 0) {
            Toast.makeText(context, "Cập nhật thông tin thành công", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            com.vitalai.ui.components.VitalScreenHeader(
                title = "Hồ sơ cơ thể",
                onBackClick = { navController.popBackStack() }
            )
        },
        containerColor = Color.White
    ) { padding ->
        when {
            uiState.isLoading -> LoadingState(modifier = Modifier.padding(padding))
            uiState.error != null && uiState.latest == null -> ErrorState(
                message = uiState.error!!,
                onRetry = viewModel::loadData,
                modifier = Modifier.padding(padding)
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 16.dp,
                    bottom = padding.calculateBottomPadding() + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Current weight + weight progress
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CurrentWeightCard(
                            latest = uiState.latest,
                            profile = uiState.healthProfile,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onLogWeight = {
                                viewModel.setUpdateTab(0)
                                showUpdateSheet = true
                            }
                        )
                        uiState.summary?.let { summary ->
                            WeightChangeRow(
                                weightChange = summary.weightChange,
                                latestDate = summary.latestDate,
                                startWeight = summary.startWeight,
                                currentWeight = summary.currentWeight,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                onClick = { navController.navigate(Screen.MetricsHistory) }
                            )
                        } ?: WeightProgressEmptyCard(
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate(Screen.MetricsHistory) }
                        )
                    }
                }

                // 2. Period chart section
                item {
                    PeriodChartSection(
                        selectedPeriod = uiState.selectedPeriod,
                        periodData = uiState.periodData,
                        onSelectPeriod = viewModel::selectPeriod,
                        earliestDate = uiState.earliestDate,
                        onExpandWeekRange = viewModel::expandWeekRange
                    )
                }

                // 3. BMI progress bar
                val profileBmi = calculateBmi(uiState.latest?.weightKg, uiState.healthProfile?.heightCm)
                profileBmi?.let { bmi ->
                    item { BmiBar(bmi = bmi) }
                }

                // 4. Personal information
                item {
                    PersonalInfoCard(
                        profile = uiState.healthProfile,
                        onClick = {
                            viewModel.setUpdateTab(3)
                            showUpdateSheet = true
                        }
                    )
                }

                // 5. Energy basics
                val currentWeight = uiState.latest?.weightKg ?: uiState.healthProfile?.initialWeightKg
                val height = uiState.healthProfile?.heightCm
                val gender = uiState.healthProfile?.gender
                val age = com.vitalai.ui.screens.metrics.calculateAge(uiState.healthProfile?.birthDate)
                val activityLevel = uiState.healthProfile?.activityLevel ?: "moderately_active"
                
                val calculatedBmr = com.vitalai.ui.screens.metrics.calculateBmr(gender, currentWeight, height, age)
                val calculatedTdee = com.vitalai.ui.screens.metrics.calculateTdee(calculatedBmr, activityLevel)
                
                if (calculatedBmr != null && calculatedTdee != null) {
                    item { EnergyBasicsCard(bmr = calculatedBmr, tdee = calculatedTdee) }
                }

                // 6. Body measurements card
                item {
                    BodyMeasurementsCard(
                        latest = uiState.latestMeasurements,
                        onClick = {
                            viewModel.setUpdateTab(1)
                            showUpdateSheet = true
                        }
                    )
                }

                // 7. Progress photos section
                item {
                    ProgressPhotosSection(
                        photos = uiState.photos,
                        isUploading = uiState.isUploadingPhoto,
                        onAddPhotoClick = {
                            viewModel.setUpdateTab(2)
                            showUpdateSheet = true
                        },
                        onDeletePhoto = { id: String -> viewModel.deletePhoto(id) }
                    )
                }

                // Empty state
                if (uiState.latest == null && !uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📊", fontSize = 40.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Chưa có dữ liệu chỉ số cơ thể",
                                    color = Ink500,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Nhấn 'Cập nhật' để thêm chỉ số đầu tiên",
                                    color = Ink400,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showUpdateSheet) {
        UpdateBottomSheet(
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = { showUpdateSheet = false }
        )
    }
}
