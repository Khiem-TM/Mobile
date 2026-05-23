package com.vitalai.ui.screens.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

@Composable
fun SignUpScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
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
            viewModel.resetState()
            navController.navigate(Screen.Onboarding(1)) {
                popUpTo(Screen.Welcome::class.qualifiedName!!) { inclusive = true }
            }
        }
    }

    SignUpContent(
        name = name,
        onNameChange = { name = it },
        email = email,
        onEmailChange = { email = it },
        password = password,
        onPasswordChange = { password = it },
        authState = authState,
        onBackClick = { navController.popBackStack() },
        onGoogleClick = {
            googleSignInClient.signOut().addOnCompleteListener {
                googleLauncher.launch(googleSignInClient.signInIntent)
            }
        },
        onSignUpClick = {
            if (name.isNotBlank() && email.isNotBlank() && password.length >= 8) {
                viewModel.register(email.trim(), password, name.trim())
            }
        },
        onSignInClick = { navController.navigate(Screen.SignIn) }
    )
}

@Composable
private fun SignUpContent(
    name: String,
    onNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    authState: AuthState,
    onBackClick: () -> Unit,
    onGoogleClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onSignInClick: () -> Unit
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
                text = "Tạo tài khoản mới",
                style = MaterialTheme.typography.headlineLarge,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Ink900
            )

            Text(
                text = "Bắt đầu hành trình chăm sóc sức khỏe thông minh cùng VitalAI",
                fontSize = 15.sp,
                color = Ink500,
                lineHeight = 22.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 28.dp)
            )

            GoogleAuthButton(
                label = "Đăng ký với Google",
                onClick = onGoogleClick,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 24.dp, bottom = 24.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = AppLine)
                Text(
                    "Hoặc đăng ký bằng email",
                    modifier = Modifier.padding(horizontal = 12.dp),
                    fontSize = 12.sp,
                    color = Ink500
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = AppLine)
            }

            AuthFieldLabel("Họ và tên")
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 14.dp),
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Ink500) },
                singleLine = true,
                shape = RoundedCornerShape(VitalRadius.Md),
                colors = authTextFieldColors(),
                placeholder = { Text("Nguyễn Văn An", color = Ink300) }
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
                placeholder = { Text("email@example.com", color = Ink300) }
            )

            AuthFieldLabel("Mật khẩu")
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
                placeholder = { Text("Tối thiểu 8 ký tự", color = Ink300) }
            )

            if (authState is AuthState.Error) {
                Text(
                    text = authState.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 12.dp),
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onSignUpClick,
                colors = ButtonDefaults.buttonColors(containerColor = Ink900),
                shape = RoundedCornerShape(100),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() &&
                    email.isNotBlank() &&
                    password.length >= 8 &&
                    authState !is AuthState.Loading
            ) {
                if (authState is AuthState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Đăng ký",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = Ink500)) {
                            append("Đã có tài khoản? ")
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
private fun GoogleAuthButton(
    label: String,
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
        Icon(
            painter = painterResource(R.drawable.ic_google_g),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Color.Unspecified
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.Medium)
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Sign Up")
@Composable
private fun SignUpContentPreview() {
    VitalAITheme {
        SignUpContent(
            name = "Nguyễn Văn An",
            onNameChange = {},
            email = "an@example.com",
            onEmailChange = {},
            password = "password",
            onPasswordChange = {},
            authState = AuthState.Idle,
            onBackClick = {},
            onGoogleClick = {},
            onSignUpClick = {},
            onSignInClick = {}
        )
    }
}
