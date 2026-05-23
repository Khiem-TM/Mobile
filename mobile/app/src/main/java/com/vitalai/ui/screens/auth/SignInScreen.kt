package com.vitalai.ui.screens.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.vitalai.R
import com.vitalai.navigation.Screen
import com.vitalai.ui.components.VitalButton
import com.vitalai.ui.components.VitalIconButton
import com.vitalai.ui.theme.AppLine
import com.vitalai.ui.theme.AppSurface2
import com.vitalai.ui.theme.Ink300
import com.vitalai.ui.theme.Ink500
import com.vitalai.ui.theme.Ink700
import com.vitalai.ui.theme.Ink900
import com.vitalai.ui.theme.Mint50
import com.vitalai.ui.theme.Mint500
import com.vitalai.ui.theme.VitalRadius

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current

    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            val token = account.idToken
            if (token != null) {
                viewModel.loginWithGoogle(token)
            } else {
                viewModel.handleGoogleError("Không lấy được Google ID token. Vui lòng thử lại.")
            }
        } catch (e: ApiException) {
            if (e.statusCode != 12501) {
                viewModel.handleGoogleError("Google Sign-In thất bại (mã lỗi: ${e.statusCode})")
            }
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            navController.navigate(Screen.Home) {
                popUpTo(Screen.Welcome::class.qualifiedName!!) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(Mint50, Color.White),
                    radius = 900f
                )
            )
            .padding(horizontal = 28.dp)
            .padding(top = 60.dp, bottom = 28.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            VitalIconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Back",
                    tint = Ink700
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = "Chào mừng trở lại \uD83D\uDC4B",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Ink900
            )
            
            Text(
                text = "Đăng nhập để tiếp tục hành trình của bạn",
                fontSize = 15.sp,
                color = Ink500,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
            )

            // Email input
            Text(
                text = "EMAIL",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Ink500,
                letterSpacing = 0.5.sp
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 14.dp),
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Ink500) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                shape = RoundedCornerShape(VitalRadius.Md),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = AppSurface2,
                    focusedContainerColor = AppSurface2,
                    unfocusedBorderColor = AppLine,
                    focusedBorderColor = Mint500
                ),
                placeholder = { Text("email@example.com", color = Ink300) }
            )

            // Password input
            Text(
                text = "PASSWORD",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Ink500,
                letterSpacing = 0.5.sp
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 8.dp),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Ink500) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                shape = RoundedCornerShape(VitalRadius.Md),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = AppSurface2,
                    focusedContainerColor = AppSurface2,
                    unfocusedBorderColor = AppLine,
                    focusedBorderColor = Mint500
                ),
                placeholder = { Text("••••••••", color = Ink300) }
            )

            Text(
                text = "Quên mật khẩu?",
                color = Mint500,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 12.dp, bottom = 24.dp)
                    .clickable { /* TODO */ }
            )

            if (authState is AuthState.Error) {
                Text(
                    text = (authState as AuthState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp),
                    fontSize = 13.sp
                )
            }

            Button(
                onClick = { viewModel.login(email, password) },
                colors = ButtonDefaults.buttonColors(containerColor = Ink900),
                shape = RoundedCornerShape(100),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                modifier = Modifier.fillMaxWidth(),
                enabled = email.isNotBlank() && password.isNotBlank() && authState !is AuthState.Loading
            ) {
                if (authState is AuthState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Đăng nhập", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Divider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 28.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = AppLine)
                Text(
                    "Hoặc tiếp tục với",
                    modifier = Modifier.padding(horizontal = 12.dp),
                    fontSize = 12.sp,
                    color = Ink500
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = AppLine)
            }

            // Social Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { /* TODO: Apple Sign In */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(VitalRadius.Md),
                    border = BorderStroke(1.dp, AppLine),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink700)
                ) {
                    Text("Apple", fontWeight = FontWeight.Medium)
                }
                
                OutlinedButton(
                    onClick = {
                        googleSignInClient.signOut().addOnCompleteListener {
                            googleLauncher.launch(googleSignInClient.signInIntent)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(VitalRadius.Md),
                    border = BorderStroke(1.dp, AppLine),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink700)
                ) {
                    Text("Google", fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = Ink500)) {
                            append("Chưa có tài khoản? ")
                        }
                        withStyle(style = SpanStyle(color = Mint500, fontWeight = FontWeight.Bold)) {
                            append("Đăng ký")
                        }
                    },
                    modifier = Modifier.clickable {
                        navController.navigate(Screen.SignUp)
                    },
                    fontSize = 13.sp
                )
            }
        }
    }
}
