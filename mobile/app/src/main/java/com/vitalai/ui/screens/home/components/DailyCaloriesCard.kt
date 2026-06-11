package com.vitalai.ui.screens.home.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vitalai.data.remote.model.BodyMetricDto
import com.vitalai.navigation.Screen
import com.vitalai.ui.components.ArcGauge
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.theme.AppLine
import com.vitalai.ui.theme.AppSurface
import com.vitalai.ui.theme.AppSurface2
import com.vitalai.ui.theme.AmberContainer
import com.vitalai.ui.theme.AmberOnContainer
import com.vitalai.ui.theme.AmberTint
import com.vitalai.ui.theme.Ink400
import com.vitalai.ui.theme.Ink500
import com.vitalai.ui.theme.Ink700
import com.vitalai.ui.theme.Ink900
import com.vitalai.ui.theme.MacroCarbs
import com.vitalai.ui.theme.MacroFat
import com.vitalai.ui.theme.MacroProtein
import com.vitalai.ui.theme.MealTimeBg
import com.vitalai.ui.theme.MealTimeText
import com.vitalai.ui.theme.Mint100
import com.vitalai.ui.theme.Mint500
import com.vitalai.ui.theme.WaterBlue
import com.vitalai.ui.theme.WaterBlueTint
import com.vitalai.ui.theme.VitalRadius
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.vitalai.ui.screens.home.viewmodels.HomeUiState
import com.vitalai.ui.screens.home.viewmodels.HomeViewModel

@Composable
fun DailyCaloriesCard(uiState: HomeUiState) {
    val d = uiState.dashboard
    val consumed = d?.caloriesConsumed?.toInt() ?: 0
    val goal = d?.calorieGoal ?: 2000
    val remaining = (goal - consumed).coerceAtLeast(0)
    val progress = if (goal > 0) (consumed.toFloat() / goal).coerceIn(0f, 1f) else 0f
    val isOnTrack = consumed <= goal

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        shape = RoundedCornerShape(VitalRadius.Xl),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text("Calo hôm nay", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink900)
                    Text("Mục tiêu: $goal kcal", fontSize = 13.sp, color = Ink500)
                }
                val statusContainerColor = if (isOnTrack) Mint100 else MaterialTheme.colorScheme.errorContainer
                val statusContentColor = if (isOnTrack) Mint500 else MaterialTheme.colorScheme.error
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100))
                        .background(statusContainerColor)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = statusContentColor, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isOnTrack) "Đúng kế hoạch" else "Vượt mức", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = statusContentColor)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                ArcGauge(
                    value = progress,
                    label = "%,d".format(consumed),
                    sublabel = "kcal · $remaining còn lại",
                    size = 200.dp,
                    stroke = 18.dp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val carbsG = d?.carbsG ?: 0f; val carbsGoal = (d?.carbsGoal ?: 1f).coerceAtLeast(1f)
                val proteinG = d?.proteinG ?: 0f; val proteinGoal = (d?.proteinGoal ?: 1f).coerceAtLeast(1f)
                val fatG = d?.fatG ?: 0f; val fatGoal = (d?.fatGoal ?: 1f).coerceAtLeast(1f)
                MacroBar("Carbs", "${carbsG.toInt()}/${carbsGoal.toInt()}g", carbsG / carbsGoal, MacroCarbs)
                MacroBar("Protein", "${proteinG.toInt()}/${proteinGoal.toInt()}g", proteinG / proteinGoal, MacroProtein)
                MacroBar("Fat", "${fatG.toInt()}/${fatGoal.toInt()}g", fatG / fatGoal, MacroFat)
            }
        }
    }
}

@Composable
fun MacroBar(label: String, value: String, progress: Float, color: Color) {
    Column(modifier = Modifier.width(85.dp)) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink900)
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(100)).background(AppSurface2)
        ) {
            Box(modifier = Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(100)).background(color))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink900)
    }
}