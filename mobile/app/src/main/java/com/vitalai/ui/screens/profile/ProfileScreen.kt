package com.vitalai.ui.screens.profile

import android.content.Context
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vitalai.navigation.Screen
import com.vitalai.ui.theme.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import android.widget.Toast
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import com.vitalai.util.copyUriToCacheFile
import java.io.File
import java.util.Locale

/** Chiều cao cố định dùng chung cho các badge nhỏ (PRO, Goal) ở header Hồ sơ. */
private val ProfileBadgeHeight = 24.dp

@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showEditAvatarDialog by remember { mutableStateOf(false) }
    var showEditDisplayNameDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val uploadFile = copyUriToCacheFile(context, uri, prefix = "avatar")
            if (uploadFile != null) {
                viewModel.uploadAvatar(uploadFile.file, uploadFile.mimeType) {
                    Toast.makeText(context, "Tải ảnh đại diện thành công!", Toast.LENGTH_SHORT).show()
                    showEditAvatarDialog = false
                }
            } else {
                viewModel.setUpdateProfileError("Không thể đọc ảnh đã chọn")
            }
        }
    }

    if (showEditDisplayNameDialog) {
        EditDisplayNameSheet(
            currentName = uiState.user?.displayName.orEmpty(),
            isSaving = uiState.isUpdatingProfile,
            error = uiState.updateProfileError,
            onDismiss = {
                viewModel.clearUpdateProfileError()
                showEditDisplayNameDialog = false
            },
            onSave = { newName ->
                viewModel.updateProfile(newName, uiState.user?.avatarUrl.orEmpty()) {
                    Toast.makeText(context, "Cập nhật tên hiển thị thành công!", Toast.LENGTH_SHORT).show()
                    showEditDisplayNameDialog = false
                }
            }
        )
    }

    if (showEditAvatarDialog) {
        EditAvatarSheet(
            avatarUrl = uiState.user?.avatarUrl.orEmpty(),
            isSaving = uiState.isUpdatingProfile,
            isUploading = uiState.isUploadingAvatar,
            error = uiState.updateProfileError,
            onDismiss = {
                viewModel.clearUpdateProfileError()
                showEditAvatarDialog = false
            },
            onSave = { avatarUrl ->
                viewModel.updateProfile(uiState.user?.displayName.orEmpty(), avatarUrl) {
                    Toast.makeText(context, "Cập nhật ảnh đại diện thành công!", Toast.LENGTH_SHORT).show()
                    showEditAvatarDialog = false
                }
            },
            onUpload = {
                viewModel.clearUpdateProfileError()
                avatarPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        )
    }

    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = { Text("Đăng xuất?", color = ForestGreen, fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc chắn muốn đăng xuất khỏi tài khoản này không?", color = Ink700) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        viewModel.logout {
                            navController.navigate(Screen.Welcome) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White
                    )
                ) {
                    Text("Chắc chắn", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("Hủy", color = Ink500, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
            Text("Hồ sơ", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
            val streak = uiState.streaks?.loginStreak ?: 0
            AnimatedVisibility(
                visible = streak > 0,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(150))
            ) {
                val strokeWidthPx = with(LocalDensity.current) { 3.5.dp.toPx() }
                Box(
                    modifier = Modifier
                    .wrapContentSize()
                    .offset(y = (-8).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .height(45.dp)
                            .aspectRatio(500f / 689f)
                    ) {
                        AsyncImage(
                            model = "file:///android_asset/ic_streak_flame.svg",
                            contentDescription = "Streak",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$streak",
                            style = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ForestGreen,
                                drawStyle = Stroke(
                                    width = strokeWidthPx,
                                    join = StrokeJoin.Round,
                                    cap = StrokeCap.Round
                                )
                            ),
                            maxLines = 1,
                            softWrap = false
                        )

                        Text(
                            text = "$streak",
                            style = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
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
                    val avatarSize = 85.dp
//                    Box(modifier = Modifier.size(76.dp)) {
                    Box(modifier = Modifier.size(avatarSize + 8.dp)) {
                        if (avatarUrl != null) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(avatarSize)
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(avatarSize)
                                    .clip(CircleShape)
                                    .background(AppSurface2),
                                contentAlignment = Alignment.Center
                            ) {
                                val initial = (uiState.user?.displayName?.firstOrNull() ?: 'U').uppercaseChar()
                                Text(initial.toString(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = -4.dp, y = -4.dp)
                                .size(30.dp)
                                .clip(CircleShape)
                                .clickable(
                                    enabled = !uiState.isUpdatingProfile && !uiState.isUploadingAvatar,
                                    onClick = {
                                        viewModel.clearUpdateProfileError()
                                        showEditAvatarDialog = true
                                    }
                                )
                                .border(3.dp, Color(0xFFF2F7F3), CircleShape)
                                .padding(3.dp)
                                .clip(CircleShape)
                                .background(ForestGreen),

                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = "Sửa avatar",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = uiState.user?.displayName ?: "Người dùng",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreen,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 36.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        viewModel.clearUpdateProfileError()
                                        showEditDisplayNameDialog = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Sửa tên",
                                    tint = Ink700,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = uiState.user?.email ?: "email@vital.app",
                            fontSize = 12.sp,
                            color = Ink500
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // PRO badge
                            ProBadge()
                            // Goal tag
                            val goalText = when (uiState.healthProfile?.goalType?.lowercase()) {
                                "lose_weight" -> "Giảm cân"
                                "gain_weight" -> "Tăng cân"
                                "gain_muscle" -> "Tăng cơ"
                                "improve_endurance" -> "Tăng sức bền"
                                "bulking" -> "Xả cơ"
                                "cutting" -> "Siết cơ"
                                "maintain" -> "Giữ cân"
                                else -> "Mục tiêu"
                            }
                            Row(
                                modifier = Modifier
                                    .height(ProfileBadgeHeight)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ForestGreen)
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Flag,
                                    contentDescription = "Goal",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(goalText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Menu Section 1
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(VitalRadius.Md),
            colors = CardDefaults.cardColors(containerColor = AppSurface),
            elevation = CardDefaults.cardElevation(
                defaultElevation = VitalElevation.Level1
            )
        ) {
                Column {
                    ProfileMenuItem(icon = Icons.Default.BarChart, label = "Hồ sơ cơ thể") {
                        navController.navigate(Screen.Metrics)
                    }
                    ProfileMenuItem(icon = Icons.Default.FitnessCenter, label = "Luyện tập") {
                        navController.navigate(Screen.Workout)
                    }
                    ProfileMenuItem(icon = Icons.Default.TrackChanges, label = "Mục tiêu & kế hoạch") {
                        navController.navigate(Screen.Goals)
                    }
                    ProfileMenuItem(icon = Icons.Default.EmojiEvents, label = "Huy hiệu của tôi") {
                        // navigate to badges
                    }
                    ProfileMenuItem(icon = Icons.Default.RoomService, label = "Món ăn của tôi") {
                        navController.navigate(Screen.SearchFood(initialTab = 2))
                    }
                    ProfileMenuItem(icon = Icons.Default.Article, label = "Bài viết của tôi", showDivider = false) {
                        navController.navigate(Screen.MyBlogs)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Logout
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(VitalRadius.Md),
            colors = CardDefaults.cardColors(containerColor = AppSurface),
            // SỬ DỤNG ELEVATION CHUẨN Ở ĐÂY
            elevation = CardDefaults.cardElevation(
                defaultElevation = VitalElevation.Level1
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // Clickable đặt ở đây để vùng bấm ăn trọn vẹn cả khối Card trắng
                    .clickable(enabled = !uiState.isLoggingOut) {
                        showLogoutConfirmDialog = true
                    }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.isLoggingOut) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFFEF4444)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text(
                    text = if (uiState.isLoggingOut) "Đang đăng xuất..." else "Đăng xuất",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFEF4444)
                )
            }
        }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

/** Chip "PRO" cạnh tên người dùng — cùng bộ hiệu ứng glow/shimmer/press như TidePlusBadgeCard,
 *  thu nhỏ tỉ lệ cho phù hợp kích thước chip. */
@Composable
private fun ProBadge(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(8.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "proBadgeScale"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.32f else 0.18f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "proBadgeGlowAlpha"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "proBadgeShimmer")
    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "proBadgeShimmerProgress"
    )

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .height(ProfileBadgeHeight)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF161616), Color(0xFF090909)),
                    start = Offset(0f, 0f),
                    end = Offset(1100f, 500f)
                )
            )
            .clickable(interactionSource = interactionSource, indication = null) { /* TODO: điều hướng tới màn nâng cấp gói */ },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = glowAlpha), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.5f),
                    radius = w * 0.6f
                ),
                radius = w * 0.6f,
                center = Offset(w * 0.5f, h * 0.5f)
            )

            val lineColor = Color.White.copy(alpha = 0.05f)
            val curves = listOf(0.2f to 0.5f, 0.5f to 0.9f)
            curves.forEach { (startFrac, endFrac) ->
                val path = Path().apply {
                    moveTo(-0.1f * w, h * startFrac)
                    cubicTo(
                        w * 0.3f, h * (startFrac - 0.18f),
                        w * 0.65f, h * (endFrac + 0.18f),
                        w * 1.1f, h * endFrac
                    )
                }
                drawPath(path, color = lineColor, style = Stroke(width = 1.dp.toPx()))
            }

            val band = h * 1.4f
            val x0 = -band + shimmerProgress * (w + band)
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.28f),
                        Color.Transparent
                    ),
                    start = Offset(x0, 0f),
                    end = Offset(x0 + band, h)
                ),
                size = size
            )
        }
        Text(
            "PRO",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 15.dp)
        )
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
                .size(36.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = ForestGreen, modifier = Modifier.size(25.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(text = label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Ink700, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Ink300, modifier = Modifier.size(20.dp))
    }
    if (showDivider) {
        HorizontalDivider(color = AppLineSoft, thickness = 1.dp, modifier = Modifier.padding(start = 70.dp))
    }
}

