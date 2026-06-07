package com.vitalai.ui.screens.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vitalai.navigation.Screen
import com.vitalai.ui.theme.AppLine
import com.vitalai.ui.theme.AppSurface2
import com.vitalai.ui.theme.Ink900
import com.vitalai.ui.theme.Mint500
import com.vitalai.ui.theme.VitalRadius

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
    val height = if (isSmall) 130.dp else 190.dp
    
    Card(
        modifier = modifier.height(height),
        shape = RoundedCornerShape(VitalRadius.Lg),
        colors = CardDefaults.cardColors(containerColor = AppSurface2),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, AppLine.copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = mealType,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(AppSurface2),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🍽️", fontSize = if (isSmall) 24.sp else 44.sp)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                            startY = 100f
                        )
                    )
            )

            Box(modifier = Modifier.fillMaxSize().padding(if (isSmall) 8.dp else 16.dp)) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .padding(horizontal = if (isSmall) 6.dp else 8.dp, vertical = if (isSmall) 2.dp else 4.dp)
                ) {
                    Text(
                        text = mealType.uppercase(), 
                        fontSize = if (isSmall) 8.sp else 10.sp, 
                        fontWeight = FontWeight.ExtraBold, 
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(end = if (isSmall) 85.dp else 100.dp)
                ) {
                    Text(
                        text = description,
                        fontSize = if (isSmall) 13.sp else 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        lineHeight = if (isSmall) 16.sp else 28.sp,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    if (!isSmall && time.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(time, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = if (isSmall) 6.dp else 10.dp, vertical = if (isSmall) 4.dp else 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = "Kcal", tint = Color(0xFFFFD54F), modifier = Modifier.size(if (isSmall) 12.dp else 16.dp))
                        Text(
                            text = "$calories kcal",
                            fontSize = if (isSmall) 10.sp else 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}