package com.vitalai.ui.screens.diary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import com.vitalai.data.remote.model.FoodDto
import com.vitalai.navigation.Screen
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFoodScreen(
    navController: NavController,
    mealType: String = "",
    date: String = "",
    viewModel: FoodViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Tất cả", "Gần đây", "Món của tôi", "Yêu thích")

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadAllFoods()
        viewModel.loadFavorites()
    }

    LaunchedEffect(uiState.addSuccess) {
        if (uiState.addSuccess) {
            viewModel.clearAddSuccess()
            navController.popBackStack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = AppMutedBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header with custom Search Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AppSurface2)
                        .clickable { navController.popBackStack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Ink900)
                }
                Spacer(modifier = Modifier.width(12.dp))
                TextField(
                    value = uiState.query,
                    onValueChange = viewModel::search,
                    placeholder = { Text("Tìm món ăn, thương hiệu...", color = Ink500, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Ink500) },
                    trailingIcon = { Icon(Icons.Default.Mic, contentDescription = null, tint = Ink500) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(VitalRadius.Pill)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = AppSurface2,
                        unfocusedContainerColor = AppSurface2,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )
            }

            // Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tabs.size) { i ->
                    val isSelected = selectedTab == i
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(VitalRadius.Pill))
                            .background(if (isSelected) Ink900 else AppSurface)
                            .border(1.dp, if (isSelected) Color.Transparent else AppLine, RoundedCornerShape(VitalRadius.Pill))
                            .clickable { selectedTab = i }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabs[i],
                            color = if (isSelected) Color.White else Ink900,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Action Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // AI Scan Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .clickable { navController.navigate(Screen.Scan) },
                    shape = RoundedCornerShape(VitalRadius.Lg),
                    colors = CardDefaults.cardColors(containerColor = Mint500)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("🔍", fontSize = 20.sp) // Replace with scan icon
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("AI Scan", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Quét ảnh món ăn", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                }

                // Create Food Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .clickable { navController.navigate(Screen.CreateFood) },
                    shape = RoundedCornerShape(VitalRadius.Lg),
                    colors = CardDefaults.cardColors(containerColor = AppSurface),
                    border = BorderStroke(1.dp, AppLine)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("✏️", fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Tạo món mới", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink900)
                        Text("Thêm vào My Foods", fontSize = 11.sp, color = Ink500)
                    }
                }
            }

            Text(
                text = "GẦN ĐÂY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Ink500,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            val displayItems = when {
                selectedTab == 3 -> uiState.favorites
                uiState.query.isNotBlank() -> uiState.searchResults
                selectedTab == 0 || selectedTab == 1 -> uiState.allFoods
                else -> emptyList()
            }

            when {
                uiState.isSearching || (selectedTab == 0 && uiState.query.isBlank() && uiState.isLoadingAll) ->
                    LoadingState()

                displayItems.isEmpty() && uiState.query.isNotBlank() ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Không tìm thấy kết quả", color = Ink500)
                    }

                else ->
                    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                        items(displayItems) { food ->
                            FoodSearchCard(
                                food = food,
                                onClick = {
                                    navController.navigate(
                                        Screen.FoodDetail(id = food.id, mealType = mealType, date = date)
                                    )
                                },
                                onQuickAdd = {
                                    viewModel.addToMealLog(mealType, date, food.id, 1f, "1 phần")
                                }
                            )
                        }
                    }
            }
        }
    }
}

@Composable
fun FoodSearchCard(food: FoodDto, onClick: () -> Unit, onQuickAdd: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(VitalRadius.Xl),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = BorderStroke(1.dp, AppLineSoft)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SubcomposeAsyncImage(
                model = food.imageUrl,
                contentDescription = food.name,
                modifier = Modifier
                    .size(56.dp)
                            .clip(RoundedCornerShape(VitalRadius.Lg)),
                contentScale = ContentScale.Crop,
                error = {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(VitalRadius.Lg))
                            .background(AppSurface2),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🍽️", fontSize = 24.sp)
                    }
                }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = food.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Ink900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "1 phần (${food.servingSizeG.toInt()}g) · ${food.caloriesPer100g.toInt()} kcal",
                    fontSize = 13.sp,
                    color = Ink500
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Mint500)
                    .clickable { onQuickAdd() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm nhanh", tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SearchFoodScreenPreview() {

    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        "Tất cả",
        "Gần đây",
        "Món của tôi",
        "Yêu thích"
    )

    val foods = listOf(
        FoodDto(
            id = "1",
            name = "Ức gà áp chảo",
            brand = "Healthy Meal",
            category = "Protein",
            imageUrls = null,
            servingSizeG = 100f,
            servingUnit = "g",
            caloriesPer100g = 165f,
            carbsPer100g = 0f,
            proteinPer100g = 31f,
            fatPer100g = 3.6f,
            fiberPer100g = 0f,
            sugarPer100g = 0f,
            sodiumPer100g = 75f,
            isVerified = true,
            isCustom = false
        ),
        FoodDto(
            id = "2",
            name = "Cơm gạo lứt",
            brand = "Healthy Meal",
            category = "Carbs",
            imageUrls = null,
            servingSizeG = 100f,
            servingUnit = "g",
            caloriesPer100g = 110f,
            carbsPer100g = 23f,
            proteinPer100g = 2.6f,
            fatPer100g = 0.9f,
            fiberPer100g = 1.8f,
            sugarPer100g = 0f,
            sodiumPer100g = 5f,
            isVerified = true,
            isCustom = false
        )
    )

    Scaffold(
        containerColor = Color(0xFFF9FAFB)
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3F4F6)),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = Color.Black
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                TextField(
                    value = "gà",
                    onValueChange = {},
                    placeholder = {
                        Text(
                            "Tìm món ăn, thương hiệu...",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(25.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF3F4F6),
                        unfocusedContainerColor = Color(0xFFF3F4F6),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )
            }

            // Tabs
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                items(tabs.size) { i ->

                    val isSelected = selectedTab == i

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected)
                                    Color(0xFF1E293B)
                                else
                                    Color.White
                            )
                            .border(
                                1.dp,
                                if (isSelected)
                                    Color.Transparent
                                else
                                    Color.Black,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                selectedTab = i
                            }
                            .padding(
                                horizontal = 16.dp,
                                vertical = 8.dp
                            ),
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = tabs[i],
                            color = if (isSelected)
                                Color.White
                            else
                                Color.Black,
                            fontWeight = if (isSelected)
                                FontWeight.Medium
                            else
                                FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Action cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF38C182)
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.Center
                    ) {

                        Text("🔍", fontSize = 20.sp)

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            "AI Scan",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            "Quét ảnh món ăn",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    border = BorderStroke(
                        1.dp,
                        Color(0xFFE5E7EB)
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.Center
                    ) {

                        Text("✏️", fontSize = 20.sp)

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            "Tạo món mới",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )

                        Text(
                            "Thêm vào My Foods",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Text(
                text = "GẦN ĐÂY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 8.dp
                )
            )

            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {

                items(foods) { food ->

                    FoodSearchCard(
                        food = food,
                        onClick = {},
                        onQuickAdd = {}
                    )
                }
            }
        }
    }
}

