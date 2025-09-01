package com.luutran.mycookingapp.ui.auth

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead // Using this icon
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.luutran.mycookingapp.navigation.NavDestinations // Ensure this path is correct
import com.luutran.mycookingapp.ui.auth.AuthUiState
import com.luutran.mycookingapp.ui.auth.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailVerificationRequiredScreen(
    modifier: Modifier,
    authViewModel: AuthViewModel,
    navController: NavController,
    featureName: String,
    onNavigateUp: (() -> Unit)? = null
) {
    val authScreenUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    if (onNavigateUp != null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Email Verification") },
                    navigationIcon = {
                    }
                )
            }
        ) { paddingValues ->
            VerificationContent(
                modifier = Modifier.padding(paddingValues),
                authViewModel = authViewModel,
                navController = navController,
                featureName = featureName,
                authScreenUiState = authScreenUiState
            )
        }
    } else {
        VerificationContent(
            authViewModel = authViewModel,
            navController = navController,
            featureName = featureName,
            authScreenUiState = authScreenUiState
        )
    }
}

@Composable
private fun VerificationContent(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel,
    navController: NavController,
    featureName: String,
    authScreenUiState: AuthUiState
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.MarkEmailRead,
            contentDescription = "Email Verification Required",
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Verify Your Email",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Please verify your email address to access and manage your $featureName.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { navController.navigate(NavDestinations.VERIFY_EMAIL_SCREEN) },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Go to Email Verification")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = { authViewModel.resendVerificationEmail() },
            enabled = authScreenUiState !is AuthUiState.Loading,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text(if (authScreenUiState is AuthUiState.Loading) "Sending Verification..." else "Resend Verification Email")
        }

        // Handle showing a toast for resend status
        LaunchedEffect(authScreenUiState) {
            when (authScreenUiState) {
                is AuthUiState.VerificationEmailSent -> {
                    Log.d("VerificationRequired", "Verification email sent successfully.")
                }
                is AuthUiState.Error -> {
                    Log.e("VerificationRequired", "Error with auth state: ${authScreenUiState.message}")
                }
                else -> {}
            }
        }
    }
}