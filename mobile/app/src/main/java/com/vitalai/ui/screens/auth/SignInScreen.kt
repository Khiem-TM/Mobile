package com.vitalai.ui.screens.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.vitalai.R
import com.vitalai.navigation.Screen
import com.vitalai.ui.components.VitalIconButton
import com.vitalai.ui.theme.AppLine
import com.vitalai.ui.theme.Ink300
import com.vitalai.ui.theme.Ink500
import com.vitalai.ui.theme.Ink700
import com.vitalai.ui.theme.Ink900
import com.vitalai.ui.theme.Mint50
import com.vitalai.ui.theme.Mint500
import com.vitalai.ui.theme.Mint600
import com.vitalai.ui.theme.VitalAITheme
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

    SignInContent(
        email = email,
        onEmailChange = { email = it },
        password = password,
        onPasswordChange = { password = it },
        authState = authState,
        onBackClick = { navController.popBackStack() },
        onSignInClick = { viewModel.login(email.trim(), password) },
        onGoogleClick = {
            googleSignInClient.signOut().addOnCompleteListener {
                googleLauncher.launch(googleSignInClient.signInIntent)
            }
        },
        onSignUpClick = { navController.navigate(Screen.SignUp) }
    )
}

@Composable
private fun SignInContent(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    authState: AuthState,
    onBackClick: () -> Unit,
    onSignInClick: () -> Unit,
    onGoogleClick: () -> Unit,
    onSignUpClick: () -> Unit
) {
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

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Chào mừng trở lại 👋",
                style = MaterialTheme.typography.headlineLarge,
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

            AuthFieldLabel("Email")
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 14.dp),
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Ink500) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                shape = RoundedCornerShape(VitalRadius.Md),
                colors = authTextFieldColors(),
                placeholder = { Text("davil@vital.app", color = Ink300) }
            )

            AuthFieldLabel("Password")
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 8.dp),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Ink500) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                shape = RoundedCornerShape(VitalRadius.Md),
                colors = authTextFieldColors(),
                placeholder = { Text("••••••••", color = Ink300) }
            )

            Text(
                text = "Quên mật khẩu?",
                color = Mint600,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 12.dp, bottom = 24.dp)
                    .clickable { /* TODO */ }
            )

            if (authState is AuthState.Error) {
                Text(
                    text = authState.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp),
                    fontSize = 13.sp
                )
            }

            Button(
                onClick = onSignInClick,
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
                    Text("Đăng nhập",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 28.dp, bottom = 20.dp)
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SocialSignInButton(
                    label = "Google",
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_google_g),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.Unspecified
                        )
                    },
                    onClick = onGoogleClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = Ink500)) {
                            append("Chưa có tài khoản? ")
                        }
                        withStyle(style = SpanStyle(color = Mint600, fontWeight = FontWeight.Bold)) {
                            append("Đăng ký")
                        }
                    },
                    modifier = Modifier.clickable(onClick = onSignUpClick),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun AuthFieldLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Ink500,
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = Color.White,
    focusedContainerColor = Color.White,
    unfocusedBorderColor = AppLine,
    focusedBorderColor = Mint500,
    unfocusedLeadingIconColor = Ink500,
    focusedLeadingIconColor = Mint500,
    cursorColor = Mint500
)

@Composable
private fun SocialSignInButton(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(VitalRadius.Md),
        border = BorderStroke(1.2.dp, Color(0xFF747775)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = Ink700
        ),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        icon()
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.Medium)
    }
}


@Preview(showBackground = true, showSystemUi = true, name = "Sign In")
@Composable
private fun SignInContentPreview() {
    VitalAITheme {
        SignInContent(
            email = "davil@vital.app",
            onEmailChange = {},
            password = "password",
            onPasswordChange = {},
            authState = AuthState.Idle,
            onBackClick = {},
            onSignInClick = {},
            onGoogleClick = {},
            onSignUpClick = {}
        )
    }
}
