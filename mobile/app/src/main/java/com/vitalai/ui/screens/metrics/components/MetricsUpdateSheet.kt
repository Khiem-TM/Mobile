package com.vitalai.ui.screens.metrics.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vitalai.data.remote.model.HealthProfileDto
import com.vitalai.data.remote.model.ProgressPhotoDto
import com.vitalai.data.remote.model.UpsertBodyMetricRequest
import com.vitalai.ui.screens.metrics.ageToBirthDate
import com.vitalai.ui.screens.metrics.calculateAge
import com.vitalai.ui.screens.metrics.calculateBmi
import com.vitalai.ui.screens.metrics.bmiColor
import com.vitalai.ui.screens.metrics.bmiLabel
import com.vitalai.ui.screens.metrics.formatCompact
import com.vitalai.ui.screens.metrics.parseFloatInput
import com.vitalai.ui.screens.metrics.genderDisplay
import com.vitalai.ui.screens.metrics.normalizeGender
import com.vitalai.ui.screens.metrics.viewmodels.MetricsUiState
import com.vitalai.ui.screens.metrics.viewmodels.MetricsViewModel
import com.vitalai.ui.theme.*
import com.vitalai.util.uriToFile
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UpdateBottomSheet(
    uiState: MetricsUiState,
    viewModel: MetricsViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        contentWindowInsets = { WindowInsets(0) },
        dragHandle = { BottomSheetDefaults.DragHandle(color = BottomSheetGrabber) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
            ) {
                when (uiState.updateSheetTab) {
                    0 -> BasicUpdateTab(uiState = uiState, viewModel = viewModel, onDone = onDismiss)
                    1 -> AdvancedUpdateTab(uiState = uiState, viewModel = viewModel, onDone = onDismiss)
                    2 -> PhotoUpdateTab(viewModel = viewModel, photos = uiState.photos, onDone = onDismiss)
                    3 -> PersonalInfoUpdateTab(uiState = uiState, viewModel = viewModel, onDone = onDismiss)
                }
            }
        }
    }
}

