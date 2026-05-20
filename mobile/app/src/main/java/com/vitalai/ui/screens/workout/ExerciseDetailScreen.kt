package com.vitalai.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vitalai.navigation.Screen
import com.vitalai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    id: String,
    navController: NavController,
    viewModel: ExerciseLibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val exercise = uiState.exercises.firstOrNull { it.id == id }
        ?: uiState.filteredExercises.firstOrNull { it.id == id }
        ?: mockExercises().firstOrNull { it.id == id }
        ?: mockExercises().first()

    val isFavorite = exercise.id in uiState.favorites
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Cách thực hiện", "Mẹo tập đúng", "Biến thể")

    val heroImageUrl = exercise.imageUrl
        ?: "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800&q=80"

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AppBackground)
    ) {
        // Hero image
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                AsyncImage(
                    model = heroImageUrl,
                    contentDescription = exercise.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(0.3f), Color.Transparent, Color.Black.copy(0.4f))
                            )
                        )
                )
                // Top bar overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .statusBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(0.4f))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = Color.White)
                    }
                    Row {
                        IconButton(
                            onClick = { viewModel.toggleFavorite(exercise.id) },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(0.4f))
                        ) {
                            Icon(
                                if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Yêu thích",
                                tint = if (isFavorite) MacroProtein else Color.White
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {},
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(0.4f))
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Tùy chọn", tint = Color.White)
                        }
                    }
                }
                // Play button center
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = "Phát video",
                        tint = Color.White.copy(0.9f),
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
        }

        // Content
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                // Tags
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TagPill(exercise.muscleGroup, Mint500, Color.White)
                    TagPill("Vừa", MacroCarbs, Color.White)
                    exercise.equipment?.let { TagPill(it, Ink100, Ink700) }
                }
                Spacer(Modifier.height(12.dp))
                // Title
                Text(exercise.name, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Ink900)
                Spacer(Modifier.height(4.dp))
                Text(
                    exercise.description ?: "Bài tập hiệu quả giúp tăng cường sức mạnh và phát triển cơ bắp.",
                    fontSize = 14.sp, color = Ink500, lineHeight = 20.sp
                )
                Spacer(Modifier.height(16.dp))
                // Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard("MET", "5.0", "điểm", Mint500, Modifier.weight(1f))
                    StatCard("Năng lượng", "${(exercise.caloriesPerMin * 60).toInt()}", "kcal/h", MacroProtein, Modifier.weight(1f))
                    StatCard("Khuyến nghị", "3×12", "sets×reps", MacroCarbs, Modifier.weight(1f))
                }
            }
        }

        // Tabs
        item {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = AppSurface,
                contentColor = Mint500,
                edgePadding = 16.dp
            ) {
                tabs.forEachIndexed { i, title ->
                    Tab(
                        selected = selectedTab == i,
                        onClick = { selectedTab = i },
                        text = {
                            Text(
                                title, fontSize = 13.sp,
                                fontWeight = if (selectedTab == i) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }

        // Tab content
        item {
            when (selectedTab) {
                0 -> StepsContent()
                1 -> TipsContent()
                2 -> VariantsContent()
            }
        }

        // CTA
        item {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedIconButton(
                    onClick = { viewModel.toggleFavorite(exercise.id) },
                    border = ButtonDefaults.outlinedButtonBorder,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Yêu thích",
                        tint = if (isFavorite) MacroProtein else Ink500
                    )
                }
                Button(
                    onClick = { navController.navigate(Screen.WorkoutBuilder) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Mint500)
                ) {
                    Text("Thêm vào buổi tập", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TagPill(text: String, bgColor: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 11.sp, color = textColor, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StatCard(label: String, value: String, unit: String, accentColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = accentColor)
            Text(unit, fontSize = 10.sp, color = Ink500)
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 11.sp, color = Ink700, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun StepsContent() {
    val steps = listOf(
        "Nằm ngửa trên ghế bench, lưng phẳng, hai chân chạm sàn.",
        "Grip thanh tạ rộng hơn vai một chút.",
        "Hạ thanh tạ xuống ngực một cách kiểm soát.",
        "Đẩy thanh tạ lên đến khi tay duỗi thẳng hoàn toàn.",
        "Giữ nhịp thở: hít vào khi hạ, thở ra khi đẩy.",
        "Lặp lại theo số rep mong muốn."
    )
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        steps.forEachIndexed { i, step ->
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Mint500),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${i + 1}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                Text(step, fontSize = 13.sp, color = Ink700, lineHeight = 18.sp, modifier = Modifier.padding(top = 3.dp))
            }
        }
    }
}

@Composable
private fun TipsContent() {
    val dos = listOf("Giữ lưng phẳng trên bench", "Kiểm soát trọng lượng khi hạ", "Thở đúng nhịp")
    val donts = listOf("Không nảy tạ trên ngực", "Tránh khóa khuỷu tay hoàn toàn", "Không dùng lưng để đẩy")
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Nên làm", fontWeight = FontWeight.Bold, color = Mint500, fontSize = 13.sp)
        dos.forEach { tip ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✓", color = Mint500, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp))
                Text(tip, fontSize = 13.sp, color = Ink700)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Không nên", fontWeight = FontWeight.Bold, color = MacroProtein, fontSize = 13.sp)
        donts.forEach { tip ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✗", color = MacroProtein, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp))
                Text(tip, fontSize = 13.sp, color = Ink700)
            }
        }
    }
}

@Composable
private fun VariantsContent() {
    val variants = listOf(
        "Incline Bench Press" to "Tập phần trên ngực",
        "Decline Bench Press" to "Tập phần dưới ngực",
        "Dumbbell Bench Press" to "Dùng tạ đơn linh hoạt hơn",
        "Close-Grip Bench Press" to "Tập thêm cơ tay sau"
    )
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        variants.forEach { (name, desc) ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AppSurface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Mint50),
                        contentAlignment = Alignment.Center
                    ) { Text("💪", fontSize = 16.sp) }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Ink900)
                        Text(desc, fontSize = 11.sp, color = Ink500)
                    }
                }
            }
        }
    }
}
