package com.vitalai.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vitalai.navigation.Screen
import com.vitalai.ui.components.VitalBottomNavBar
import com.vitalai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cá nhân", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppSurface)
            )
        },
        bottomBar = { VitalBottomNavBar(navController = navController) },
        containerColor = AppBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar + info
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AppSurface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Mint500, Mint700))),
                        contentAlignment = Alignment.Center
                    ) {
                        val initial = uiState.displayName.firstOrNull()?.uppercaseChar() ?: 'U'
                        Text(
                            text = initial.toString(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = uiState.displayName.ifBlank { "Người dùng" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Ink900
                    )
                    Text(
                        text = uiState.email.ifBlank { "email@example.com" },
                        fontSize = 13.sp,
                        color = Ink500
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatChip(label = "Cân nặng", value = "--")
                        StatChip(label = "Mục tiêu", value = "--")
                        StatChip(label = "BMI", value = "--")
                    }
                }
            }

            // Menu sections
            ProfileMenuSection(title = "Sức khoẻ") {
                ProfileMenuItem(
                    icon = Icons.Default.BarChart,
                    label = "Số liệu cơ thể",
                    onClick = { navController.navigate(Screen.Metrics) }
                )
                ProfileMenuItem(
                    icon = Icons.Default.FitnessCenter,
                    label = "Luyện tập",
                    onClick = { navController.navigate(Screen.Workout) }
                )
            }

            ProfileMenuSection(title = "Khám phá") {
                ProfileMenuItem(
                    icon = Icons.AutoMirrored.Filled.Article,
                    label = "Bài viết & Blog",
                    onClick = { navController.navigate(Screen.Discover) }
                )
                ProfileMenuItem(
                    icon = Icons.Default.Notifications,
                    label = "Thông báo",
                    onClick = { navController.navigate(Screen.Notifications) }
                )
            }

            ProfileMenuSection(title = "Tài khoản") {
                ProfileMenuItem(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    label = "Đăng xuất",
                    labelColor = MacroProtein,
                    onClick = {
                        viewModel.logout {
                            navController.navigate(Screen.Welcome) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink900)
        Text(text = label, fontSize = 11.sp, color = Ink500)
    }
}

@Composable
private fun ProfileMenuSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ink500,
            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AppSurface),
            elevation = CardDefaults.cardElevation(1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    label: String,
    labelColor: Color = Ink900,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = if (labelColor == Ink900) Ink700 else labelColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(text = label, fontSize = 15.sp, color = labelColor, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Ink300, modifier = Modifier.size(18.dp))
    }
    HorizontalDivider(color = Ink200, thickness = 0.5.dp, modifier = Modifier.padding(start = 50.dp))
}
