package com.vitalai.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.vitalai.navigation.BottomNavReselectBus
import com.vitalai.navigation.Screen
import com.vitalai.ui.theme.AppLine
import com.vitalai.ui.theme.AppSurface
import com.vitalai.ui.theme.Ink400
import com.vitalai.ui.theme.Mint100
import com.vitalai.ui.theme.Mint400
import com.vitalai.ui.theme.Mint600
import com.vitalai.ui.theme.VitalElevation
import com.vitalai.ui.theme.VitalRadius
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt

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
    BottomNavItem("Khám phá", Icons.Default.Explore, Screen.Discover),
    BottomNavItem("Hồ sơ", Icons.Default.Person, Screen.Profile)
)

/**
 * Mô phỏng vật thể trôi trong nước của quả cầu tuyết.
 * Nối tiếp vận tốc của ngón tay lúc thả (không khựng), rơi theo TRỌNG LỰC
 * nhưng bị LỰC CẢN của nước làm chậm lại, chạm đáy thì NẢY yếu dần rồi tự tắt,
 * đồng thời trôi ngang về tâm và lắng về vị trí cũ (Offset.Zero).
 */
private suspend fun settleLikeSnowglobe(
    initialVelocity: Offset,
    get: () -> Offset,
    set: (Offset) -> Unit
) {
    // Giới hạn vận tốc ban đầu để cú ném quá mạnh không bị "bắn" đi quá xa
    val maxSpeed = 4000f
    var velocity = Offset(
        initialVelocity.x.coerceIn(-maxSpeed, maxSpeed),
        initialVelocity.y.coerceIn(-maxSpeed, maxSpeed)
    )

    val gravity = 2600f       // Trọng lực kéo ngôi sao xuống
    val sideSpring = 60f
    // Lò xo kéo theo phương ngang về tâm
    val viscosity = 4f        // Độ nhớt của nước (càng lớn rơi càng chậm)
    val restitution = 0.5f    // Độ nảy khi chạm đáy: 0 = không nảy, 1 = nảy hết cỡ
    val bounceCutoff = 70f    // Dưới tốc độ này thì thôi nảy -> nằm yên

    var lastFrame = 0L
    while (true) {
        val frame = withFrameNanos { it }
        // Frame đầu giả định ~16ms để DI CHUYỂN NGAY, không bỏ phí frame
        val dt = if (lastFrame == 0L) 1f / 60f
        else ((frame - lastFrame) / 1_000_000_000f).coerceIn(0f, 1f / 30f)
        lastFrame = frame

        val p = get()

        // Lực: ngang -> lò xo về tâm; dọc -> chỉ có trọng lực khi còn ở TRÊN đáy
        val ax = -sideSpring * p.x
        val ay = if (p.y < 0f) gravity else 0f

        // Tích phân vận tốc + lực cản nhớt của nước
        velocity = Offset(velocity.x + ax * dt, velocity.y + ay * dt)
        velocity *= exp(-viscosity * dt)

        var next = Offset(p.x + velocity.x * dt, p.y + velocity.y * dt)

        // Chạm đáy -> nảy lại yếu dần (mỗi lần mất bớt năng lượng -> tự tắt)
        if (next.y >= 0f) {
            next = Offset(next.x, 0f)
            velocity = if (velocity.y > bounceCutoff)
                Offset(velocity.x, -velocity.y * restitution)
            else
                Offset(velocity.x, 0f)
        }

        set(next)

        // Đã nằm đáy, hết nảy và phương ngang đã lắng -> chốt về 0 rồi dừng
        if (next.y == 0f && abs(velocity.y) < 1f && abs(next.x) < 0.5f && abs(velocity.x) < 8f) {
            set(Offset.Zero)
            break
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VitalBottomNavBar(navController: NavController) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route
    val density = LocalDensity.current
    val navigationBarHeight = with(density) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }

    // --- TRẠNG THÁI ANIMATION (state thường, đọc trong lambda -> không recompose) ---
    var offset by remember { mutableStateOf(Offset.Zero) }
    var pressed by remember { mutableStateOf(false) }
    var physicsJob by remember { mutableStateOf<Job?>(null) }
    var showAiActions by remember { mutableStateOf(false) }

    // Theo dõi vận tốc ngón tay để cú thả nối tiếp liền mạch (không khựng)
    val velocityTracker = remember { VelocityTracker() }

    // Scale lấy dưới dạng State, đọc .value trong graphicsLayer { } để hoãn sang pha draw
    val scaleState = animateFloatAsState(
        targetValue = if (pressed) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "scanScale"
    )

    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .background(Color.Transparent)
    ) {
        // --- NỀN THANH ĐIỀU HƯỚNG ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .align(Alignment.BottomCenter)
                .shadow(elevation = VitalElevation.Level3, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(AppSurface.copy(alpha = 0.96f))
        ) {
            HorizontalDivider(color = AppLine, thickness = 1.dp, modifier = Modifier.align(Alignment.TopCenter))
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 14.dp),
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

        if (showAiActions) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-62).dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AiQuickActionButton(
                    icon = Icons.Default.ChatBubble,
                    label = "Hỏi AI",
                    onClick = {
                        showAiActions = false
                        navController.navigate(Screen.Coach) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                AiQuickActionButton(
                    icon = Icons.Default.CameraAlt,
                    label = "Camera",
                    onClick = {
                        showAiActions = false
                        navController.navigate(Screen.Scan)
                    }
                )
            }
        }

        // --- NÚT SCAN NỔI BẬT VỚI HIỆU ỨNG KÉO THẢ ---
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                // Nhô lên 10dp khỏi thanh nav nhưng KHÔNG tính vào chiều cao đo được,
                // nhờ Box cha cao đúng 90dp -> Scaffold chỉ chừa 90dp, nội dung không bị xén.
                .offset(y = (-10).dp)
                .size(64.dp)
                // Đọc offset trong lambda -> chỉ invalidate layout, KHÔNG recompose
                .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                // Đọc scale trong lambda -> chỉ invalidate draw, KHÔNG recompose
                .graphicsLayer {
                    scaleX = scaleState.value
                    scaleY = scaleState.value
                }
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            // Hủy mô phỏng rơi (nếu đang chạy) để bám theo tay ngay
                            physicsJob?.cancel()
                            physicsJob = null
                            pressed = true
                            velocityTracker.resetTracking()
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offset += dragAmount // Cập nhật đồng bộ, KHÔNG mở coroutine
                            // Ghi lại vị trí + thời điểm để tính vận tốc lúc thả
                            velocityTracker.addPosition(change.uptimeMillis, offset)
                        },
                        onDragEnd = {
                            pressed = false
                            val v = velocityTracker.calculateVelocity()
                            physicsJob?.cancel()
                            physicsJob = coroutineScope.launch {
                                // Nối tiếp vận tốc ngón tay -> đi liền, không khựng
                                settleLikeSnowglobe(Offset(v.x, v.y), { offset }, { offset = it })
                            }
                        },
                        onDragCancel = {
                            pressed = false
                            physicsJob?.cancel()
                            physicsJob = coroutineScope.launch {
                                settleLikeSnowglobe(Offset.Zero, { offset }, { offset = it })
                            }
                        }
                    )
                }
                // Hình dáng và thiết kế tĩnh ( shadow TRƯỚC background )
                .shadow(VitalElevation.Fab, CircleShape, clip = false)
                .clip(CircleShape)
                .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Mint400, Mint600)))
                // Xử lý nhấp chuột thường
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        showAiActions = !showAiActions
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = "Scan", tint = Color.White, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun AiQuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .shadow(VitalElevation.Level2, RoundedCornerShape(VitalRadius.Pill), clip = false)
            .clip(RoundedCornerShape(VitalRadius.Pill))
            .background(AppSurface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Mint600),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
        }
        Spacer(modifier = Modifier.width(7.dp))
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Mint600, maxLines = 1)
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
            } else {
                item.route::class.qualifiedName?.let(BottomNavReselectBus::emit)
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