@Composable
private fun BasicUpdateTab(uiState: MetricsUiState, viewModel: MetricsViewModel, onDone: () -> Unit) {
    val latest = uiState.latest
    var weightStr by remember(latest?.weightKg) {
        mutableStateOf(latest?.weightKg?.let { it.formatCompact() }.orEmpty())
    }
    val weightVal = weightStr.parseFloatInput()
    val heightCm = uiState.healthProfile?.heightCm
    val bmiPreview = calculateBmi(weightVal, heightCm)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Cập nhật cân nặng", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ForestGreen)

        LabeledTextField(
            label = "Cân nặng",
            value = weightStr,
            onValueChange = { weightStr = it },
            suffix = "kg",
            modifier = Modifier.fillMaxWidth()
        )

        bmiPreview?.let { bmi ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(VitalRadius.Md))
                    .background(bmiColor(bmi).copy(alpha = 0.10f))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("BMI dự kiến: ", fontSize = 14.sp, color = Ink700)
                    Text(
                        bmi.formatCompact(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = bmiColor(bmi)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        bmiLabel(bmi),
                        fontSize = 13.sp,
                        color = bmiColor(bmi),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Button(
            onClick = {
                if (weightVal != null) {
                    viewModel.addMetric(
                        UpsertBodyMetricRequest(
                            recordedAt = LocalDate.now().toString(),
                            weightKg = weightVal,
                            heightCm = heightCm
                        )
                    )
                    onDone()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(VitalRadius.Md),
            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
        ) {
            Text("Lưu thông tin", fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonalInfoUpdateTab(uiState: MetricsUiState, viewModel: MetricsViewModel, onDone: () -> Unit) {
    val profile = uiState.healthProfile
    var genderExpanded by remember { mutableStateOf(false) }
    var gender by remember(profile?.gender) { mutableStateOf(normalizeGender(profile?.gender)) }
    var ageStr by remember(profile?.birthDate) {
        mutableStateOf(calculateAge(profile?.birthDate)?.toString().orEmpty())
    }
    var heightStr by remember(profile?.heightCm) {
        mutableStateOf(profile?.heightCm?.let { "%.0f".format(it) }.orEmpty())
    }
    var initialWeightStr by remember(profile?.initialWeightKg) {
        mutableStateOf(profile?.initialWeightKg?.let { it.formatCompact() }.orEmpty())
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Cập nhật thông tin cá nhân", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ForestGreen)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                InputTopLabel("Giới tính")
                ExposedDropdownMenuBox(
                    expanded = genderExpanded,
                    onExpandedChange = { genderExpanded = !genderExpanded }
                ) {
                    OutlinedTextField(
                        value = genderDisplay(gender),
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = metricTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = genderExpanded,
                        onDismissRequest = { genderExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        listOf("male" to "Nam", "female" to "Nữ", "other" to "Khác").forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    gender = value
                                    genderExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            LabeledTextField(
                label = "Tuổi",
                value = ageStr,
                onValueChange = { ageStr = it.filter(Char::isDigit) },
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LabeledTextField(
                label = "Chiều cao",
                value = heightStr,
                onValueChange = { heightStr = it },
                suffix = "cm",
                modifier = Modifier.weight(1f)
            )
            LabeledTextField(
                label = "Cân nặng ban đầu",
                value = initialWeightStr,
                onValueChange = { initialWeightStr = it },
                suffix = "kg",
                modifier = Modifier.weight(1f)
            )
        }

        Button(
            onClick = {
                val updated = (profile ?: HealthProfileDto()).copy(
                    gender = gender,
                    birthDate = ageStr.toIntOrNull()?.let { ageToBirthDate(it, profile?.birthDate) } ?: profile?.birthDate,
                    heightCm = heightStr.parseFloatInput(),
                    initialWeightKg = initialWeightStr.parseFloatInput()
                )
                viewModel.updateHealthProfile(updated)
                onDone()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(VitalRadius.Md),
            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
        ) {
            Text("Lưu thông tin", fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

private data class AdvancedField(
    val label: String,
    val value: String,
    val suffix: String,
    val onChange: (String) -> Unit
)

@Composable
private fun AdvancedUpdateTab(uiState: MetricsUiState, viewModel: MetricsViewModel, onDone: () -> Unit) {
    val latest = uiState.latest
    var bodyFatStr by remember(latest?.bodyFatPct) {
        mutableStateOf(latest?.bodyFatPct?.let { it.formatCompact() }.orEmpty())
    }
    var waistStr by remember(latest?.waistCm) {
        mutableStateOf(latest?.waistCm?.let { "%.0f".format(it) }.orEmpty())
    }
    var hipStr by remember(latest?.hipCm) {
        mutableStateOf(latest?.hipCm?.let { "%.0f".format(it) }.orEmpty())
    }
    var chestStr by remember(latest?.chestCm) {
        mutableStateOf(latest?.chestCm?.let { "%.0f".format(it) }.orEmpty())
    }
    var armStr by remember(latest?.armCm) {
        mutableStateOf(latest?.armCm?.let { "%.0f".format(it) }.orEmpty())
    }
    var neckStr by remember(latest?.neckCm) {
        mutableStateOf(latest?.neckCm?.let { "%.0f".format(it) }.orEmpty())
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Số đo nâng cao", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ForestGreen)
        Text("Tất cả trường đều tùy chọn", fontSize = 12.sp, color = Ink500)

        val fieldDefs = listOf(
            AdvancedField("% Mỡ cơ thể", bodyFatStr, "%") { v: String -> bodyFatStr = v },
            AdvancedField("Vòng eo", waistStr, "cm") { v: String -> waistStr = v },
            AdvancedField("Vòng hông", hipStr, "cm") { v: String -> hipStr = v },
            AdvancedField("Vòng ngực", chestStr, "cm") { v: String -> chestStr = v },
            AdvancedField("Bắp tay", armStr, "cm") { v: String -> armStr = v },
            AdvancedField("Vòng cổ", neckStr, "cm") { v: String -> neckStr = v }
        )

        fieldDefs.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { field ->
                    LabeledTextField(
                        label = field.label,
                        value = field.value,
                        onValueChange = field.onChange,
                        suffix = field.suffix,
                        modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Decimal
                    )
                }
            }
        }

        Button(
            onClick = {
                viewModel.addMetric(
                    UpsertBodyMetricRequest(
                        recordedAt = LocalDate.now().toString(),
                        bodyFatPct = bodyFatStr.parseFloatInput(),
                        waistCm = waistStr.parseFloatInput(),
                        hipCm = hipStr.parseFloatInput(),
                        chestCm = chestStr.parseFloatInput(),
                        neckCm = neckStr.parseFloatInput(),
                        armCm = armStr.parseFloatInput()
                    )
                )
                onDone()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(VitalRadius.Md),
            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
        ) {
            Text("Lưu số đo", fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

@Composable
private fun PhotoUpdateTab(
    viewModel: MetricsViewModel,
    photos: List<ProgressPhotoDto>,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    var selectedPhotoType by remember { mutableStateOf("front") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    val photoTypes = listOf(
        "front" to "Mặt trước",
        "back" to "Mặt sau",
        "side" to "Mặt bên"
    )

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) selectedUri = uri
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Thêm ảnh tiến độ", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink900)

        Text("Loại ảnh", fontSize = 14.sp, color = Ink700, fontWeight = FontWeight.SemiBold)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            photoTypes.forEach { (type, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(VitalRadius.Md))
                        .clickable { selectedPhotoType = type }
                        .background(
                            if (selectedPhotoType == type) Mint500.copy(alpha = 0.10f)
                            else Color.Transparent
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedPhotoType == type,
                        onClick = { selectedPhotoType = type },
                        colors = RadioButtonDefaults.colors(selectedColor = Mint500)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(label, fontSize = 14.sp, color = Ink900)
                }
            }
        }

        selectedUri?.let { uri ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(VitalRadius.Md))
            ) {
                AsyncImage(
                    model = uri,
                    contentDescription = "Ảnh đã chọn",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = { selectedUri = null },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Xóa ảnh đã chọn",
                        tint = Color.White,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .padding(4.dp)
                    )
                }
            }
        }

        OutlinedButton(
            onClick = { launcher.launch("image/*") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(VitalRadius.Md),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Mint600),
            border = androidx.compose.foundation.BorderStroke(1.dp, Mint500)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                if (selectedUri == null) "Chọn ảnh từ thư viện" else "Chọn ảnh khác",
                fontWeight = FontWeight.SemiBold
            )
        }

        Button(
            onClick = {
                val uri = selectedUri
                if (uri != null) {
                    val file = uriToFile(context, uri)
                    if (file != null) {
                        viewModel.uploadPhoto(file, selectedPhotoType)
                        onDone()
                    } else {
                        Toast.makeText(context, "Không thể đọc ảnh", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            enabled = selectedUri != null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(VitalRadius.Md),
            colors = ButtonDefaults.buttonColors(containerColor = Mint500)
        ) {
            Text("Tải lên", fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

@Composable
private fun InputTopLabel(label: String) {
    Text(
        label,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Ink500,
        modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
    )
}

@Composable
private fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    keyboardType: KeyboardType = KeyboardType.Decimal
) {
    Column(modifier = modifier) {
        InputTopLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            suffix = suffix?.let { { Text(it, color = Ink500, fontSize = 12.sp) } },
            modifier = Modifier.fillMaxWidth(),
            colors = metricTextFieldColors()
        )
    }
}

@Composable
private fun metricTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ForestGreen,
    focusedLabelColor = ForestGreen,
    cursorColor = ForestGreen,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    disabledContainerColor = Color.White,
    errorContainerColor = Color.White
)
