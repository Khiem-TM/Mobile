package com.vitalai.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.vitalai.navigation.Screen
import com.vitalai.ui.theme.Ink700
import com.vitalai.ui.theme.MacroCarbs
import com.vitalai.ui.theme.MacroProtein
import com.vitalai.ui.theme.Mint500
import com.vitalai.ui.theme.VitalAITheme

@Composable
fun WelcomeScreen(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Background Image
        AsyncImage(
            model = "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?ixlib=rb-4.0.3&auto=format&fit=crop&w=1200&q=80",
            contentDescription = "Healthy food background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Floating Tags
        // Egg
        FoodTag(
            text = "Egg · 280 kcal",
            dotColor = MacroProtein,
            modifier = Modifier
                .offset(x = 32.dp, y = 100.dp)
        )

        // Avocado
        FoodTag(
            text = "Avocado · 160 kcal",
            dotColor = MacroCarbs,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-32).dp, y = 220.dp)
        )

        // Greens
        FoodTag(
            text = "Greens · 45 kcal",
            dotColor = Mint500,
            modifier = Modifier
                .offset(x = 48.dp, y = 320.dp)
        )

        // Gradient Overlay at bottom for text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.4f to Color.Transparent,
                        0.7f to Color.Black.copy(alpha = 0.6f),
                        1.0f to Color.Black.copy(alpha = 0.95f)
                    )
                )
        )

        // Bottom Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 40.dp)
        ) {
            // Indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .height(4.dp)
                        .width(24.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White)
                )
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.5f))
                )
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.5f))
                )
            }

            // Title
            Text(
                text = "Ăn thông minh, sống khỏe mỗi ngày",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 42.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Subtitle
            Text(
                text = "Theo dõi calo, quét bữa ăn bằng AI, và nhận tư vấn cá nhân hóa từ huấn luyện viên ảo.",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 16.sp,
                lineHeight = 24.sp,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Button
            Button(
                onClick = { navController.navigate(Screen.SignUp) },
                colors = ButtonDefaults.buttonColors(containerColor = Mint500),
                shape = RoundedCornerShape(100),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bắt đầu ngay",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Forward",
                        tint = Color.White
                    )
                }
            }

            // Login text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = Color.White.copy(alpha = 0.8f))) {
                            append("Đã có tài khoản? ")
                        }
                        withStyle(style = SpanStyle(color = Mint500, fontWeight = FontWeight.Bold)) {
                            append("Đăng nhập")
                        }
                    },
                    modifier = Modifier.clickable {
                        navController.navigate(Screen.SignIn)
                    },
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun FoodTag(text: String, dotColor: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(100),
        color = Color.White,
        modifier = modifier.padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = Ink700,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Preview(showBackground = true, name = "Welcome Screen")
@Composable
fun WelcomeScreenPreview() {
    VitalAITheme {
        WelcomeScreen(rememberNavController())
    }
}
