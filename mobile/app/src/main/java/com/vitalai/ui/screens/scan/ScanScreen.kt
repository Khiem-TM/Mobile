package com.vitalai.ui.screens.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.vitalai.ui.theme.Mint500

@Composable
fun ScanScreen(navController: NavController) {
    var selectedMode by remember { mutableIntStateOf(0) }
    val modes = listOf("Ảnh", "Barcode", "Nhãn")
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        snackbarHostState.showSnackbar("Tính năng AI Scan sẽ sớm ra mắt 🚀")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Reticle
        Box(
            modifier = Modifier.align(Alignment.Center)
        ) {
            androidx.compose.foundation.Canvas(
                modifier = Modifier.size(280.dp)
            ) {
                val strokeW = 4.dp.toPx()
                val cornerLen = 32.dp.toPx()
                val r = size
                val s = strokeW / 2

                // Draw 4 corner brackets
                listOf(
                    // Top-left
                    Offset(s, s) to listOf(
                        Offset(s, s) to Offset(cornerLen, s),
                        Offset(s, s) to Offset(s, cornerLen)
                    ),
                    // Top-right
                    Offset(r.width - s, s) to listOf(
                        Offset(r.width - s, s) to Offset(r.width - cornerLen, s),
                        Offset(r.width - s, s) to Offset(r.width - s, cornerLen)
                    ),
                    // Bottom-left
                    Offset(s, r.height - s) to listOf(
                        Offset(s, r.height - s) to Offset(cornerLen, r.height - s),
                        Offset(s, r.height - s) to Offset(s, r.height - cornerLen)
                    ),
                    // Bottom-right
                    Offset(r.width - s, r.height - s) to listOf(
                        Offset(r.width - s, r.height - s) to Offset(r.width - cornerLen, r.height - s),
                        Offset(r.width - s, r.height - s) to Offset(r.width - s, r.height - cornerLen)
                    )
                ).forEach { (_, lines) ->
                    lines.forEach { (start, end) ->
                        drawLine(
                            color = Color.White,
                            start = start,
                            end = end,
                            strokeWidth = strokeW
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (selectedMode == 1) "Hướng barcode vào khung" else "Đưa món ăn vào khung quét",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp
                )
            }
        }

        // Top controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
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
                onClick = { },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                Icon(Icons.Default.FlashOn, contentDescription = "Flash", tint = Color.White)
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mode selector
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                modes.forEachIndexed { i, mode ->
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (selectedMode == i) Mint500 else Color.White.copy(alpha = 0.2f),
                        onClick = { selectedMode = i }
                    ) {
                        Text(
                            mode,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = if (selectedMode == i) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Shutter
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 72.dp)
        )
    }
}
