package com.vitalai.ui.screens.scan

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.vitalai.navigation.Screen
import com.vitalai.ui.theme.Mint500
import com.vitalai.util.ClassificationResult
import java.util.concurrent.Executors

@Composable
fun ScanScreen(
    navController: NavController,
    viewModel: ScanViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    var torchEnabled by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(uiState.navTarget) {
        when (val target = uiState.navTarget) {
            is ScanNavTarget.Detail -> navController.navigate(Screen.FoodDetail(id = target.foodId))
            is ScanNavTarget.Create -> navController.navigate(Screen.CreateFood(prefillName = target.name))
            null -> {}
        }
        if (uiState.navTarget != null) viewModel.consumeNav()
    }

    LaunchedEffect(torchEnabled, camera) {
        camera?.cameraControl?.enableTorch(torchEnabled)
    }

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            // Camera preview
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            val bitmap = imageProxy.toSafeBitmap()
                            imageProxy.close()
                            if (bitmap != null) viewModel.classify(bitmap)
                        }
                        try {
                            cameraProvider.unbindAll()
                            camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )
                        } catch (_: Exception) {}
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Viewfinder reticle
            val scanProgress by rememberInfiniteTransition(label = "scan_line").animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1600, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scan_progress"
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-60).dp)
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(280.dp)) {
                    val sw = 4.dp.toPx()
                    val cl = 42.dp.toPx()
                    val s = sw / 2
                    val w = size.width
                    val h = size.height
                    val corners = listOf(
                        listOf(s to s, cl to s, s to s, s to cl),
                        listOf(w - s to s, w - cl to s, w - s to s, w - s to cl),
                        listOf(s to h - s, cl to h - s, s to h - s, s to h - cl),
                        listOf(w - s to h - s, w - cl to h - s, w - s to h - s, w - s to h - cl)
                    )
                    corners.forEach { pts ->
                        drawLine(Color.White, androidx.compose.ui.geometry.Offset(pts[0].first, pts[0].second), androidx.compose.ui.geometry.Offset(pts[1].first, pts[1].second), sw)
                        drawLine(Color.White, androidx.compose.ui.geometry.Offset(pts[2].first, pts[2].second), androidx.compose.ui.geometry.Offset(pts[3].first, pts[3].second), sw)
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (scanProgress * 278f).dp)
                        .width(220.dp)
                        .height(2.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Mint500.copy(alpha = 0.92f))
                )
                Text(
                    text = "Đưa món ăn vào khung",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 28.dp)
                )
            }
        } else {
            // No permission UI
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Cần quyền truy cập camera", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = Mint500)
                ) {
                    Text("Cấp quyền camera")
                }
            }
        }

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 48.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Đóng", tint = Color.White)
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Mint500.copy(alpha = 0.9f)
            ) {
                Text(
                    "AI Scan",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    fontSize = 13.sp
                )
            }
            IconButton(
                onClick = { torchEnabled = !torchEnabled },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Flash",
                    tint = if (torchEnabled) Color.Yellow else Color.White
                )
            }
        }

        // Bottom panel: live probabilities + capture button
        val isConfident = uiState.results.firstOrNull()?.let { it.confidence >= 0.5f } == true
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = uiState.results.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxWidth()
            ) {
                ResultCard(
                    results = uiState.results,
                    inferenceTimeMs = uiState.inferenceTimeMs
                )
            }

            AnimatedVisibility(
                visible = uiState.results.isEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Món ăn", "Barcode", "Nhãn").forEachIndexed { index, label ->
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (index == 0) Color.White else Color.Black.copy(alpha = 0.42f)
                        ) {
                            Text(
                                label,
                                color = if (index == 0) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            CaptureButton(
                enabled = isConfident && !uiState.isResolving,
                isResolving = uiState.isResolving,
                onClick = { viewModel.captureAndResolve() }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = when {
                    uiState.isResolving -> "Đang mở món..."
                    isConfident -> "Chạm để chọn món"
                    else -> "Đang nhận diện..."
                },
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun CaptureButton(
    enabled: Boolean,
    isResolving: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .alpha(if (enabled || isResolving) 1f else 0.35f)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.2f))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(6.dp)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        if (isResolving) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = Mint500,
                strokeWidth = 3.dp
            )
        } else {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, Color.Black.copy(alpha = 0.12f), CircleShape)
            )
        }
    }
}

@Composable
private fun ResultCard(results: List<ClassificationResult>, inferenceTimeMs: Long) {
    val top = results.firstOrNull() ?: return
    val isConfident = top.confidence >= 0.5f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 32.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.Black.copy(alpha = 0.82f),
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isConfident) top.label else "Đang nhận diện...",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (isConfident) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Mint500.copy(alpha = 0.85f)
                    ) {
                        Text(
                            text = "${(top.confidence * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (isConfident) {
                Spacer(Modifier.height(14.dp))
                results.drop(1).take(4).forEach { result ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = result.label,
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 13.sp,
                            modifier = Modifier.width(160.dp)
                        )
                        LinearProgressIndicator(
                            progress = { result.confidence },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Mint500.copy(alpha = 0.7f),
                            trackColor = Color.White.copy(alpha = 0.15f)
                        )
                        Text(
                            text = "${(result.confidence * 100).toInt()}%",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Inference: ${inferenceTimeMs}ms",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 11.sp
            )
        }
    }
}

private fun ImageProxy.toSafeBitmap(): Bitmap? {
    return try {
        val plane = planes[0]
        val rowPadding = plane.rowStride - plane.pixelStride * width
        val bitmap = Bitmap.createBitmap(
            width + rowPadding / plane.pixelStride,
            height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(plane.buffer)
        if (rowPadding == 0) bitmap else Bitmap.createBitmap(bitmap, 0, 0, width, height)
    } catch (_: Exception) {
        null
    }
}
