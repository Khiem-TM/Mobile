package com.vitalai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
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
import com.vitalai.ui.theme.AppLine
import com.vitalai.ui.theme.AppSurface
import com.vitalai.ui.theme.Ink400
import com.vitalai.ui.theme.Mint100
import com.vitalai.ui.theme.Mint400
import com.vitalai.ui.theme.Mint600
import com.vitalai.ui.theme.VitalElevation
import com.vitalai.ui.theme.VitalRadius

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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .background(Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .align(Alignment.BottomCenter)
                .shadow(elevation = VitalElevation.Level3, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(AppSurface.copy(alpha = 0.96f))
        ) {
            HorizontalDivider(color = AppLine, thickness = 1.dp, modifier = Modifier.align(Alignment.TopCenter))
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                    bottomNavItemsLeft.forEach { item ->
                        NavBarItem(item, currentRoute, navController)
                    }
                }

                Spacer(modifier = Modifier.width(68.dp))

                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                    bottomNavItemsRight.forEach { item ->
                        NavBarItem(item, currentRoute, navController)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(64.dp)
                .clip(CircleShape)
                .background(AppSurface)
                .padding(6.dp)
                .clip(CircleShape)
                .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Mint400, Mint600)))
                .shadow(VitalElevation.Fab, CircleShape, clip = false)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    navController.navigate(Screen.Scan)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = "Scan", tint = Color.White, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
fun NavBarItem(
    item: BottomNavItem,
    currentRoute: String?,
    navController: NavController
) {
    val isSelected = currentRoute == item.route::class.qualifiedName

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
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(VitalRadius.Pill))
                .background(if (isSelected) Mint100 else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = if (isSelected) Mint600 else Ink400,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) Mint600 else Ink400,
            maxLines = 1
        )
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
