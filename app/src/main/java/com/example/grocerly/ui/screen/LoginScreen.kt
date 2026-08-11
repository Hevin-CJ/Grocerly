package com.example.grocerly.ui.screen

import androidx.activity.ComponentActivity
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.grocerly.R
import com.example.grocerly.utils.NetworkResult
import com.example.grocerly.utils.NetworkUtils
import com.example.grocerly.utils.RegisterValidation
import com.example.grocerly.viewmodel.LoginViewModel
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel = hiltViewModel(),
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit = {},
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val callbackManager = remember { CallbackManager.Factory.create() }

    DisposableEffect(Unit) {
        LoginManager.getInstance().registerCallback(
            callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onCancel() {
                    Toast.makeText(context, "Login Cancelled", Toast.LENGTH_SHORT).show()
                }

                override fun onError(error: FacebookException) {
                    Toast.makeText(context, error.message.toString(), Toast.LENGTH_SHORT).show()
                }

                override fun onSuccess(result: LoginResult) {
                    loginViewModel.signInWithFacebook(result.accessToken)
                }
            }
        )
        onDispose { }
    }

    LaunchedEffect(Unit) {
        loginViewModel.validationState.collectLatest { state ->
            emailError = if (state.email is RegisterValidation.Failed) state.email.message else null
            passwordError = if (state.password is RegisterValidation.Failed) state.password.message else null
        }
    }

    // Observe Login State
    LaunchedEffect(Unit) {
        loginViewModel.loginstate.collectLatest { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    isLoading = true
                }
                is NetworkResult.Success -> {
                    isLoading = false
                    onLoginSuccess()
                }
                is NetworkResult.Error -> {
                    isLoading = false
                    Toast.makeText(context, result.message ?: "An error occurred", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    isLoading = false
                }
            }
        }
    }

    LoginContent(
        emailError = emailError,
        passwordError = passwordError,
        isLoading = isLoading,
        onLoginClick = { email, password ->
            if (NetworkUtils.isNetworkAvailable(context)) {
                loginViewModel.loginUserIntoFirebase(email.trim(), password.trim())
            } else {
                Toast.makeText(context, "Enable Wifi or Mobile Data", Toast.LENGTH_SHORT).show()
            }
        },
        onNavigateToSignUp = onNavigateToSignUp,
        onNavigateToForgotPassword = onNavigateToForgotPassword,
        onGoogleLoginClick = { loginViewModel.signInWithGoogle() },
        onFacebookLoginClick = {
            val currentToken = AccessToken.getCurrentAccessToken()
            if (currentToken != null && !currentToken.isExpired) {
                loginViewModel.signInWithFacebook(currentToken)
            } else {
                activity?.let {
                    LoginManager.getInstance().logInWithReadPermissions(
                        it,
                        callbackManager,
                        listOf("email", "public_profile")
                    )
                }
            }
        },
        onXLoginClick = {
            activity?.let { loginViewModel.signInWithX(it) }
        },
        onEmailChange = { emailError = null },
        onPasswordChange = { passwordError = null }
    )
}

@Composable
fun LoginContent(
    emailError: String?,
    passwordError: String?,
    isLoading: Boolean,
    onLoginClick: (String, String) -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onGoogleLoginClick: () -> Unit,
    onFacebookLoginClick: () -> Unit,
    onXLoginClick: () -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo
            Image(
                painter = painterResource(id = R.drawable.grocerly),
                contentDescription = "Grocerly Logo",
                modifier = Modifier
                    .height(45.dp)
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Email Input Field
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    onEmailChange(it)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(text = "Email", color = Color.Gray) },
                leadingIcon = {
                    Image(
                        painter = painterResource(id = R.drawable.email),
                        contentDescription = "Email Icon",
                        modifier = Modifier.size(20.dp)
                    )
                },
                isError = emailError != null,
                supportingText = {
                    emailError?.let { Text(text = it, color = Color.Red) }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Black,
                    unfocusedBorderColor = Color.LightGray,
                    errorBorderColor = Color.Red,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Password Input Field
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    onPasswordChange(it)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(text = "Password", color = Color.Gray) },
                leadingIcon = {
                    Image(
                        painter = painterResource(id = R.drawable.password),
                        contentDescription = "Password Icon",
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = "Toggle Password Visibility")
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                isError = passwordError != null,
                supportingText = {
                    passwordError?.let { Text(text = it, color = Color.Red) }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Black,
                    unfocusedBorderColor = Color.LightGray,
                    errorBorderColor = Color.Red,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Forgot Password
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "Forgot Password?",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.clickable { onNavigateToForgotPassword() }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Login Button
            Button(
                onClick = { onLoginClick(email, password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2E7D32),
                    contentColor = Color.White
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Login",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Create Account Link
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Don't have an account?",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Sign Up",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.clickable { onNavigateToSignUp() }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Social Login Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Google Button
                Image(
                    painter = painterResource(id = R.drawable.google),
                    contentDescription = "Google Sign In",
                    modifier = Modifier
                        .size(42.dp)
                        .clickable { onGoogleLoginClick() }
                )

                // Facebook Button
                Image(
                    painter = painterResource(id = R.drawable.facebook),
                    contentDescription = "Facebook Sign In",
                    modifier = Modifier
                        .size(42.dp)
                        .clickable { onFacebookLoginClick() }
                )


                Image(
                    painter = painterResource(id = R.drawable.twitter),
                    contentDescription = "X Sign In",
                    modifier = Modifier
                        .size(42.dp)
                        .clickable { onXLoginClick() }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginContent(
        emailError = null,
        passwordError = null,
        isLoading = false,
        onLoginClick = { _, _ -> },
        onNavigateToSignUp = {},
        onNavigateToForgotPassword = {},
        onGoogleLoginClick = {},
        onFacebookLoginClick = {},
        onXLoginClick = {},
        onEmailChange = {},
        onPasswordChange = {}
    )
}
