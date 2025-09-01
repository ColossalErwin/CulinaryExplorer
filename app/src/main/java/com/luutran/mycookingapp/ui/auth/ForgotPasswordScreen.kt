package com.luutran.mycookingapp.ui.auth

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    authViewModel: AuthViewModel,
    initialEmail: String?, // Email passed from AuthScreen
    onNavigateBackToAuth: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    val navigationEvent by authViewModel.navigationEvent.collectAsState() // Observe for back navigation if needed

    val context = LocalContext.current
    var email by remember { mutableStateOf(initialEmail ?: "") }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var emailSentMessageVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        Log.d("ForgotPasswordScreen", "UI State changed: $uiState")
        when (val currentState = uiState) {
            is AuthUiState.Error -> {
                Toast.makeText(context, currentState.message, Toast.LENGTH_LONG).show()
                emailSentMessageVisible = false // Hide success message on new error
            }
            AuthUiState.PasswordResetEmailSent -> {
                Toast.makeText(context, "If an account with this email exists, a password reset link will be sent shortly.", Toast.LENGTH_LONG).show()
                emailSentMessageVisible = true // Show success message
                keyboardController?.hide()
                focusManager.clearFocus()
            }
            else -> {
                // If loading starts or state becomes idle, hide previous success message
                if (currentState is AuthUiState.Loading || currentState is AuthUiState.Idle) {
                    emailSentMessageVisible = false
                }
            }
        }
    }

    LaunchedEffect(navigationEvent) {
        if (navigationEvent == NavigationEvent.NavigateBackToAuth) {
            onNavigateBackToAuth()
            authViewModel.navigationEventConsumed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reset Password") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBackToAuth) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Sign In")
                    }
                }
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
                text = "Enter your account's email address and we will send you a link to reset your password.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it.trim()
                    if (emailSentMessageVisible) emailSentMessageVisible = false // Hide message if user types again
                },
                label = { Text("Email Address") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    if (email.isNotBlank()) {
                        authViewModel.requestPasswordReset(email)
                    } else {
                        Toast.makeText(context, "Please enter your email address.", Toast.LENGTH_SHORT).show()
                    }
                }),
                modifier = Modifier.fillMaxWidth(),
                isError = uiState is AuthUiState.Error && (uiState as AuthUiState.Error).message.contains("email", ignoreCase = true)
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (uiState is AuthUiState.Loading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    authViewModel.requestPasswordReset(email)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is AuthUiState.Loading && email.isNotBlank() && !emailSentMessageVisible
            ) {
                Text("Send Reset Link")
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (emailSentMessageVisible) {
                Text(
                    text = "Password reset instructions have been sent to your email address (if your account exists). Please check your inbox (and spam folder).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary, // Or a success color
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button( // Button to go back to login after message is shown
                    onClick = onNavigateBackToAuth,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back to Sign In")
                }
            } else {
                TextButton(onClick = onNavigateBackToAuth) {
                    Text("Back to Sign In")
                }
            }
        }
    }
}