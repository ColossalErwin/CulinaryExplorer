package com.luutran.mycookingapp.ui.auth

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyEmailScreen(
    authViewModel: AuthViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToAuth: () -> Unit // In case user wants to sign out from here
) {
    val uiState by authViewModel.uiState.collectAsState()
    val navigationEvent by authViewModel.navigationEvent.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(navigationEvent) {
        when (navigationEvent) {
            NavigationEvent.NavigateToHome -> {
                Toast.makeText(context, "Email verified successfully!", Toast.LENGTH_SHORT).show()
                onNavigateToHome()
                authViewModel.navigationEventConsumed()
            }
            // Other navigation events can be handled if needed from this screen
            else -> {}
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.VerificationEmailSent) {
            Toast.makeText(context, "Verification email sent.", Toast.LENGTH_SHORT).show()
        }
        if (uiState is AuthUiState.Error) {
            Toast.makeText(context, (uiState as AuthUiState.Error).message, Toast.LENGTH_LONG).show()
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Verify Your Email") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "A verification link has been sent to:",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = currentUser?.email ?: "your email address.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Please check your inbox (and spam folder) and click the link to activate your account.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { authViewModel.reloadUserAndCheckVerification() },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is AuthUiState.Loading
            ) {
                Text("I've Verified My Email (Refresh)")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { authViewModel.resendVerificationEmail() },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is AuthUiState.Loading && uiState !is AuthUiState.VerificationEmailSent
            ) {
                Text("Resend Verification Email")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    authViewModel.signOut()
                    onNavigateToAuth() // Navigate back to AuthScreen after sign out
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign Out")
            }

            if (uiState is AuthUiState.Loading) {
                Spacer(modifier = Modifier.height(24.dp))
                CircularProgressIndicator()
            }
        }
    }
}