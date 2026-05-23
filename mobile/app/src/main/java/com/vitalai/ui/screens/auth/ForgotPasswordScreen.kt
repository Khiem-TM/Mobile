package com.vitalai.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.vitalai.ui.components.VitalIconButton
import com.vitalai.ui.theme.AppLine
import com.vitalai.ui.theme.Ink300
import com.vitalai.ui.theme.Ink500
import com.vitalai.ui.theme.Ink700
import com.vitalai.ui.theme.Ink900
import com.vitalai.ui.theme.Mint50
import com.vitalai.ui.theme.Mint100
import com.vitalai.ui.theme.Mint500
import com.vitalai.ui.theme.Mint600
import com.vitalai.ui.theme.VitalAITheme
import com.vitalai.ui.theme.VitalRadius

@Composable
fun ForgotPasswordScreen(navController: NavController) {
    var email by rememberSaveable { mutableStateOf("") }
    var isSubmitted by rememberSaveable { mutableStateOf(false) }

    ForgotPasswordContent(
        email = email,
        onEmailChange = {
            email = it
            isSubmitted = false
        },
        isSubmitted = isSubmitted,
        onBackClick = { navController.popBackStack() },
        onSubmitClick = { isSubmitted = true },
        onSignInClick = { navController.popBackStack() }
    )
}

@Composable
private fun ForgotPasswordContent(
    email: String,
    onEmailChange: (String) -> Unit,
    isSubmitted: Boolean,
    onBackClick: () -> Unit,
    onSubmitClick: () -> Unit,
    onSignInClick: () -> Unit
) {
    val isEmailValid = remember(email) {
        email.trim().matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Mint50, Color.Transparent),
                        center = Offset.Zero,
                        radius = 850f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFFFF4C7), Color.Transparent),
                        center = Offset(900f, 0f),
                        radius = 700f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .padding(top = 60.dp, bottom = 28.dp)
        ) {
            VitalIconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Back",
                    tint = Ink700
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(Mint100, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MarkEmailRead,
                    contentDescription = null,
                    tint = Mint600,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Quên mật khẩu?",
                style = MaterialTheme.typography.headlineLarge,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Ink900
            )

            Text(
                text = "Nhập email đã đăng ký, VitalAI sẽ gửi hướng dẫn đặt lại mật khẩu cho bạn.",
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = Ink500,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
            )

            Text(
                text = "EMAIL",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink500,
                letterSpacing = 0.5.sp
            )
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Ink500) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                shape = RoundedCornerShape(VitalRadius.Md),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    unfocusedBorderColor = AppLine,
                    focusedBorderColor = Mint500,
                    unfocusedLeadingIconColor = Ink500,
                    focusedLeadingIconColor = Mint500,
                    cursorColor = Mint500
                ),
                placeholder = { Text("email@example.com", color = Ink300) }
            )

            if (email.isNotBlank() && !isEmailValid) {
                Text(
                    text = "Email không hợp lệ",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (isSubmitted) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp)
                        .background(Mint50, RoundedCornerShape(VitalRadius.Md))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Nếu email tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu đã được gửi.",
                        color = Mint600,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onSubmitClick,
                colors = ButtonDefaults.buttonColors(containerColor = Ink900),
                shape = RoundedCornerShape(100),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                modifier = Modifier.fillMaxWidth(),
                enabled = isEmailValid
            ) {
                Text(
                    text = if (isSubmitted) "Gửi lại hướng dẫn" else "Gửi hướng dẫn",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = Ink500)) {
                            append("Nhớ mật khẩu rồi? ")
                        }
                        withStyle(style = SpanStyle(color = Mint600, fontWeight = FontWeight.Bold)) {
                            append("Đăng nhập")
                        }
                    },
                    modifier = Modifier.clickable(onClick = onSignInClick),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Forgot Password")
@Composable
private fun ForgotPasswordPreview() {
    VitalAITheme {
        ForgotPasswordContent(
            email = "davil@vital.app",
            onEmailChange = {},
            isSubmitted = false,
            onBackClick = {},
            onSubmitClick = {},
            onSignInClick = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Forgot Password Sent")
@Composable
private fun ForgotPasswordSentPreview() {
    VitalAITheme {
        ForgotPasswordContent(
            email = "davil@vital.app",
            onEmailChange = {},
            isSubmitted = true,
            onBackClick = {},
            onSubmitClick = {},
            onSignInClick = {}
        )
    }
}
