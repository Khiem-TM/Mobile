package com.vitalai.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vitalai.navigation.Screen
import com.vitalai.ui.components.VitalBottomNavBar

@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = { VitalBottomNavBar(navController = navController) },
        containerColor = Color(0xFFF9FAFB)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Hồ sơ", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Color(0xFFF3F4F6), CircleShape)
                        .clickable { /* settings */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.Black, modifier = Modifier.size(22.dp))
                }
            }

            // User Info Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    // Avatar
                    val avatarUrl = uiState.user?.avatarUrl
                    if (avatarUrl != null) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE5E7EB)),
                            contentAlignment = Alignment.Center
                        ) {
                            val initial = (uiState.user?.displayName?.firstOrNull() ?: 'U').uppercaseChar()
                            Text(initial.toString(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = uiState.user?.displayName ?: "Người dùng",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = uiState.user?.email ?: "email@vital.app",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // PRO badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100))
                                    .background(Color(0xFF0F172A))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("PRO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            // Goal tag
                            val goalText = when (uiState.healthProfile?.goalType?.lowercase()) {
                                "lose_weight" -> "Giảm cân"
                                "gain_weight" -> "Tăng cân"
                                "maintain" -> "Giữ cân"
                                else -> "Mục tiêu"
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100))
                                    .background(Color(0xFFFEF3C7))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🎯", fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(goalText, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFFB45309))
                            }
                            // Streak tag
                            val streak = uiState.streaks?.loginStreak ?: 0
                            if (streak > 0) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(100))
                                        .background(Color(0xFFECFDF5))
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🔥", fontSize = 11.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("$streak day streak", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF047857))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val currentWeight = uiState.healthProfile?.initialWeightKg
                val targetWeight = uiState.healthProfile?.targetWeightKg
                val heightCm = uiState.healthProfile?.heightCm
                val bmi = if (currentWeight != null && heightCm != null && heightCm > 0) {
                    val heightM = heightCm / 100f
                    currentWeight / (heightM * heightM)
                } else null

                StatCard(
                    value = currentWeight?.let { String.format("%.1f", it) } ?: "--",
                    unit = "kg",
                    label = "Hiện tại",
                    color = Color(0xFF38C182),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    value = targetWeight?.let { String.format("%.0f", it) } ?: "--",
                    unit = "kg",
                    label = "Mục tiêu",
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    value = bmi?.let { String.format("%.1f", it) } ?: "--",
                    unit = "",
                    label = "BMI",
                    color = Color(0xFF38C182),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Menu Section 1
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF3F4F6))
            ) {
                Column {
                    ProfileMenuItem(icon = Icons.Default.BarChart, label = "Chỉ số cơ thể") {
                        navController.navigate(Screen.Metrics)
                    }
                    ProfileMenuItem(icon = Icons.Default.FitnessCenter, label = "Luyện tập") {
                        navController.navigate(Screen.Workout)
                    }
                    ProfileMenuItem(icon = Icons.Default.TrackChanges, label = "Mục tiêu & kế hoạch") {
                        // navigate to goals
                    }
                    ProfileMenuItem(icon = Icons.Default.EmojiEvents, label = "Huy hiệu của tôi") {
                        // navigate to badges
                    }
                    ProfileMenuItem(icon = Icons.Default.FavoriteBorder, label = "Món của tôi (My Foods)") {
                        navController.navigate(Screen.CreateFood)
                    }
                    ProfileMenuItem(icon = Icons.Default.Article, label = "Bài viết của tôi", showDivider = false) {
                        navController.navigate(Screen.MyBlogs)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section Title
            Text(
                "KHÁM PHÁ THÊM",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            // Menu Section 2
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF3F4F6))
            ) {
                Column {
                    ProfileMenuItem(icon = Icons.Default.Language, label = "Khám phá (Blog)") {
                        navController.navigate(Screen.Discover)
                    }
                    ProfileMenuItem(icon = Icons.Default.Notifications, label = "Thông báo") {
                        navController.navigate(Screen.Notifications)
                    }
                    ProfileMenuItem(icon = Icons.Default.Settings, label = "Cài đặt", showDivider = false) {
                        // navigate to settings
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Logout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFF3F4F6), RoundedCornerShape(20.dp))
                    .clickable {
                        viewModel.logout {
                            navController.navigate(Screen.Welcome) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = Color(0xFFEF4444), modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(14.dp))
                Text("Đăng xuất", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color(0xFFEF4444))
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StatCard(value: String, unit: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF3F4F6))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(unit, fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 2.dp))
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    label: String,
    showDivider: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFF3F4F6)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color(0xFF374151), modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(text = label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.Black, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFD1D5DB), modifier = Modifier.size(20.dp))
    }
    if (showDivider) {
        HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp, modifier = Modifier.padding(start = 70.dp))
    }
}
