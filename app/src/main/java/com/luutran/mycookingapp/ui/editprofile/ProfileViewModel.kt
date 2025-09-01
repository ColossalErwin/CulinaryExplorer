package com.luutran.mycookingapp.ui.editprofile

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
// Import Firebase Storage
import com.google.firebase.storage.FirebaseStorage // ADD THIS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID // For generating unique file names
import androidx.core.net.toUri
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.Timestamp

data class UserProfile(
    val uid: String = "",
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val phoneNumber: String? = null,
    @ServerTimestamp val lastUpdated: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    constructor() : this("", "", null, null, null, null, null)
}

data class CombinedUserProfile(
    val uid: String = "",
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val phoneNumber: String? = null,
)

sealed class ProfileLoadState {
    data object Idle : ProfileLoadState()
    data object Loading : ProfileLoadState()
    data class Success(val userProfile: UserProfile) : ProfileLoadState()
    data class Error(val message: String) : ProfileLoadState()
}

sealed class ProfileUpdateState {
    data object Idle : ProfileUpdateState()
    data object Loading : ProfileUpdateState()
    data object Success : ProfileUpdateState()
    data class Error(val message: String) : ProfileUpdateState()
    data object ImageUploading : ProfileUpdateState()
    data class ImageUploadSuccess(val downloadUrl: String) : ProfileUpdateState()
}


class ProfileViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) : ViewModel() {

    private val _profileLoadState = MutableStateFlow<ProfileLoadState>(ProfileLoadState.Idle)
    val profileLoadState: StateFlow<ProfileLoadState> = _profileLoadState.asStateFlow()

    private val _profileUpdateState = MutableStateFlow<ProfileUpdateState>(ProfileUpdateState.Idle)
    val profileUpdateState: StateFlow<ProfileUpdateState> = _profileUpdateState.asStateFlow()

    private val currentUser: FirebaseUser? get() = auth.currentUser

    init {
        loadUserProfile()
    }

    fun refreshUserProfile() {
        Log.d("ProfileViewModel", "refreshUserProfile called explicitly.")
        loadUserProfile()
    }


    fun loadUserProfile() {
        val firebaseUser = currentUser
        if (firebaseUser == null) {
            _profileLoadState.value = ProfileLoadState.Error("User not logged in.")
            return
        }
        // ... (UID blank check) ...

        _profileLoadState.value = ProfileLoadState.Loading
        viewModelScope.launch {
            try {
                firebaseUser.reload().await()
                Log.d("ProfileViewModel", "FirebaseUser reloaded in loadUserProfile.")

                val userDocRef = db.collection("users").document(firebaseUser.uid)
                val firestoreDocument = userDocRef.get().await()
                var firestoreProfile = if (firestoreDocument.exists()) {
                    firestoreDocument.toObject(UserProfile::class.java)
                } else null

                // Ensure a UserProfile object exists to hold combined data,
                // especially for createdAt.
                val effectiveUserProfile = firestoreProfile ?: UserProfile(uid = firebaseUser.uid, email = firebaseUser.email)

                // Combine data, prioritizing Auth, then Firestore
                val finalProfile = effectiveUserProfile.copy(
                    email = firebaseUser.email ?: effectiveUserProfile.email, // Auth email is source of truth
                    displayName = firebaseUser.displayName.takeIf { !it.isNullOrEmpty() }
                        ?: effectiveUserProfile.displayName,
                    photoUrl = firebaseUser.photoUrl?.toString().takeIf { !it.isNullOrEmpty() }
                        ?: effectiveUserProfile.photoUrl
                    // phoneNumber is already from firestoreProfile
                    // createdAt will be from firestoreProfile if it exists, or null if new
                    // lastUpdated will be from firestoreProfile or updated later
                )

                _profileLoadState.value = ProfileLoadState.Success(finalProfile)
                Log.d("ProfileViewModel", "User profile loaded: $finalProfile")

                // Sync logic (ensure Firestore has up-to-date info from Auth and sets timestamps)
                var needsSync = false
                val updateData = mutableMapOf<String, Any?>()

                if (finalProfile.displayName != firestoreProfile?.displayName) {
                    updateData["displayName"] = finalProfile.displayName
                    needsSync = true
                }
                if (finalProfile.photoUrl != firestoreProfile?.photoUrl) {
                    updateData["photoUrl"] = finalProfile.photoUrl
                    needsSync = true
                }
                if (finalProfile.email != firestoreProfile?.email) {
                    updateData["email"] = finalProfile.email // Should always be synced
                    needsSync = true
                }
                if (finalProfile.phoneNumber != firestoreProfile?.phoneNumber) {
                    updateData["phoneNumber"] = finalProfile.phoneNumber // Could be null
                    needsSync = true
                }

                // If firestoreProfile was null (new user document) or if critical fields differ
                if (firestoreProfile == null || needsSync) {
                    updateData["uid"] = finalProfile.uid
                    updateData["lastUpdated"] = FieldValue.serverTimestamp()
                    if (firestoreProfile?.createdAt == null) { // Only set createdAt if it's truly new
                        updateData["createdAt"] = FieldValue.serverTimestamp()
                    }
                    userDocRef.set(updateData, SetOptions.merge()).await()
                    Log.d("ProfileViewModel", "Synced Firestore profile for ${firebaseUser.uid} with $updateData")
                }

            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading profile for UID ${firebaseUser.uid}", e)
                _profileLoadState.value = ProfileLoadState.Error("Failed to load profile: ${e.message}")
            }
        }
    }

