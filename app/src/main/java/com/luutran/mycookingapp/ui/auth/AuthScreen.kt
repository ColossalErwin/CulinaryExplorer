package com.luutran.mycookingapp.ui.auth

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToVerifyEmail: () -> Unit,
    onNavigateToForgotPassword: (email: String?) -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    val navigationEvent by authViewModel.navigationEvent.collectAsState()

    val context = LocalContext.current
    context as? Activity

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUpMode by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Handle navigation events
    LaunchedEffect(navigationEvent) {
        Log.d("AuthScreen", "Navigation event received: $navigationEvent")
        when (val event = navigationEvent) {
            NavigationEvent.NavigateToHome -> {
                Log.d("AuthScreen", "Navigating to Home")
                onNavigateToHome()
                authViewModel.navigationEventConsumed() // Reset the event
            }
            NavigationEvent.NavigateToVerifyEmail -> {
                Log.d("AuthScreen", "Navigating to Verify Email")
                onNavigateToVerifyEmail()
                authViewModel.navigationEventConsumed() // Reset the event
            }
            NavigationEvent.None -> {
                Log.d("AuthScreen", "Navigation event is None")
            }

            is NavigationEvent.NavigateToForgotPassword -> { // <-- HANDLE new event
                Log.d("AuthScreen", "Navigating to Forgot Password with email: ${event.email}")
                onNavigateToForgotPassword(event.email) // Pass the email
                authViewModel.navigationEventConsumed()
            }
            NavigationEvent.NavigateBackToAuth -> {
                Log.d("AuthScreen", "Navigation event NavigateBackToAuth received and consumed.")
                authViewModel.navigationEventConsumed()
            }
        }
    }

    // Handle UI state changes
    LaunchedEffect(uiState) {
        Log.d("AuthScreen", "UI State changed: $uiState")
        when (val currentState = uiState) {
            is AuthUiState.Error -> {
                Toast.makeText(context, currentState.message, Toast.LENGTH_LONG).show()
                Log.e("AuthScreen", "Auth Error: ${currentState.message}")
            }
            is AuthUiState.Success -> {
                Log.i("AuthScreen", "Auth Success for user: ${currentState.user.email}, Verified: ${currentState.isEmailVerified}")
            }
            is AuthUiState.VerificationEmailSent -> {
                Toast.makeText(context, "Verification email sent. Please check your inbox.", Toast.LENGTH_LONG).show()
                Log.i("AuthScreen", "Verification email sent successfully.")
            }
            AuthUiState.Loading -> Log.d("AuthScreen", "UI State is Loading")
            AuthUiState.Idle -> Log.d("AuthScreen", "UI State is Idle")
            is AuthUiState.PasswordResetEmailSent -> {
                Log.i("AuthScreen", "PasswordResetEmailSent state observed (likely from previous screen).")
            }
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isSignUpMode) "Create Account" else "Sign In") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isSignUpMode) "Create a New Account" else "Welcome Back!",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trim() },
                label = { Text("Email Address") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = {
                    focusManager.moveFocus(FocusDirection.Down)
                }),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    if (isSignUpMode) {
                        authViewModel.createAccountWithEmailPassword(email, password)
                    } else {
                        authViewModel.signInWithEmailPassword(email, password)
                    }
                }),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Forgot Password TextButton
            if (!isSignUpMode) {
                TextButton(
                    onClick = {
                        authViewModel.prepareNavigateToForgotPassword(email.ifBlank { null })
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Forgot Password?")
                }
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (uiState is AuthUiState.Loading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    if (isSignUpMode) {
                        authViewModel.createAccountWithEmailPassword(email, password)
                    } else {
                        authViewModel.signInWithEmailPassword(email, password)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is AuthUiState.Loading && email.isNotBlank() && password.isNotBlank()
            ) {
                Text(if (isSignUpMode) "Sign Up" else "Sign In")
            }
            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = { isSignUpMode = !isSignUpMode },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSignUpMode) "Already have an account? Sign In" else "Don't have an account? Sign Up")
            }
        }
    }
}
