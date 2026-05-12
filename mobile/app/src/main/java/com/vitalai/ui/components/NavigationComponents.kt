package com.vitalai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.vitalai.navigation.Screen

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: Any
)

val bottomNavItemsLeft = listOf(
    BottomNavItem("Trang chủ", Icons.Default.Home, Screen.Home),
    BottomNavItem("Nhật ký", Icons.Default.MenuBook, Screen.Diary)
)

val bottomNavItemsRight = listOf(
    BottomNavItem("AI Coach", Icons.Default.SmartToy, Screen.Coach),
    BottomNavItem("Hồ sơ", Icons.Default.Person, Screen.Profile)
)

@Composable
fun VitalBottomNavBar(navController: NavController) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    val primaryGreen = Color(0xFF38C182)
    val activeBorderBlue = Color(0xFF2563EB)
    val inactiveGray = Color(0xFF9CA3AF)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .background(Color.Transparent)
    ) {
        // Bottom bar background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .align(Alignment.BottomCenter)
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color.White)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left items
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                    bottomNavItemsLeft.forEach { item ->
                        NavBarItem(item, currentRoute, navController, primaryGreen, activeBorderBlue, inactiveGray)
                    }
                }

                // Spacer for FAB
                Spacer(modifier = Modifier.width(64.dp))

                // Right items
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                    bottomNavItemsRight.forEach { item ->
                        NavBarItem(item, currentRoute, navController, primaryGreen, activeBorderBlue, inactiveGray)
                    }
                }
            }
        }

        // Center FAB
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp)
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.White) // Outer white ring
                .padding(6.dp)
                .clip(CircleShape)
                .background(primaryGreen)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    // Navigate to scan
                    navController.navigate(Screen.Scan)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Star, contentDescription = "Scan", tint = Color.White, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun NavBarItem(
    item: BottomNavItem,
    currentRoute: String?,
    navController: NavController,
    activeColor: Color,
    activeBorderColor: Color,
    inactiveColor: Color
) {
    val isSelected = currentRoute == item.route::class.qualifiedName

    val modifier = if (isSelected) {
        Modifier
            .border(1.5.dp, activeBorderColor, RoundedCornerShape(12.dp))
            .background(Color(0xFFF0FDF4), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    } else {
        Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            if (!isSelected) {
                navController.navigate(item.route) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    ) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = if (isSelected) activeColor else inactiveColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) activeBorderColor else inactiveColor
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalTopAppBar(
    title: String,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = { Text(text = title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = navigationIcon,
        actions = actions,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}
