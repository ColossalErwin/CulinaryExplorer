package com.luutran.mycookingapp.ui.editprofile

import android.net.Uri
import android.util.Log
import android.widget.Toast // For simple feedback
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PhotoCamera // For image picker button
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.luutran.mycookingapp.R
import com.luutran.mycookingapp.data.utils.DateUtils
import com.luutran.mycookingapp.navigation.NavDestinations
import com.luutran.mycookingapp.ui.auth.AuthUiState
import com.luutran.mycookingapp.ui.auth.AuthViewModel
import com.luutran.mycookingapp.ui.auth.EmailVerificationRequiredScreen
import com.luutran.mycookingapp.ui.theme.LightGreenApp
import com.luutran.mycookingapp.ui.theme.OnLightGreenApp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    authViewModel: AuthViewModel,
    profileViewModel: ProfileViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    navController: NavController,
) {
    val context = LocalContext.current
    val profileLoadState by profileViewModel.profileLoadState.collectAsState()
    val profileUpdateState by profileViewModel.profileUpdateState.collectAsState()

    // Auth State from AuthViewModel
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()

    var displayName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var currentPhotoUrl by remember { mutableStateOf<String?>(null) } // Displays current/newly uploaded URL

    // This will hold the Uri of the image selected by the user from their gallery
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    var accountCreatedAt by remember { mutableStateOf<com.google.firebase.Timestamp?>(null) } // Store createdAt

    // ActivityResultLauncher for picking an image
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                selectedImageUri = it // Store the selected image Uri
                Log.d("EditProfileScreen", "Image selected: $it")
            }
        }
    )

    // Effect to populate fields when profile data loads
    LaunchedEffect(profileLoadState) {
        if (profileLoadState is ProfileLoadState.Success) {
            val userProfile = (profileLoadState as ProfileLoadState.Success).userProfile // Now UserProfile type
            displayName = userProfile.displayName ?: ""
            phoneNumber = userProfile.phoneNumber ?: ""
            currentPhotoUrl = userProfile.photoUrl
            accountCreatedAt = userProfile.createdAt // <-- Get createdAt
            selectedImageUri = null
            Log.d("EditProfileScreen", "Profile loaded into fields: $userProfile")
        }
    }
    /*
    LaunchedEffect(profileLoadState) {
        if (profileLoadState is ProfileLoadState.Success) {
            val profile = (profileLoadState as ProfileLoadState.Success).userProfile
            displayName = profile.displayName ?: ""
            phoneNumber = profile.phoneNumber ?: ""
            currentPhotoUrl = profile.photoUrl // Load initial photo URL
            selectedImageUri = null // Reset selected image if profile reloads
            Log.d("EditProfileScreen", "Profile loaded into fields: $profile")
        }
    }
    */

    // Effect to handle update results
    LaunchedEffect(profileUpdateState) {
        when (val state = profileUpdateState) { // Give a name to the state
            is ProfileUpdateState.Success -> {
                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                profileViewModel.resetUpdateState()
                selectedImageUri = null // Clear selected image after successful save
                // `loadUserProfile` is called in ViewModel, so fields will refresh.
                // Consider if onNavigateBack should happen here or after user explicitly clicks back.
                // For now, let's assume user stays on screen to see changes.
                // onNavigateBack()
            }
            is ProfileUpdateState.Error -> {
                Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                profileViewModel.resetUpdateState()
            }
            is ProfileUpdateState.ImageUploading -> {
                Toast.makeText(context, "Uploading image...", Toast.LENGTH_SHORT).show()
            }
            is ProfileUpdateState.ImageUploadSuccess -> {
                Log.d("EditProfileScreen", "Image uploaded, URL: ${state.downloadUrl}. Profile save pending.")
            }
            ProfileUpdateState.Idle -> Unit
            ProfileUpdateState.Loading -> {
                Toast.makeText(context, "Saving profile...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Determine if email verification is required based on AuthUiState
    val (userForVerificationCheck, isEmailVerifiedForVerificationCheck) = when (authUiState) {
        is AuthUiState.Success -> Pair((authUiState as AuthUiState.Success).user, (authUiState as AuthUiState.Success).isEmailVerified)
        else -> Pair(null, false) // Default to no user/not verified if not in Success state
    }

    val needsVerification = userForVerificationCheck != null &&
            !isEmailVerifiedForVerificationCheck &&
            userForVerificationCheck.providerData.any { it.providerId == "password" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LightGreenApp,
                    titleContentColor = OnLightGreenApp,
                    navigationIconContentColor = OnLightGreenApp,
                    actionIconContentColor = OnLightGreenApp
                ),
                actions = {
                    // Show refresh only if user is successfully loaded and verification is not pending
                    if (authUiState is AuthUiState.Success && !needsVerification) {
                        IconButton(onClick = {
                            profileViewModel.refreshUserProfile()
                            Toast.makeText(context, "Refreshing profile...", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Filled.Refresh, "Refresh Profile")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        when (val currentAuthUiState = authUiState) { // Give a name to the collected state
            is AuthUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                    Text("Loading user data...")
                }
            }
            is AuthUiState.Success -> {
                // User is successfully loaded (could be verified or not, social or email/pass)
                if (needsVerification) {
                    // This specific user (email/pass) needs to verify their email
                    EmailVerificationRequiredScreen(
                        modifier = Modifier.padding(innerPadding).fillMaxSize(),
                        authViewModel = authViewModel,
                        navController = navController,
                        featureName = "profile"
                    )
                } else {
                    // User is verified, or social login, or email/pass but doesn't need verification screen here.
                    // Proceed to show the profile editing form or its loading/error states.
                    when (profileLoadState) {
                        is ProfileLoadState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        is ProfileLoadState.Success -> {
                            // val currentProfile = (profileLoadState as ProfileLoadState.Success).userProfile // Already used in LaunchedEffect

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    Image(
                                        painter = rememberAsyncImagePainter(
                                            ImageRequest.Builder(LocalContext.current)
                                                // Show selected local image URI first for immediate preview,
                                                // then fallback to currentPhotoUrl (from Firebase)
                                                .data(data = selectedImageUri ?: currentPhotoUrl ?: R.drawable.ic_launcher_foreground)
                                                .placeholder(R.drawable.ic_launcher_foreground)
                                                .error(R.drawable.ic_launcher_foreground)
                                                .crossfade(true)
                                                .build()
                                        ),
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable {
                                                imagePickerLauncher.launch("image/*") // Launch image picker
                                            },
                                        contentScale = ContentScale.Crop
                                    )
                                    FloatingActionButton( // Small FAB for changing photo
                                        onClick = { imagePickerLauncher.launch("image/*") },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .offset(x = (8).dp, y = (8).dp), // Adjust offset as needed
                                        shape = CircleShape,
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ) {
                                        Icon(
                                            Icons.Filled.PhotoCamera,
                                            contentDescription = "Change Profile Photo",
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                                    Text("Change Photo")
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = displayName,
                                    onValueChange = { displayName = it },
                                    label = { Text("Display Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = (profileLoadState as ProfileLoadState.Success).userProfile.email ?: "No email",
                                    onValueChange = {},
                                    label = { Text("Email (Read-only)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    readOnly = true,
                                    enabled = false
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = phoneNumber,
                                    onValueChange = { phoneNumber = it },
                                    label = { Text("Phone Number (Optional)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Phone,
                                        imeAction = ImeAction.Done
                                    ),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Display Account Creation Date
                                OutlinedTextField(
                                    value = DateUtils.formatTimestamp(accountCreatedAt, "MMM dd, yyyy"), // Use DateUtils
                                    onValueChange = {},
                                    label = { Text("Account Created Date") },
                                    modifier = Modifier.fillMaxWidth(),
                                    readOnly = true,
                                    enabled = false,
                                    leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = "Creation Date") }
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = {
                                        if (selectedImageUri != null) {
                                            // If a new image is selected, upload it first
                                            profileViewModel.uploadProfileImageAndUpdateProfile(
                                                imageUri = selectedImageUri!!,
                                                currentDisplayName = displayName.takeIf { it.isNotBlank() },
                                                currentPhoneNumber = phoneNumber.takeIf { it.isNotBlank() }
                                            )
                                        } else {
                                            // Otherwise, just update text fields
                                            profileViewModel.updateUserProfile(
                                                newDisplayName = displayName.takeIf { it.isNotBlank() },
                                                newPhotoUrlString = currentPhotoUrl, // Pass the existing URL if no new image
                                                newPhoneNumber = phoneNumber.takeIf { it.isNotBlank() }
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = profileUpdateState !is ProfileUpdateState.Loading && profileUpdateState !is ProfileUpdateState.ImageUploading
                                ) {
                                    when (profileUpdateState) {
                                        is ProfileUpdateState.Loading, is ProfileUpdateState.ImageUploading -> {
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                                        }
                                        else -> Text("Save Changes")
                                    }
                                }
                            }
                        }
                        is ProfileLoadState.Error -> {
                            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                                Text("Error: ${(profileLoadState as ProfileLoadState.Error).message}")
                            }
                        }
                        ProfileLoadState.Idle -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {}
                        }
                    }
                }
            }

            is AuthUiState.Idle, is AuthUiState.Error, is AuthUiState.VerificationEmailSent, is AuthUiState.PasswordResetEmailSent -> {
                // Handle states where the user is not in a 'Success' (logged-in) state
                // or if there's an error.
                // VerificationEmailSent might briefly appear; if so, EmailVerificationRequiredScreen should handle it.
                // For Idle or Error, prompt to sign in.
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            when (currentAuthUiState) {
                                is AuthUiState.Error -> "Authentication error: ${currentAuthUiState.message}"
                                AuthUiState.Idle -> "You need to be signed in to edit your profile."
                                AuthUiState.VerificationEmailSent -> "Verification email sent. Please check your inbox." // Or let EmailVerificationScreen handle this
                                else -> "Please sign in." // Should not happen with current states
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Only show sign-in button if not in VerificationEmailSent or specific error states
                        if (currentAuthUiState is AuthUiState.Idle || currentAuthUiState is AuthUiState.Error) {
                            Button(onClick = {
                                navController.navigate(NavDestinations.AUTH_SCREEN) {
                                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }) {
                                Text("Sign In")
                            }
                        }
                    }
                }
            }
        }
    }
}