/** OutlinedTextField với viền giống ô input của màn Hồ sơ cơ thể (xám đậm khi focus, xám nhạt khi không). */
@Composable
private fun DialogBorderedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    topLabel: String? = null,
    placeholder: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    isError: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor = if (isFocused) Color(0xFF9E9E9E) else Color(0xFFDDDDDD)

    Column(modifier = modifier) {
        if (topLabel != null) {
            Text(
                topLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = ForestGreen,
                modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, borderColor, RoundedCornerShape(8.dp)),
            placeholder = placeholder,
            singleLine = true,
            enabled = enabled,
            isError = isError,
            interactionSource = interactionSource,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
                errorBorderColor = Color.Transparent,
                cursorColor = ForestGreen,
                focusedPlaceholderColor = Ink400,
                unfocusedPlaceholderColor = Ink400
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditAvatarSheet(
    avatarUrl: String,
    isSaving: Boolean,
    isUploading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (avatarUrl: String) -> Unit,
    onUpload: () -> Unit
) {
    var avatarUrlDraft by remember(avatarUrl) {
        mutableStateOf(avatarUrl)
    }
    var step by remember { mutableStateOf(AvatarSheetStep.Choice) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = {
            if (!isSaving && !isUploading) onDismiss()
        },
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0) },
        dragHandle = { BottomSheetDefaults.DragHandle(color = BottomSheetGrabber) }
    ) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (step != AvatarSheetStep.Choice) {
                            IconButton(
                                onClick = { step = AvatarSheetStep.Choice },
                                enabled = !isSaving && !isUploading,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Quay lại",
                                    tint = ForestGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            "Cập nhật ảnh đại diện",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = ForestGreen
                        )
                    }

                    when (step) {
                        AvatarSheetStep.Choice -> {
                            AvatarOptionRow(
                                icon = Icons.Default.Link,
                                label = "Link ảnh",
                                onClick = { step = AvatarSheetStep.Link }
                            )
                            AvatarOptionRow(
                                icon = Icons.Default.PhotoLibrary,
                                label = "Tải ảnh lên",
                                onClick = { step = AvatarSheetStep.Upload }
                            )
                        }

                        AvatarSheetStep.Link -> {
                            DialogBorderedTextField(
                                value = avatarUrlDraft,
                                onValueChange = { avatarUrlDraft = it },
                                topLabel = "Link ảnh",
                                enabled = !isSaving,
                                placeholder = { Text("https://example.com/avatar.jpg") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (error != null) {
                                Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                            }
                            Button(
                                onClick = { onSave(avatarUrlDraft) },
                                enabled = !isSaving && avatarUrlDraft.trim().isNotBlank(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(VitalRadius.Md),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ForestGreen,
                                    disabledContainerColor = ForestGreen.copy(alpha = 0.5f),
                                    contentColor = Color.White,
                                    disabledContentColor = Color.White.copy(alpha = 0.6f)
                                )
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = "Lưu",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        AvatarSheetStep.Upload -> {
                            Button(
                                onClick = onUpload,
                                enabled = !isUploading,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(VitalRadius.Md),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ForestGreen,
                                    disabledContainerColor = ForestGreen.copy(alpha = 0.5f),
                                    contentColor = Color.White,
                                    disabledContentColor = Color.White.copy(alpha = 0.6f)
                                )
                            ) {
                                Icon(
                                    Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Chọn ảnh từ thư viện",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (isUploading) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Đang tải ảnh lên...", color = ForestGreen, fontSize = 13.sp)
                                }
                            }
                            if (error != null) {
                                Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class AvatarSheetStep { Choice, Link, Upload }

@Composable
private fun AvatarOptionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(VitalRadius.Md))
            .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(VitalRadius.Md))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = ForestGreen,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ink700,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Ink400,
            modifier = Modifier.size(22.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditDisplayNameSheet(
    currentName: String,
    isSaving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (newName: String) -> Unit
) {
    var nameDraft by remember(currentName) { mutableStateOf(currentName) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0) },
        dragHandle = { BottomSheetDefaults.DragHandle(color = BottomSheetGrabber) }
    ) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Cập nhật tên hiển thị",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = ForestGreen
                    )
                    DialogBorderedTextField(
                        value = nameDraft,
                        onValueChange = { nameDraft = it },
                        topLabel = "Tên mới",
                        enabled = !isSaving,
                        isError = nameDraft.trim().length !in 2..100,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (error != null) {
                        Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }
                    Button(
                        onClick = { onSave(nameDraft) },
                        enabled = !isSaving && nameDraft.trim().length in 2..100,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(VitalRadius.Md),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ForestGreen,
                            disabledContainerColor = ForestGreen.copy(alpha = 0.5f),
                            contentColor = Color.White,
                            disabledContentColor = Color.White.copy(alpha = 0.6f)
                        )
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = "Lưu",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileScreenPreview() {

    Scaffold(
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

                Text(
                    "Hồ sơ",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(
                            1.dp,
                            Color(0xFFF3F4F6),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // User card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {

                Row(verticalAlignment = Alignment.Top) {

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE5E7EB)),
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            "S",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {

                        Text(
                            text = "Sơn Royale",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )

                        Text(
                            text = "son@example.com",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100))
                                    .background(Color(0xFF0F172A))
                                    .padding(
                                        horizontal = 10.dp,
                                        vertical = 4.dp
                                    )
                            ) {

                                Text(
                                    "PRO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100))
                                    .background(Color(0xFFFEF3C7))
                                    .padding(
                                        horizontal = 10.dp,
                                        vertical = 4.dp
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Text("🎯", fontSize = 11.sp)

                                Spacer(modifier = Modifier.width(4.dp))

                                Text(
                                    "Giảm cân",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFB45309)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100))
                                    .background(Color(0xFFECFDF5))
                                    .padding(
                                        horizontal = 10.dp,
                                        vertical = 4.dp
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Text("🔥", fontSize = 11.sp)

                                Spacer(modifier = Modifier.width(4.dp))

                                Text(
                                    "7 day streak",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF047857)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 1
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                border = BorderStroke(
                    1.dp,
                    Color(0xFFF3F4F6)
                )
            ) {

                Column {

                    ProfileMenuItem(
                        icon = Icons.Default.BarChart,
                        label = "Chỉ số cơ thể"
                    ) {}

                    ProfileMenuItem(
                        icon = Icons.Default.TrackChanges,
                        label = "Mục tiêu & kế hoạch"
                    ) {}

                    ProfileMenuItem(
                        icon = Icons.Default.EmojiEvents,
                        label = "Huy hiệu của tôi"
                    ) {}

                    ProfileMenuItem(
                        icon = Icons.Default.RoomService,
                        label = "Món ăn của tôi",
                        showDivider = false
                    ) {}
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "KHÁM PHÁ THÊM",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(
                    horizontal = 24.dp,
                    vertical = 8.dp
                )
            )

            // Section 2
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                border = BorderStroke(
                    1.dp,
                    Color(0xFFF3F4F6)
                )
            ) {

                Column {

                    ProfileMenuItem(
                        icon = Icons.Default.Language,
                        label = "Khám phá (Blog)"
                    ) {}

                    ProfileMenuItem(
                        icon = Icons.Default.Notifications,
                        label = "Thông báo"
                    ) {}

                    ProfileMenuItem(
                        icon = Icons.Default.Settings,
                        label = "Cài đặt",
                        showDivider = false
                    ) {}
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
                    .border(
                        1.dp,
                        Color(0xFFF3F4F6),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(22.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    "Đăng xuất",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFEF4444)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
