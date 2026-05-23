package com.vitalai.ui.screens.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.vitalai.R
import com.vitalai.navigation.Screen
import com.vitalai.ui.theme.AppLine
import com.vitalai.ui.theme.AppSurface2
import com.vitalai.ui.theme.Ink500
import com.vitalai.ui.theme.Ink900
import com.vitalai.ui.theme.Mint50
import com.vitalai.ui.theme.Mint500
import com.vitalai.ui.theme.VitalRadius
import com.vitalai.ui.theme.VitalAITheme

@OptIn(ExperimentalMaterial3Api::class)
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
        // Always try to extract result — DEVELOPER_ERROR comes back as RESULT_CANCELED
        // with the ApiException embedded in the intent data, so we can't gate on resultCode.
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
            if (e.statusCode != 12501) { // 12501 = user pressed back (silent cancel)
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

    Scaffold(
        containerColor = Mint50,
        topBar = {
            TopAppBar(
                title = { Text("Đăng ký", style = MaterialTheme.typography.titleLarge, color = Ink900) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Text("←", fontSize = 24.sp, color = Ink900)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Mint50
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Tạo tài khoản mới",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Ink900,
                modifier = Modifier.align(Alignment.Start)
            )

            Text(
                text = "Bắt đầu hành trình chăm sóc sức khỏe thông minh cùng Tracker",
                style = MaterialTheme.typography.bodyMedium,
                color = Ink500,
                modifier = Modifier.align(Alignment.Start).padding(top = 8.dp, bottom = 32.dp)
            )

            OutlinedButton(
                onClick = {
                    googleSignInClient.signOut().addOnCompleteListener {
                        googleLauncher.launch(googleSignInClient.signInIntent)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                border = BorderStroke(1.dp, AppLine),
                shape = RoundedCornerShape(VitalRadius.Pill),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Ink900),
                enabled = authState !is AuthState.Loading
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("G", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Đăng ký với Google", style = MaterialTheme.typography.titleMedium, color = Ink900)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(" Hoặc đăng ký bằng email ", modifier = Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.bodySmall, color = Ink500)
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Họ và tên") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(VitalRadius.Md),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = AppSurface2,
                    focusedContainerColor = AppSurface2,
                    unfocusedBorderColor = AppLine,
                    focusedBorderColor = Mint500,
                    unfocusedLabelColor = Ink500,
                    focusedLabelColor = Mint500,
                    unfocusedLeadingIconColor = Ink500,
                    focusedLeadingIconColor = Mint500
                ),
                isError = authState is AuthState.Error && name.isBlank()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                shape = RoundedCornerShape(VitalRadius.Md),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = AppSurface2,
                    focusedContainerColor = AppSurface2,
                    unfocusedBorderColor = AppLine,
                    focusedBorderColor = Mint500,
                    unfocusedLabelColor = Ink500,
                    focusedLabelColor = Mint500,
                    unfocusedLeadingIconColor = Ink500,
                    focusedLeadingIconColor = Mint500
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Mật khẩu (tối thiểu 8 ký tự)") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                shape = RoundedCornerShape(VitalRadius.Md),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = AppSurface2,
                    focusedContainerColor = AppSurface2,
                    unfocusedBorderColor = AppLine,
                    focusedBorderColor = Mint500,
                    unfocusedLabelColor = Ink500,
                    focusedLabelColor = Mint500,
                    unfocusedLeadingIconColor = Ink500,
                    focusedLeadingIconColor = Mint500
                )
            )

            if (authState is AuthState.Error) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = (authState as AuthState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (name.isNotBlank() && email.isNotBlank() && password.length >= 8) {
                        viewModel.register(email.trim(), password, name.trim())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(VitalRadius.Pill),
                colors = ButtonDefaults.buttonColors(containerColor = Ink900, contentColor = Color.White),
                enabled = authState !is AuthState.Loading
            ) {
                if (authState is AuthState.Loading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Đăng ký", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Đã có tài khoản?", style = MaterialTheme.typography.bodyMedium, color = Ink500)
                TextButton(onClick = { navController.navigate(Screen.SignIn) }) {
                    Text("Đăng nhập", fontWeight = FontWeight.Bold, color = Mint500)
                }
            }
        }
    }
}

//@Preview(showBackground = true, name = "Đăng ký")
//@Composable
//fun SignUpScreenPreview() {
//    VitalAITheme {
//        SignUpScreen(rememberNavController())
//    }
//}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SignUpScreenPreview() {

    VitalAITheme {

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text("Đăng ký")
                    }
                )
            }
        ) { padding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Tạo tài khoản mới",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Text(
                    text = "Bắt đầu hành trình chăm sóc sức khỏe thông minh cùng VitalAI",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(top = 8.dp, bottom = 32.dp)
                )

                OutlinedTextField(
                    value = "a",
                    onValueChange = {},
                    label = { Text("Họ và tên") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Person, null)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = "demo@gmail.com",
                    onValueChange = {},
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Email, null)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = "12345678",
                    onValueChange = {},
                    label = { Text("Mật khẩu") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Lock, null)
                    },
                    visualTransformation = PasswordVisualTransformation()
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        "Đăng ký",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
