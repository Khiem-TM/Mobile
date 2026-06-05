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
fun MealsSection(mealLogs: List<com.vitalai.data.remote.model.MealLogDto>, navController: NavController, selectedDate: String) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Bữa ăn hôm nay", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink900)
            Text(
                "Xem tất cả",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Mint500,
                modifier = Modifier.clickable { navController.navigate(Screen.Diary) }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            val breakfast = mealLogs.find { it.mealType.equals("breakfast", ignoreCase = true) }
            val lunch = mealLogs.find { it.mealType.equals("lunch", ignoreCase = true) }
            val snack = mealLogs.find { it.mealType.equals("snack", ignoreCase = true) }

            MealOverviewCard(
                mealType = "Breakfast",
                imageUrl = breakfast?.items?.firstOrNull()?.imageUrl,
                description = breakfast?.items?.takeIf { it.isNotEmpty() }?.joinToString(" · ") { it.foodName } ?: "Chưa có món ăn",
                time = "Bữa sáng",
                calories = breakfast?.totalCalories?.toInt() ?: 0,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MealOverviewCard(
                    mealType = "Lunch",
                    imageUrl = lunch?.items?.firstOrNull()?.imageUrl,
                    description = lunch?.items?.takeIf { it.isNotEmpty() }?.joinToString(" · ") { it.foodName } ?: "Chưa có món ăn",
                    time = "",
                    calories = lunch?.totalCalories?.toInt() ?: 0,
                    modifier = Modifier.weight(1f),
                    isSmall = true
                )
                MealOverviewCard(
                    mealType = "Snack",
                    imageUrl = snack?.items?.firstOrNull()?.imageUrl,
                    description = snack?.items?.takeIf { it.isNotEmpty() }?.joinToString(" · ") { it.foodName } ?: "Chưa có món ăn",
                    time = "",
                    calories = snack?.totalCalories?.toInt() ?: 0,
                    modifier = Modifier.weight(1f),
                    isSmall = true
                )
            }
        }
    }
}

@Composable
fun MealOverviewCard(
    mealType: String,
    imageUrl: String?,
    description: String,
    time: String,
    calories: Int,
    modifier: Modifier = Modifier,
    isSmall: Boolean = false
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(VitalRadius.Lg),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = BorderStroke(1.dp, AppLine)
    ) {
        Column {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = mealType,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(if (isSmall) 90.dp else 140.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isSmall) 90.dp else 140.dp)
                        .background(AppSurface2),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🍽️", fontSize = if (isSmall) 26.sp else 34.sp)
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(mealType, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink900)
                if (!isSmall) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(description, fontSize = 12.sp, color = Ink500, maxLines = 1, modifier = Modifier.weight(1f))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.clip(RoundedCornerShape(100)).background(MealTimeBg).padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("⏱️", fontSize = 10.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(time, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MealTimeText)
                            }
                            Row(
                                modifier = Modifier.clip(RoundedCornerShape(100)).background(AmberContainer).padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.LocalDrink, contentDescription = null, tint = AmberOnContainer, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("$calories", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = AmberOnContainer)
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(description, fontSize = 12.sp, color = Ink500)
                }
            }
        }
    }
}