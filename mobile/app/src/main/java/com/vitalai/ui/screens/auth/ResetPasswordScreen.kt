package com.vitalai.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vitalai.ui.components.VitalIconButton
import com.vitalai.ui.theme.AppLine
import com.vitalai.ui.theme.Ink300
import com.vitalai.ui.theme.Ink500
import com.vitalai.ui.theme.Ink700
import com.vitalai.ui.theme.Ink900
import com.vitalai.ui.theme.Mint50
import com.vitalai.ui.theme.Mint500
import com.vitalai.ui.theme.Mint600
import com.vitalai.ui.theme.VitalRadius

@Composable
fun ResetPasswordScreen(
    navController: NavController,
    token: String = "",
    viewModel: AuthViewModel = hiltViewModel()
) {
    var tokenInput by rememberSaveable(token) { mutableStateOf(token) }
    var newPassword by rememberSaveable { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.resetState()
    }

    val canSubmit = tokenInput.isNotBlank() && newPassword.length >= 8 && authState !is AuthState.Loading

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
        Text(
            text = "Đặt lại mật khẩu",
            style = MaterialTheme.typography.headlineLarge,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Ink900
        )
        Text(
            text = "Nhập token từ email và mật khẩu mới của bạn.",
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = Ink500,
            modifier = Modifier.padding(top = 8.dp, bottom = 28.dp)
        )

        OutlinedTextField(
            value = tokenInput,
            onValueChange = {
                tokenInput = it
                if (authState !is AuthState.Idle) viewModel.resetState()
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Token") },
            shape = RoundedCornerShape(VitalRadius.Md),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                unfocusedBorderColor = AppLine,
                focusedBorderColor = Mint500,
                cursorColor = Mint500
            )
        )

        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = newPassword,
            onValueChange = {
                newPassword = it
                if (authState !is AuthState.Idle) viewModel.resetState()
            },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Ink500) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            label = { Text("Mật khẩu mới") },
            placeholder = { Text("Tối thiểu 8 ký tự", color = Ink300) },
            shape = RoundedCornerShape(VitalRadius.Md),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                unfocusedBorderColor = AppLine,
                focusedBorderColor = Mint500,
                cursorColor = Mint500
            )
        )

        val message = when (val state = authState) {
            is AuthState.Success -> state.userName
            is AuthState.Error -> state.message
            else -> null
        }
        if (!message.isNullOrBlank()) {
            Text(
                text = message,
                color = if (authState is AuthState.Success) Mint600 else MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .background(if (authState is AuthState.Success) Mint50 else Color(0xFFFFEBEE), RoundedCornerShape(VitalRadius.Md))
                    .padding(14.dp)
            )
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = { viewModel.resetPassword(tokenInput.trim(), newPassword) },
            colors = ButtonDefaults.buttonColors(containerColor = Ink900),
            shape = RoundedCornerShape(100),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            modifier = Modifier.fillMaxWidth(),
            enabled = canSubmit
        ) {
            Text(
                text = if (authState is AuthState.Loading) "Đang đặt lại..." else "Đặt lại mật khẩu",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
