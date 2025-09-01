package com.luutran.mycookingapp.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.luutran.mycookingapp.R // Make sure R is imported if you use it for string resources
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.Credential
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential


sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data class Success(val user: FirebaseUser, val isEmailVerified: Boolean) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
    data object VerificationEmailSent : AuthUiState()
    data object PasswordResetEmailSent : AuthUiState()
}

// To signal navigation events specifically for verification
sealed class NavigationEvent {
    data object NavigateToHome : NavigationEvent()
    data object NavigateToVerifyEmail : NavigationEvent()
    data object None : NavigationEvent() // No navigation event
    data class NavigateToForgotPassword(val email: String? = null) : NavigationEvent()
    data object NavigateBackToAuth : NavigationEvent()
}

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Consolidated navigation signal
    private val _navigationEvent = MutableStateFlow<NavigationEvent>(NavigationEvent.None)
    val navigationEvent: StateFlow<NavigationEvent> = _navigationEvent.asStateFlow()

    private val _isUserAuthenticated = MutableStateFlow<Boolean?>(null)
    val isUserAuthenticated: StateFlow<Boolean?> = _isUserAuthenticated.asStateFlow()

    private val _isEmailVerified = MutableStateFlow<Boolean>(false)
    val isEmailVerified: StateFlow<Boolean> = _isEmailVerified.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()


    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        _currentUser.value = user
        _isUserAuthenticated.value = user != null
        _isEmailVerified.value = user?.isEmailVerified ?: false

        if (user != null) {
            // Update UiState with verification status
            if (_uiState.value !is AuthUiState.Success || (_uiState.value as? AuthUiState.Success)?.user?.uid != user.uid || (_uiState.value as? AuthUiState.Success)?.isEmailVerified != user.isEmailVerified) {
                _uiState.value = AuthUiState.Success(user, user.isEmailVerified)
            }
            Log.d("AuthViewModel", "AuthStateListener: User authenticated = true. Email Verified: ${user.isEmailVerified}. User: ${user.uid}")
        } else {
            if (_uiState.value !is AuthUiState.Idle) { // Reset if not already idle
                _uiState.value = AuthUiState.Idle
            }
            Log.d("AuthViewModel", "AuthStateListener: User authenticated = false.")
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
        val initialUser = auth.currentUser
        if (initialUser != null) {
            _currentUser.value = initialUser
            _isUserAuthenticated.value = true
            _isEmailVerified.value = initialUser.isEmailVerified
            _uiState.value = AuthUiState.Success(initialUser, initialUser.isEmailVerified)
            Log.d("AuthViewModel", "Init: User already logged in. Email Verified: ${initialUser.isEmailVerified}")
        } else {
            _isUserAuthenticated.value = false // Explicitly set to false if no initial user
            Log.d("AuthViewModel", "Init: No user logged in.")
        }
        Log.d("AuthViewModel", "Init: AuthStateListener added.")
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
    }

    fun navigationEventConsumed() {
        _navigationEvent.value = NavigationEvent.None
    }

    fun prepareNavigateToForgotPassword(email: String?) {
        _navigationEvent.value = NavigationEvent.NavigateToForgotPassword(email)
    }

    // Password Reset
    fun requestPasswordReset(email: String) {
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.value = AuthUiState.Error("Please enter a valid email address.")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("AuthViewModel", "Password reset email sent successfully to $email (if account exists).")
                        _uiState.value = AuthUiState.PasswordResetEmailSent
                    } else {
                        Log.e("AuthViewModel", "Failed to send password reset email.", task.exception)
                        val errorMessage = when (task.exception) {
                            is FirebaseAuthInvalidUserException -> {
                                "If an account exists for $email, a reset link has been sent."
                            }
                            is FirebaseAuthInvalidCredentialsException -> "The email address is badly formatted." // e.g. ERROR_INVALID_EMAIL
                            else -> task.exception?.message ?: "Failed to send password reset email. Please try again."
                        }
                        _uiState.value = AuthUiState.Error(errorMessage)
                    }
                }
        }
    }

    fun signInWithEmailPassword(email: String, password: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser // User will be updated by listener
                        if (user != null) {
                            // Listener will update _uiState, _isEmailVerified
                            // _uiState.value = AuthUiState.Success(user, user.isEmailVerified) // Handled by listener
                            if (user.isEmailVerified) {
                                _navigationEvent.value = NavigationEvent.NavigateToHome
                            } else {
                            }
                            Log.d("AuthViewModel", "Email/Pass Sign-In Success: ${user.email}, Verified: ${user.isEmailVerified}")
                        } else {
                            _uiState.value = AuthUiState.Error("Login successful but user data is null.")
                            Log.e("AuthViewModel", "Email/Pass Sign-In Success but user is null")
                        }
                    } else {
                        handleAuthFailure(task.exception, "Sign-in failed.")
                    }
                }
        }
    }

    fun createAccountWithEmailPassword(email: String, password: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            sendVerificationEmail(user, true) // Send email, then navigate to VerifyEmail
                            // Listener will update _uiState to Success
                            // _uiState.value = AuthUiState.Success(user, user.isEmailVerified) // Handled by listener
                            Log.d("AuthViewModel", "Account Creation Success: ${user.email}")
                        } else {
                            _uiState.value = AuthUiState.Error("Account creation successful but user data is null.")
                            Log.e("AuthViewModel", "Sign-Up Success but user is null")
                        }
                    } else {
                        handleAuthFailure(task.exception, "Sign-up failed.")
                    }
                }
        }
    }

    private fun sendVerificationEmail(user: FirebaseUser, navigateToVerifyOnSuccess: Boolean = false) {
        user.sendEmailVerification()
            .addOnCompleteListener { verificationTask ->
                if (verificationTask.isSuccessful) {
                    Log.d("AuthViewModel", "Verification email sent to ${user.email}")
                    _uiState.value = AuthUiState.VerificationEmailSent // Inform UI
                    if (navigateToVerifyOnSuccess) {
                        _navigationEvent.value = NavigationEvent.NavigateToVerifyEmail
                    }
                } else {
                    Log.e("AuthViewModel", "Failed to send verification email.", verificationTask.exception)
                    // Potentially update UI state with this specific error
                    _uiState.value = AuthUiState.Error("Failed to send verification email: ${verificationTask.exception?.message}")
                }
            }
    }


    fun resendVerificationEmail() {
        val user = auth.currentUser
        if (user != null) {
            if (!user.isEmailVerified) {
                _uiState.value = AuthUiState.Loading // Or a more specific "SendingEmail" state
                sendVerificationEmail(user) // Reuse the helper
            } else {
                _uiState.value = AuthUiState.Error("Your email is already verified.")
                // Re-set to success if already verified
                _uiState.value = AuthUiState.Success(user, true)
            }
        } else {
            _uiState.value = AuthUiState.Error("No user logged in to resend verification email.")
        }
    }

    // Call this when the user indicates they have verified (e.g. from VerifyEmailScreen)
    // or when the app comes to the foreground after user might have verified via link.
    fun reloadUserAndCheckVerification() {
        val user = auth.currentUser
        if (user != null) {
            _uiState.value = AuthUiState.Loading
            user.reload().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val updatedUser = auth.currentUser // fetch the latest user state
                    _currentUser.value = updatedUser // Ensure our local copy is updated
                    val isVerified = updatedUser?.isEmailVerified ?: false
                    _isEmailVerified.value = isVerified // Update the specific flow

                    if (updatedUser != null) {
                        _uiState.value = AuthUiState.Success(updatedUser, isVerified)
                        if (isVerified) {
                            // If verified now, navigate to home
                            _navigationEvent.value = NavigationEvent.NavigateToHome
                            Log.d("AuthViewModel", "User reload success. Email IS verified.")
                        } else {
                            // Still not verified, UI might show a message.
                            // Could also navigate to VerifyEmailScreen if coming from a specific action.
                            Log.d("AuthViewModel", "User reload success. Email still NOT verified.")
                        }
                    } else {
                        _uiState.value = AuthUiState.Idle // User became null after reload
                        _isUserAuthenticated.value = false
                        Log.d("AuthViewModel", "User reload resulted in null user.")
                    }
                } else {
                    Log.e("AuthViewModel", "Failed to reload user.", task.exception)
                    // If reload fails, reflect current known state or error
                    _uiState.value = AuthUiState.Error("Failed to update verification status: ${task.exception?.message}")
                    // Fallback to current known state from listener
                    auth.currentUser?.let {
                        _uiState.value = AuthUiState.Success(it, it.isEmailVerified)
                    } ?: run {
                        _uiState.value = AuthUiState.Idle
                    }
                }
            }
        } else {
            Log.d("AuthViewModel", "reloadUserAndCheckVerification: No user to reload.")
            _uiState.value = AuthUiState.Idle // Should align with listener
            _isUserAuthenticated.value = false
            _isEmailVerified.value = false
        }
    }


    private fun handleAuthFailure(exception: Exception?, defaultMessage: String) {
        val errorMessage = when (exception) {
            is FirebaseAuthInvalidUserException -> "No account found with this email."
            is FirebaseAuthInvalidCredentialsException -> "Incorrect password or email format."
            is FirebaseAuthUserCollisionException -> "An account already exists with this email address."
            else -> exception?.message ?: defaultMessage
        }
        _uiState.value = AuthUiState.Error(errorMessage)
        Log.e("AuthViewModel", "$defaultMessage Error", exception)
    }

    fun signInWithGoogle(activity: Activity, context: Context) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val credentialManager = CredentialManager.create(context)
            val serverClientId = try {
                context.getString(R.string.default_web_client_id)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "R.string.default_web_client_id not found", e)
                _uiState.value = AuthUiState.Error("Google Sign-In configuration error (client ID missing).")
                return@launch
            }

            Log.d("AuthViewModel", "Google Sign-In: Attempting with filterByAuthorizedAccounts = false")
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            try {
                val result = credentialManager.getCredential(activity, request)
                Log.d("AuthViewModel", "Google Sign-In: Credential retrieved successfully.")
                handleGoogleSignInResult(result.credential)
            } catch (e: GetCredentialException) {
                Log.e("AuthViewModel", "Google Sign-In GetCredentialException", e)
                val errorMessage = when (e) {
                    is GetCredentialCancellationException -> "Sign-in cancelled by user."
                    is NoCredentialException -> "No Google account found or selected for sign-in. Please ensure Google accounts are on your device and Play Services is up to date."
                    else -> "An error occurred during Google Sign-In: ${e.message ?: e.javaClass.simpleName}"
                }
                _uiState.value = AuthUiState.Error(errorMessage)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google Sign-In General Exception", e)
                _uiState.value = AuthUiState.Error("An unexpected error occurred during Google Sign-In: ${e.localizedMessage}")
            }
        }
    }


    private fun handleGoogleSignInResult(credential: Credential) {
        if (credential is GoogleIdTokenCredential) {
            val googleIdToken = credential.idToken
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
            signInToFirebase(firebaseCredential)
        } else {
            _uiState.value = AuthUiState.Error("Unexpected credential type received from Google Sign-In.")
            Log.e("AuthViewModel", "Unexpected credential type: ${credential.type}")
        }
    }

    private fun signInToFirebase(credential: AuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser // Listener will update state
                    if (user != null) {
                        if (user.isEmailVerified) {
                            _navigationEvent.value = NavigationEvent.NavigateToHome
                        } else {
                            // This case is less common for Google, but good to handle
                            sendVerificationEmail(user, true)
                        }
                        Log.d("AuthViewModel", "Firebase sign-in with Google success: ${user.email}, Verified: ${user.isEmailVerified}")
                    } else {
                        _uiState.value = AuthUiState.Error("Firebase user is null after successful Google sign-in.")
                        Log.e("AuthViewModel", "Firebase user null after Google sign-in success")
                    }
                } else {
                    handleAuthFailure(task.exception, "Firebase sign-in with Google failed.")
                }
            }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                auth.signOut()
                _navigationEvent.value = NavigationEvent.None // Explicitly stop any pending navigation
                Log.d("AuthViewModel", "User signed out successfully.")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error signing out", e)
                _uiState.value = AuthUiState.Error("Error signing out: ${e.message}")
            }
        }
    }
}