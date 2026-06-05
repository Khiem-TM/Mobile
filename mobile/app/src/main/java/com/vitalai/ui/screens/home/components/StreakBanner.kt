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
fun StreakBanner(streak: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .clip(RoundedCornerShape(VitalRadius.Lg))
            .background(Ink900)
            .padding(20.dp)
            .clickable { /* navigate to streaks */ }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🏆", fontSize = 24.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Streak hiện tại", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    Text("$streak ngày liên tục! 🔥", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Icon(Icons.Default.TrendingUp, contentDescription = "Arrow", tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}