    fun uploadProfileImageAndUpdateProfile(
        imageUri: Uri,
        currentDisplayName: String?,
        currentPhoneNumber: String?
    ) {
        val firebaseUser = currentUser
        if (firebaseUser == null || firebaseUser.uid.isBlank()) {
            _profileUpdateState.value = ProfileUpdateState.Error("User not available for image upload.")
            return
        }

        _profileUpdateState.value = ProfileUpdateState.ImageUploading
        viewModelScope.launch {
            var newImageFileName: String? = null // To identify the new image
            try {
                //val userImageFolder = "profile_images/${firebaseUser.uid}"
                //newImageFileName = "profile_pic_${UUID.randomUUID()}.jpg" // Keep track of this
                val userImageFolder = "users/${firebaseUser.uid}/profileImage"
                newImageFileName = "profile_pic_${UUID.randomUUID()}.jpg" // Keep track of this
                val newImageRef = storage.reference.child("$userImageFolder/$newImageFileName")

                newImageRef.putFile(imageUri).await()
                val downloadUrl = newImageRef.downloadUrl.await().toString()
                Log.d("ProfileViewModel", "Image uploaded: $downloadUrl (File: $newImageFileName)")

                _profileUpdateState.value = ProfileUpdateState.ImageUploadSuccess(downloadUrl)

                // Delete old images BEFORE updating the profile with the new URL.
                // If deletion fails partially, the profile update will still proceed with the new image.
                try {
                    val listResult = storage.reference.child(userImageFolder).listAll().await()
                    listResult.items.forEach { item ->
                        if (item.name != newImageFileName) { // Don't delete the new file
                            Log.d("ProfileViewModel", "Deleting old image: ${item.path}")
                            item.delete().await()
                        }
                    }
                    Log.d("ProfileViewModel", "Finished deleting old profile images.")
                } catch (e: Exception) {
                    Log.e("ProfileViewModel", "Error deleting old profile images for UID: ${firebaseUser.uid}", e)
                }

                updateUserProfile(
                    newDisplayName = currentDisplayName,
                    newPhotoUrlString = downloadUrl, // Pass the NEWLY uploaded URL
                    newPhoneNumber = currentPhoneNumber
                )

            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error in image upload/processing for UID: ${firebaseUser.uid}", e)
                _profileUpdateState.value = ProfileUpdateState.Error("Image upload failed: ${e.message}")
            }
        }
    }

