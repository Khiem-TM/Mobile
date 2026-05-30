package com.vitalai.ui.screens.profile

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
    var isEditingDisplayName by remember { mutableStateOf(false) }
    var displayNameDraft by remember { mutableStateOf("") }
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

    LaunchedEffect(uiState.user?.id, uiState.user?.displayName) {
        if (!isEditingDisplayName) {
            displayNameDraft = uiState.user?.displayName.orEmpty()
        }
    }

    if (showEditAvatarDialog) {
        EditAvatarDialog(
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
                        if (isEditingDisplayName) {
                            OutlinedTextField(
                                value = displayNameDraft,
                                onValueChange = { displayNameDraft = it },
                                singleLine = true,
                                enabled = !uiState.isUpdatingProfile,
                                isError = displayNameDraft.trim().length !in 2..100,
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Ink900
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = {
                                        viewModel.updateProfile(displayNameDraft, uiState.user?.avatarUrl.orEmpty()) {
                                            Toast.makeText(context, "Cập nhật tên hiển thị thành công!", Toast.LENGTH_SHORT).show()
                                            isEditingDisplayName = false
                                        }
                                    },
                                    enabled = !uiState.isUpdatingProfile && displayNameDraft.trim().length in 2..100,
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    if (uiState.isUpdatingProfile) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text("Lưu", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                                TextButton(
                                    onClick = {
                                        viewModel.clearUpdateProfileError()
                                        displayNameDraft = uiState.user?.displayName.orEmpty()
                                        isEditingDisplayName = false
                                    },
                                    enabled = !uiState.isUpdatingProfile,
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Hủy", fontSize = 13.sp)
                                }
                            }
                            uiState.updateProfileError?.let { error ->
                                Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        } else {
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
                                            isEditingDisplayName = true
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
                                "maintain" -> "Giữ cân"
                                else -> "Mục tiêu"
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100))
                                    .background(AmberContainer)
                                    .padding(horizontal = 10.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🎯", fontSize = 10.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(goalText, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AmberOnContainer)
                            }
                            // Streak tag
                            val streak = uiState.streaks?.loginStreak ?: 0
                            if (streak > 0) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(100))
                                        .background(Mint50)
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🔥", fontSize = 11.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("$streak day streak", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Mint700)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val currentWeight = uiState.healthProfile?.initialWeightKg
                val targetWeight = uiState.healthProfile?.targetWeightKg
                val heightCm = uiState.healthProfile?.heightCm
                val bmi = if (currentWeight != null && heightCm != null && heightCm > 0) {
                    val heightM = heightCm / 100f
                    currentWeight / (heightM * heightM)
                } else null

                StatCard(
                    value = currentWeight?.let { String.format(Locale.US, "%.1f", it) } ?: "--",
                    unit = "kg",
                    label = "Hiện tại",
                    color = Mint500,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    value = targetWeight?.let { String.format(Locale.US, "%.0f", it) } ?: "--",
                    unit = "kg",
                    label = "Mục tiêu",
                    color = Ink900,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    value = bmi?.let { String.format(Locale.US, "%.1f", it) } ?: "--",
                    unit = "",
                    label = "BMI",
                    color = Mint500,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                    ProfileMenuItem(icon = Icons.Default.BarChart, label = "Chỉ số cơ thể") {
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
                        navController.navigate(Screen.CreateFood)
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
private fun StatCard(value: String, unit: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(VitalRadius.Lg),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = BorderStroke(1.dp, AppLine)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(unit, fontSize = 13.sp, color = Ink500, modifier = Modifier.padding(bottom = 2.dp))
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, fontSize = 12.sp, color = Ink500)
        }
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

@Composable
private fun EditAvatarDialog(
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

    AlertDialog(
        onDismissRequest = {
            if (!isSaving && !isUploading) onDismiss()
        },
        containerColor = AppSurface,
        title = {
            Text("Sửa avatar", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = avatarUrlDraft,
                    onValueChange = { avatarUrlDraft = it },
                    label = { Text("Avatar URL") },
                    singleLine = true,
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
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onUpload,
                    enabled = !isSaving && !isUploading
                ) {
                    Icon(
                        Icons.Default.FileUpload,
                        contentDescription = "Tải ảnh avatar",
                        tint = Ink700,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, enabled = !isSaving && !isUploading) {
                        Text("Hủy")
                    }
                    Button(
                        onClick = { onSave(avatarUrlDraft) },
                        enabled = !isSaving && !isUploading && avatarUrlDraft.trim().isNotBlank()
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = "Lưu",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun EditDisplayNameDialog(
    currentName: String,
    isSaving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (newName: String) -> Unit
) {
    var nameDraft by remember(currentName) { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        containerColor = AppSurface,
        title = { Text("Đổi tên hiển thị", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it },
                    label = { Text("Tên mới") },
                    singleLine = true,
                    enabled = !isSaving,
                    isError = nameDraft.trim().length !in 2..100,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(nameDraft) },
                enabled = !isSaving && nameDraft.trim().length in 2..100
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = "Lưu",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Hủy") }
        }
    )
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

            // Stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                StatCard(
                    value = "76.5",
                    unit = "kg",
                    label = "Hiện tại",
                    color = Color(0xFF38C182),
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    value = "70",
                    unit = "kg",
                    label = "Mục tiêu",
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    value = "24.1",
                    unit = "",
                    label = "BMI",
                    color = Color(0xFF38C182),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

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
