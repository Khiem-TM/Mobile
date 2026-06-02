package com.vitalai.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vitalai.navigation.Screen
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
import com.vitalai.ui.theme.VitalRadius

@Composable
fun VerifyEmailScreen(
    navController: NavController,
    email: String,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var code by rememberSaveable { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            viewModel.resetState()
            navController.navigate(Screen.Onboarding(1)) {
                popUpTo(Screen.Welcome::class.qualifiedName!!) { inclusive = true }
            }
        }
    }

    val sanitizedCode = code.filter { it.isDigit() }.take(6)
    val canSubmit = sanitizedCode.length == 6 && authState !is AuthState.Loading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp)
            .padding(top = 60.dp, bottom = 28.dp)
    ) {
        VitalIconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = Ink700)
        }

        Spacer(Modifier.height(40.dp))

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

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Xác minh email",
            style = MaterialTheme.typography.headlineLarge,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Ink900
        )
        Text(
            text = "Nhập mã 6 số đã gửi tới $email để tiếp tục onboarding.",
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = Ink500,
            modifier = Modifier.padding(top = 8.dp, bottom = 28.dp)
        )

        OutlinedTextField(
            value = sanitizedCode,
            onValueChange = {
                code = it.filter { char -> char.isDigit() }.take(6)
                if (authState !is AuthState.Idle) viewModel.resetState()
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                textAlign = TextAlign.Center,
                letterSpacing = 6.sp,
                fontWeight = FontWeight.Bold
            ),
            shape = RoundedCornerShape(VitalRadius.Md),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                unfocusedBorderColor = AppLine,
                focusedBorderColor = Mint500,
                cursorColor = Mint500
            ),
            placeholder = { Text("000000", color = Ink300, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
        )

        if (authState is AuthState.Error) {
            Text(
                text = (authState as AuthState.Error).message,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .background(Color(0xFFFFEBEE), RoundedCornerShape(VitalRadius.Md))
                    .padding(14.dp)
            )
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = { viewModel.verifyEmailCode(email, sanitizedCode) },
            colors = ButtonDefaults.buttonColors(containerColor = Ink900),
            shape = RoundedCornerShape(100),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            modifier = Modifier.fillMaxWidth(),
            enabled = canSubmit
        ) {
            Text(
                text = if (authState is AuthState.Loading) "Đang xác minh..." else "Xác minh",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Text(
            text = "Mã có hiệu lực trong 10 phút.",
            color = Ink500,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}
