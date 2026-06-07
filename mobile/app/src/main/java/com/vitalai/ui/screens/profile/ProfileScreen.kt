package com.vitalai.ui.screens.profile

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import android.widget.Toast
import androidx.compose.material.icons.outlined.Flag
import com.vitalai.util.copyUriToCacheFile
import java.io.File
import java.util.Locale
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showEditAvatarDialog by remember { mutableStateOf(false) }
    var showEditDisplayNameDialog by remember { mutableStateOf(false) }
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
            Text("Hồ sơ", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink900)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AppSurface)
                        .border(1.dp, AppLine, CircleShape)
                        .clickable { /* settings */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Ink900, modifier = Modifier.size(22.dp))
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
                                Text(initial.toString(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink500)
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
                                .background(Color(0xFF0EB67E)),

                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = "Sửa avatar",
                                tint = AppSurface,
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
                                color = Ink900,
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
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0EB67E))
                                    .padding(horizontal = 15.dp, vertical = 1.dp)
                            ) {
                                Text("PRO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
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
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MacroWater)
                                    .padding(horizontal = 10.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Flag,
                                    contentDescription = "Goal",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(goalText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            // Streak tag
                            val streak = uiState.streaks?.loginStreak ?: 0
                            if (streak > 0) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFF6D00))
                                        .padding(horizontal = 10.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LocalFireDepartment,
                                        contentDescription = "Goal",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Chuỗi $streak", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
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
                shape = RoundedCornerShape(VitalRadius.Lg),
                colors = CardDefaults.cardColors(containerColor = AppSurface),
                border = BorderStroke(1.dp, AppLine)
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
                    ProfileMenuItem(icon = Icons.Default.FavoriteBorder, label = "Món của tôi (My Foods)") {
                        navController.navigate(Screen.SearchFood(initialTab = 2))
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
                color = Ink500,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            // Menu Section 2
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(VitalRadius.Lg),
                colors = CardDefaults.cardColors(containerColor = AppSurface),
                border = BorderStroke(1.dp, AppLine)
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
                    .clip(RoundedCornerShape(VitalRadius.Lg))
                    .background(AppSurface)
                    .border(1.dp, AppLine, RoundedCornerShape(VitalRadius.Lg))
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
                .background(AppSurface2),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Ink700, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(text = label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Ink900, modifier = Modifier.weight(1f))
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
                color = Ink500,
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = {
            if (!isSaving && !isUploading) onDismiss()
        },
        sheetState = sheetState,
        containerColor = Color.White,
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Cập nhật ảnh đại diện",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = ForestGreen
                        )
                        IconButton(
                            onClick = onUpload,
                            enabled = !isSaving && !isUploading
                        ) {
                            Icon(
                                Icons.Default.FileUpload,
                                contentDescription = "Tải ảnh avatar",
                                tint = ForestGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    DialogBorderedTextField(
                        value = avatarUrlDraft,
                        onValueChange = { avatarUrlDraft = it },
                        topLabel = "Link ảnh",
                        enabled = !isSaving && !isUploading,
                        placeholder = { Text("https://example.com/avatar.jpg") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isUploading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Đang tải ảnh lên...", color = Ink500, fontSize = 13.sp)
                        }
                    }
                    if (error != null) {
                        Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }
                    Button(
                        onClick = { onSave(avatarUrlDraft) },
                        enabled = !isSaving && !isUploading && avatarUrlDraft.trim().isNotBlank(),
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
        containerColor = Color.White,
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
                        icon = Icons.Default.FavoriteBorder,
                        label = "Món của tôi (My Foods)",
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
