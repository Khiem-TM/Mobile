package com.vitalai.ui.screens.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
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
    val tabs = listOf("Tất cả", "Yêu thích", "Của tôi")

    LaunchedEffect(Unit) {
        viewModel.loadAllFoods()
        viewModel.loadFavorites()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tìm kiếm món ăn", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    TextButton(onClick = { navController.navigate(Screen.CreateFood) }) {
                        Text("Tạo mới", color = Mint500)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppSurface)
            )
        },
        containerColor = AppBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::search,
                placeholder = { Text("Tìm kiếm món ăn...", color = Ink500) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Ink500) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Mint500,
                    unfocusedBorderColor = Ink200,
                    focusedContainerColor = AppSurface,
                    unfocusedContainerColor = AppSurface
                )
            )

            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = AppSurface,
                contentColor = Mint500,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { i, title ->
                    Tab(
                        selected = selectedTab == i,
                        onClick = { selectedTab = i },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == i) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == i) Mint500 else Ink500
                            )
                        }
                    )
                }
            }

            val displayItems = when {
                selectedTab == 1 -> uiState.favorites
                uiState.query.isNotBlank() -> uiState.searchResults
                selectedTab == 0 -> uiState.allFoods
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
                            FoodSearchItem(
                                food = food,
                                onClick = {
                                    navController.navigate(
                                        Screen.FoodDetail(id = food.id, mealType = mealType, date = date)
                                    )
                                }
                            )
                        }
                    }
            }
        }
    }
}

@Composable
private fun FoodSearchItem(food: FoodDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (food.imageUrl != null) {
            AsyncImage(
                model = food.imageUrl,
                contentDescription = food.name,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Mint50),
                contentAlignment = Alignment.Center
            ) {
                Text("🍽️", fontSize = 22.sp)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = food.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Ink900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = food.brand ?: "100g",
                fontSize = 12.sp,
                color = Ink500
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${food.caloriesPer100g.toInt()} kcal",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = Mint500
            )
            Text(text = "/100g", fontSize = 11.sp, color = Ink500)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Mint500),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "Thêm", tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}
