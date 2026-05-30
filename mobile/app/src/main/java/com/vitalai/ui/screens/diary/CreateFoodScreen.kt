package com.vitalai.ui.screens.diary

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vitalai.data.remote.model.CreateFoodRequest
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.theme.*
import com.vitalai.util.PickedImage
import com.vitalai.util.copyUriToCacheFile

@Composable
fun CreateFoodScreen(
    navController: NavController,
    viewModel: FoodViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var servingSize by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var fiber by remember { mutableStateOf("") }
    var pickedImage by remember { mutableStateOf<PickedImage?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val picked = copyUriToCacheFile(context, uri, prefix = "food")
            if (picked != null) pickedImage = picked
            else viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.createSuccess) {
        if (uiState.createSuccess) navController.popBackStack()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = AppMutedBackground
    ) { padding ->
        if (uiState.isLoading) {
            LoadingState(modifier = Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
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
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Ink900)
                }
                Text("Món của tôi", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink900)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(VitalRadius.Pill))
                        .background(Mint500)
                        .clickable {
                            if (name.isNotBlank() && calories.isNotBlank()) {
                                viewModel.createFood(
                                    request = CreateFoodRequest(
                                        name = name,
                                        brand = brand.ifBlank { null },
                                        servingSizeG = servingSize.toFloatOrNull() ?: 100f,
                                        caloriesPer100g = calories.toFloatOrNull() ?: 0f,
                                        carbsPer100g = carbs.toFloatOrNull() ?: 0f,
                                        proteinPer100g = protein.toFloatOrNull() ?: 0f,
                                        fatPer100g = fat.toFloatOrNull() ?: 0f,
                                        fiberPer100g = fiber.toFloatOrNull()
                                    ),
                                    imageFile = pickedImage?.file,
                                    imageMimeType = pickedImage?.mimeType
                                )
                            } else {
                                Toast.makeText(context, "Vui lòng nhập tên món và calo", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text("Lưu", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Add Image Box
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(VitalRadius.Lg))
                        .background(AppSurface)
                        .border(1.5.dp, Mint500, RoundedCornerShape(VitalRadius.Lg))
                        .clickable {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val picked = pickedImage
                    if (picked != null) {
                        AsyncImage(
                            model = picked.file,
                            contentDescription = "Ảnh món ăn",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(VitalRadius.Lg))
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = Ink500, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Thêm ảnh", fontSize = 12.sp, color = Ink500)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Basic Info Fields
                FormField("TÊN MÓN", "VD: Salad cá ngừ tự làm", name) { name = it }
                Spacer(modifier = Modifier.height(16.dp))
                FormField("THƯƠNG HIỆU (TUỲ CHỌN)", "VD: Homemade", brand) { brand = it }
                Spacer(modifier = Modifier.height(16.dp))
                FormField("KHẨU PHẦN", "VD: 1 phần · 250g", servingSize) { servingSize = it }

                Spacer(modifier = Modifier.height(24.dp))
                Text("THÔNG TIN DINH DƯỠNG", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink500)
                Spacer(modifier = Modifier.height(8.dp))

                // Nutrition Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(VitalRadius.Lg),
                    colors = CardDefaults.cardColors(containerColor = AppSurface),
                    border = BorderStroke(1.dp, AppLine)
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        NutritionField("Năng lượng", calories, "kcal") { calories = it }
                        HorizontalDivider(color = AppLineSoft, thickness = 1.dp)
                        NutritionField("Carbs", carbs, "g") { carbs = it }
                        HorizontalDivider(color = AppLineSoft, thickness = 1.dp)
                        NutritionField("Protein", protein, "g") { protein = it }
                        HorizontalDivider(color = AppLineSoft, thickness = 1.dp)
                        NutritionField("Fat", fat, "g") { fat = it }
                        HorizontalDivider(color = AppLineSoft, thickness = 1.dp)
                        NutritionField("Fiber", fiber, "g") { fiber = it }
                    }
                }

                uiState.error?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = it, color = Color(0xFFEF4444), fontSize = 13.sp)
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun FormField(label: String, placeholder: String, value: String, onValueChange: (String) -> Unit) {
    Column {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink500)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Ink500) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(VitalRadius.Md),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Mint500,
                unfocusedBorderColor = AppLine,
                focusedContainerColor = AppSurface,
                unfocusedContainerColor = AppSurface,
                cursorColor = Mint500
            )
        )
    }
}

@Composable
fun NutritionField(label: String, value: String, unit: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 15.sp, color = Ink900)
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink900, textAlign = androidx.compose.ui.text.style.TextAlign.End),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(60.dp),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text("0", color = Ink500, fontSize = 15.sp, textAlign = androidx.compose.ui.text.style.TextAlign.End, modifier = Modifier.fillMaxWidth())
                    }
                    innerTextField()
                }
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(unit, fontSize = 13.sp, color = Ink500)
        }
    }
}