    fun updateUserProfile(
        newDisplayName: String?,
        newPhotoUrlString: String?,
        newPhoneNumber: String?
        // Removed oldPhotoUrlForPossibleDeletion as deletion is handled in uploadProfileImageAndUpdateProfile
    ) {
        val firebaseUser = currentUser
        if (firebaseUser == null || firebaseUser.uid.isBlank()) {
            _profileUpdateState.value = ProfileUpdateState.Error("User not available for profile update.")
            return
        }

        if (_profileUpdateState.value !is ProfileUpdateState.ImageUploading &&
            _profileUpdateState.value !is ProfileUpdateState.ImageUploadSuccess) {
            _profileUpdateState.value = ProfileUpdateState.Loading
        }

        viewModelScope.launch {
            try {
                var finalDisplayNameForAuth = firebaseUser.displayName
                var finalPhotoUrlForAuth = firebaseUser.photoUrl?.toString()

                val profileUpdatesBuilder = UserProfileChangeRequest.Builder()
                var authProfileChanged = false

                if (newDisplayName != null && newDisplayName != firebaseUser.displayName) {
                    profileUpdatesBuilder.displayName = newDisplayName
                    finalDisplayNameForAuth = newDisplayName
                    authProfileChanged = true
                }
                if (newPhotoUrlString != null && newPhotoUrlString != firebaseUser.photoUrl?.toString()) {
                    profileUpdatesBuilder.photoUri = newPhotoUrlString.toUri() // Use androidx.core.net.toUri
                    finalPhotoUrlForAuth = newPhotoUrlString
                    authProfileChanged = true
                }

                if (authProfileChanged) {
                    firebaseUser.updateProfile(profileUpdatesBuilder.build()).await()
                    Log.d("ProfileViewModel", "Firebase Auth profile updated.")
                    firebaseUser.reload().await() // CRUCIAL: Reload to get updated Auth values
                    Log.d("ProfileViewModel", "FirebaseUser reloaded after auth update.")
                    // Update final values from the reloaded user for Firestore
                    finalDisplayNameForAuth = firebaseUser.displayName
                    finalPhotoUrlForAuth = firebaseUser.photoUrl?.toString()
                }

                val userDocRef = db.collection("users").document(firebaseUser.uid)
                val firestoreUpdates = mutableMapOf<String, Any?>()

                firestoreUpdates["displayName"] = finalDisplayNameForAuth
                firestoreUpdates["photoUrl"] = finalPhotoUrlForAuth
                firestoreUpdates["email"] = firebaseUser.email // Typically static here

                // Simplified Phone Number Logic
                var firestorePhoneNumberChanged = false
                val currentFirestoreProfile = try { userDocRef.get().await().toObject(UserProfile::class.java) } catch (e: Exception) { null }
                val existingFirestorePhoneNumber = currentFirestoreProfile?.phoneNumber

                if (newPhoneNumber != existingFirestorePhoneNumber) {
                    firestoreUpdates["phoneNumber"] = newPhoneNumber // Handles add, change, or remove (if newPhoneNumber is null)
                    firestorePhoneNumberChanged = true
                }

                firestoreUpdates["lastUpdated"] = FieldValue.serverTimestamp()

                val needsFirestoreUpdate = authProfileChanged ||
                        firestorePhoneNumberChanged ||
                        finalDisplayNameForAuth != currentFirestoreProfile?.displayName ||
                        finalPhotoUrlForAuth != currentFirestoreProfile?.photoUrl

                if (needsFirestoreUpdate) {
                    if (currentFirestoreProfile?.createdAt == null) {
                        firestoreUpdates["createdAt"] = FieldValue.serverTimestamp()
                    }
                    firestoreUpdates.putIfAbsent("uid", firebaseUser.uid)
                    userDocRef.set(firestoreUpdates, SetOptions.merge()).await()
                    Log.d("ProfileViewModel", "Firestore profile updated for UID: ${firebaseUser.uid} with: $firestoreUpdates")
                } else {
                    Log.d("ProfileViewModel", "No new changes for Firestore for UID: ${firebaseUser.uid}")
                }

                _profileUpdateState.value = ProfileUpdateState.Success
                loadUserProfile() // Refresh UI

            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error updating profile for UID: ${firebaseUser.uid}", e)
                _profileUpdateState.value = ProfileUpdateState.Error("Profile update failed: ${e.message}")
            }
        }
    }

    fun resetUpdateState() {
        _profileUpdateState.value = ProfileUpdateState.Idle
    }
}