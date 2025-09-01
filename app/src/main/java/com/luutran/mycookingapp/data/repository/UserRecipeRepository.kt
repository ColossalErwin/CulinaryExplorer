package com.luutran.mycookingapp.data.repository

import android.net.Uri
import android.system.Os
import android.util.Log
import androidx.compose.foundation.gestures.forEach
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.size
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.storage.FirebaseStorage
import com.luutran.mycookingapp.data.model.UserRecipe
import kotlinx.coroutines.tasks.await
import java.util.UUID
import com.google.firebase.firestore.Query
import com.google.firebase.storage.StorageException
import com.luutran.mycookingapp.data.model.DishMemory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.text.isNotBlank

// Result sealed class (remains the same)
sealed class RepositoryResult<out T> {
    data class Success<T>(val data: T) : RepositoryResult<T>()
    data class Error(val exception: Exception) : RepositoryResult<Nothing>()
}

inline fun <T, R> RepositoryResult<T>.fold(
    onSuccess: (value: T) -> R,
    onFailure: (exception: Exception) -> R
): R {
    return when (this) {
        is RepositoryResult.Success -> onSuccess(data)
        is RepositoryResult.Error -> onFailure(exception)
    }
}

class UserRecipeRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Assuming a structure like /users/{userId}/user_recipes/{recipeId}
    // The ViewModel will be responsible for providing the correct collection path.
    // This repository will then operate on that specific user's recipe collection.

    /**
     * Helper to get the 'memories' subcollection for a specific user recipe.
     */
    private fun getUserRecipeMemoriesCollection(userId: String, userRecipeId: String): com.google.firebase.firestore.CollectionReference {
        if (userId.isBlank() || userRecipeId.isBlank()) {
            throw IllegalArgumentException("User ID and User Recipe ID cannot be blank.")
        }
        return firestore.collection("users").document(userId)
            .collection("userRecipes")
            .document(userRecipeId)
            .collection("memories")
    }

    /**
     * Deletes a user recipe, its associated images from Storage, AND all its memories.
     */
    suspend fun deleteUserRecipeWithMemories(userRecipeId: String): RepositoryResult<Unit> {
        val currentUser = auth.currentUser
            ?: return RepositoryResult.Error(Exception("User not authenticated to delete recipe."))
        val userId = currentUser.uid

        if (userRecipeId.isBlank()) {
            return RepositoryResult.Error(IllegalArgumentException("User Recipe ID cannot be blank."))
        }

        try {
            // 1. Fetch the recipe to get its image URLs for deletion from Storage
            val recipeSnapshot = getUserRecipesCollectionFor(userId).document(userRecipeId).get().await()
            val recipeData = recipeSnapshot.toObject<UserRecipe>() // Make sure UserRecipe.kt is correct

            // Delete recipe images from Storage
            if (recipeData != null && recipeData.imageUrls.isNotEmpty()) {
                for (imageUrl in recipeData.imageUrls) {
                    try {
                        deleteRecipeImage(imageUrl)
                    } catch (e: Exception) {
                        Log.e("UserRecipeRepo", "Failed to delete a recipe image ($imageUrl) for $userRecipeId. Continuing...", e)
                    }
                }
            }

            // 2. Delete all memories in the subcollection and their images
            //    It's more efficient to list memories once, delete their images, then delete memory documents in a batch.
            val memoriesCollection = getUserRecipeMemoriesCollection(userId, userRecipeId)
            val memorySnapshots = memoriesCollection.get().await()
            val batch = firestore.batch()

            for (memoryDoc in memorySnapshots.documents) {
                val memory = memoryDoc.toObject(DishMemory::class.java) // Ensure DishMemory model is correct
                // Delete associated images for each memory from Storage
                memory?.imageUrls?.forEach { imageUrl ->
                    if (imageUrl.isNotBlank()) {
                        try {
                            deleteRecipeImage(imageUrl)
                        } catch (e: Exception) {
                            Log.e("UserRecipeRepo", "Failed to delete a memory image ($imageUrl) for memory ${memoryDoc.id}. Continuing...", e)
                        }
                    }
                }
                batch.delete(memoryDoc.reference)
            }
            batch.commit().await() // Commit batch deletion of memory documents
            Log.d("UserRecipeRepo", "All memories for recipe $userRecipeId deleted.")


            // 3. Delete the recipe document itself
            getUserRecipesCollectionFor(userId).document(userRecipeId).delete().await()
            Log.d("UserRecipeRepo", "Recipe $userRecipeId and its content deleted successfully for user $userId.")
            return RepositoryResult.Success(Unit)

        } catch (e: Exception) {
            Log.e("UserRecipeRepo", "Error deleting recipe $userRecipeId with memories for user $userId: ${e.message}", e)
            return RepositoryResult.Error(e)
        }
    }

    /**
     * Gets a Flow of dish memories for a specific user-created recipe for the current user.
     * Listens for real-time updates.
     * The path will be users/{userId}/userRecipes/{userRecipeId}/memories
     *
     * @param userRecipeId The ID of the user recipe.
     * @return Flow<RepositoryResult<List<DishMemory>>>
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getMemoriesForUserRecipe(userRecipeId: String): Flow<RepositoryResult<List<DishMemory>>> {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            return callbackFlow {
                trySend(RepositoryResult.Error(Exception("User not authenticated."))); close()
            }
        }
        if (userRecipeId.isBlank()) {
            return callbackFlow {
                trySend(RepositoryResult.Error(IllegalArgumentException("User Recipe ID cannot be blank."))); close()
            }
        }
        val userId = currentUser.uid

        return callbackFlow {
            val memoriesCollection = getUserRecipeMemoriesCollection(userId, userRecipeId)
                .orderBy("timestamp", Query.Direction.DESCENDING) // Assuming DishMemory has a timestamp

            val listenerRegistration = memoriesCollection.addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("UserRecipeRepo", "Error listening to memories for user recipe $userRecipeId", error)
                    close(error) // Close flow with error
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    val memories = snapshots.toObjects(DishMemory::class.java) // Ensure DishMemory.kt is correct
                    Log.d("UserRecipeRepo", "Flow: Emitting ${memories.size} memories for user recipe $userRecipeId")
                    trySend(RepositoryResult.Success(memories)).isSuccess
                } else {
                    trySend(RepositoryResult.Success(emptyList<DishMemory>())).isSuccess // Firestore can send null snapshot
                }
            }
            awaitClose {
                Log.d("UserRecipeRepo", "Closing Firestore listener for user recipe $userRecipeId memories.")
                listenerRegistration.remove()
            }
        }
    }

    /**
     * Deletes specified dish memories for a given user recipe for the current user.
     * Also deletes associated images from Firebase Storage.
     *
     * @param userRecipeId The ID of the user recipe these memories belong to.
     * @param memoryIds The list of memory IDs to delete.
     * @return RepositoryResult<Unit> indicating success or failure.
     */
    suspend fun deleteMemoriesForUserRecipe(userRecipeId: String, memoryIds: List<String>): RepositoryResult<Unit> {
        val currentUser = auth.currentUser
            ?: return RepositoryResult.Error(Exception("User not authenticated."))
        if (userRecipeId.isBlank()) {
            return RepositoryResult.Error(IllegalArgumentException("User Recipe ID cannot be blank."))
        }
        if (memoryIds.isEmpty()) {
            return RepositoryResult.Success(Unit) // Nothing to delete
        }
        val userId = currentUser.uid

        try {
            val memoriesCollection = getUserRecipeMemoriesCollection(userId, userRecipeId)
            val batch = firestore.batch()

            for (memoryId in memoryIds) {
                if (memoryId.isBlank()) continue

                // Fetch the memory to get its image URLs for deletion from Storage
                val memoryDocRef = memoriesCollection.document(memoryId)
                try {
                    val memorySnapshot = memoryDocRef.get().await()
                    val memoryData = memorySnapshot.toObject(DishMemory::class.java)

                    memoryData?.imageUrls?.forEach { imageUrl ->
                        if (imageUrl.isNotBlank()) {
                            try {
                                // Assuming deleteRecipeImage can handle these URLs
                                // Or create a specific deleteMemoryImage if paths differ significantly
                                deleteRecipeImage(imageUrl)
                            } catch (e: StorageException) {
                                Log.e("UserRecipeRepo", "Failed to delete storage image $imageUrl for memory $memoryId: ${e.message}", e)
                                // Decide if this is a critical failure or continue
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("UserRecipeRepo", "Could not fetch memory $memoryId for image deletion: ${e.message}", e)
                    // If fetching fails, we might still want to delete the Firestore entry
                }
                batch.delete(memoryDocRef)
            }

            batch.commit().await()
            Log.d("UserRecipeRepo", "Successfully deleted memories for user recipe $userRecipeId: $memoryIds")
            return RepositoryResult.Success(Unit)
        } catch (e: Exception) {
            Log.e("UserRecipeRepo", "Error deleting memories for user recipe $userRecipeId: ${e.message}", e)
            return RepositoryResult.Error(e)
        }
    }

    /**
     * Gets the Firestore collection reference for a specific user's recipes.
     * @param userId The ID of the user.
     * @return CollectionReference to the user's recipes.
     * @throws IllegalStateException if userId is blank.
     */
    private fun getUserRecipesCollectionFor(userId: String): com.google.firebase.firestore.CollectionReference {
        if (userId.isBlank()) {
            throw IllegalStateException("User ID cannot be blank to get recipes collection.")
        }
        return firestore.collection("users").document(userId).collection("userRecipes")
    }

    /**
     * Generates a new unique ID for a recipe document within a user's collection.
     */
    fun getNewRecipeId(userId: String): String {
        return getUserRecipesCollectionFor(userId).document().id
    }

    suspend fun addUserRecipe(recipe: UserRecipe, imageUris: List<Uri>?): RepositoryResult<String> {
        val currentUser = auth.currentUser
            ?: return RepositoryResult.Error(Exception("User not authenticated."))
        val userId = currentUser.uid

        if (recipe.id.isBlank()) {
            return RepositoryResult.Error(Exception("Recipe ID cannot be blank for add operation."))
        }

        try {
            val uploadedImageUrls = mutableListOf<String>()
            if (!imageUris.isNullOrEmpty()) {
                for (uri in imageUris) {
                    val imageUrl = uploadRecipeImage(userId, recipe.id, uri)
                    uploadedImageUrls.add(imageUrl)
                }
            }

            val finalRecipe = recipe.copy(
                imageUrls = uploadedImageUrls
            )

            getUserRecipesCollectionFor(userId).document(finalRecipe.id).set(finalRecipe).await()
            Log.d("UserRecipeRepo", "Recipe added successfully: ${finalRecipe.id} for user $userId")
            return RepositoryResult.Success(finalRecipe.id)

        } catch (e: Exception) {
            Log.e("UserRecipeRepo", "Error adding recipe for user $userId: ${e.message}", e)
            return RepositoryResult.Error(e)
        }
    }

    private suspend fun uploadRecipeImage(userId: String, recipeId: String, imageUri: Uri): String {
        // Path includes userId and recipeId for organization and security rules.
        val imageName = "${UUID.randomUUID()}.jpg" // Unique name for each image
        val imageRef = storage.reference.child("users/$userId/userRecipes/$recipeId/$imageName")

        Log.d("UserRecipeRepo", "Uploading image to: ${imageRef.path}")
        val uploadTask = imageRef.putFile(imageUri).await()
        val downloadUrl = uploadTask.storage.downloadUrl.await().toString()
        Log.d("UserRecipeRepo", "Image uploaded successfully: $downloadUrl")
        return downloadUrl
    }

    /**
     * Fetches a specific user recipe by its ID for the currently authenticated user.
     *
     * @param recipeId The ID of the recipe to fetch.
     * @return RepositoryResult<UserRecipe> containing the recipe on success.
     */
    suspend fun getUserRecipe(recipeId: String): RepositoryResult<UserRecipe> {
        val currentUser = auth.currentUser
            ?: return RepositoryResult.Error(Exception("User not authenticated."))
        val userId = currentUser.uid

        try {
            val documentSnapshot = getUserRecipesCollectionFor(userId).document(recipeId).get().await()
            // @DocumentId in UserRecipe model should map the document's ID to recipe.id
            val recipe = documentSnapshot.toObject<UserRecipe>()
            return if (recipe != null) {
                RepositoryResult.Success(recipe)
            } else {
                RepositoryResult.Error(Exception("Recipe not found or could not be deserialized."))
            }
        } catch (e: Exception) {
            Log.e("UserRecipeRepo", "Error fetching recipe $recipeId for user $userId: ${e.message}", e)
            return RepositoryResult.Error(e)
        }
    }

    suspend fun updateUserRecipe(
        recipe: UserRecipe,
        newImageUris: List<Uri>?,
        imagesToDelete: List<String>? // URLs of images marked for deletion
    ): RepositoryResult<Unit> {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            return RepositoryResult.Error(Exception("User not authenticated to update recipe."))
        }
        val userId = currentUser.uid

        if (recipe.id.isBlank()) {
            return RepositoryResult.Error(Exception("Recipe ID is required for update."))
        }

        var recipeToUpdate = recipe.copy() // Start with the passed recipe state

        try {
            val currentImageUrls = recipeToUpdate.imageUrls.toMutableList()

            // 1. Delete images marked for deletion
            if (!imagesToDelete.isNullOrEmpty()) {
                for (urlToDelete in imagesToDelete) {
                    try {
                        deleteRecipeImage(urlToDelete) // Implement this helper
                        currentImageUrls.remove(urlToDelete)
                        Log.d("UserRecipeRepo", "Successfully deleted image: $urlToDelete")
                    } catch (e: Exception) {
                        Log.e("UserRecipeRepo", "Failed to delete image $urlToDelete: ${e.message}", e)
                    }
                }
            }

            // 2. Upload new images and add their URLs
            val newlyUploadedUrls = mutableListOf<String>()
            if (!newImageUris.isNullOrEmpty()) {
                for (uri in newImageUris) {
                    val newImageUrl = uploadRecipeImage(userId, recipe.id, uri)
                    newlyUploadedUrls.add(newImageUrl)
                }
            }
            currentImageUrls.addAll(newlyUploadedUrls)

            recipeToUpdate = recipeToUpdate.copy(imageUrls = currentImageUrls.distinct()) // Ensure distinct URLs

            // Using SetOptions.merge() is often safer for updates.
            getUserRecipesCollectionFor(userId).document(recipeToUpdate.id).set(recipeToUpdate, SetOptions.merge()).await()
            Log.d("UserRecipeRepo", "Recipe updated successfully: ${recipeToUpdate.id} for user $userId")
            return RepositoryResult.Success(Unit)

        } catch (e: Exception) {
            Log.e("UserRecipeRepo", "Error updating recipe ${recipe.id} for user $userId: ${e.message}", e)
            return RepositoryResult.Error(e)
        }
    }

    /**
     * Deletes a specific image from Firebase Storage using its download URL.
     */
    private suspend fun deleteRecipeImage(imageUrl: String) {
        if (imageUrl.isBlank() || !imageUrl.startsWith("https://firebasestorage.googleapis.com")) {
            Log.w("UserRecipeRepo", "Invalid or empty image URL for deletion: $imageUrl")
            return // Or throw exception
        }
        try {
            val imageRef = storage.getReferenceFromUrl(imageUrl)
            imageRef.delete().await()
            Log.d("UserRecipeRepo", "Image deleted from Storage: $imageUrl")
        } catch (e: Exception) {
            Log.e("UserRecipeRepo", "Error deleting image $imageUrl from Storage", e)
            throw e // Re-throw to be handled by caller
        }
    }

    /**
     * Deletes a user recipe and all its associated images from Storage.
     */
    suspend fun deleteUserRecipe(recipeId: String): RepositoryResult<Unit> {
        val currentUser = auth.currentUser
            ?: return RepositoryResult.Error(Exception("User not authenticated to delete recipe."))
        val userId = currentUser.uid

        try {
            // First, fetch the recipe to get its image URLs for deletion from Storage
            val recipeSnapshot = getUserRecipesCollectionFor(userId).document(recipeId).get().await()
            val recipeData = recipeSnapshot.toObject<UserRecipe>()

            if (recipeData != null && recipeData.imageUrls.isNotEmpty()) {
                for (imageUrl in recipeData.imageUrls) {
                    try {
                        deleteRecipeImage(imageUrl)
                    } catch (e: Exception) {
                        Log.e("UserRecipeRepo", "Failed to delete an image ($imageUrl) during recipe deletion for $recipeId. Continuing...", e)
                    }
                }
            }
            getUserRecipesCollectionFor(userId).document(recipeId).delete().await()
            Log.d("UserRecipeRepo", "Recipe $recipeId and its images deleted successfully for user $userId.")
            return RepositoryResult.Success(Unit)
        } catch (e: Exception) {
            Log.e("UserRecipeRepo", "Error deleting recipe $recipeId for user $userId: ${e.message}", e)
            return RepositoryResult.Error(e)
        }
    }

    suspend fun getUserRecipesForCurrentUserOnce(): RepositoryResult<List<UserRecipe>> {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w("UserRecipeRepo", "User not authenticated for getUserRecipesForCurrentUserOnce.")
            return RepositoryResult.Error(Exception("User not authenticated."))
        }
        val userId = currentUser.uid

        return try {
            val querySnapshot = getUserRecipesCollectionFor(userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val recipes = querySnapshot.documents.mapNotNull { document ->
                document.toObject<UserRecipe>()
            }
            Log.d("UserRecipeRepo", "Fetched ${recipes.size} recipes for user $userId")
            RepositoryResult.Success(recipes)
        } catch (e: Exception) {
            Log.e("UserRecipeRepo", "Error fetching recipes for user $userId: ${e.message}", e)
            RepositoryResult.Error(e)
        }
    }

    fun getUserRecipesFlow(): Flow<RepositoryResult<List<UserRecipe>>> {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w("UserRecipeRepo", "User not authenticated for getUserRecipesFlow.")
            return callbackFlow {
                trySend(RepositoryResult.Error(Exception("User not authenticated.")))
                close()
            }
        }
        val userId = currentUser.uid

        return callbackFlow {
            val listenerRegistration = getUserRecipesCollectionFor(userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.e("UserRecipeRepo", "Error listening to recipe snapshots for user $userId", error)
                        close(error)
                        return@addSnapshotListener
                    }

                    if (snapshots != null) {
                        val recipes = snapshots.documents.mapNotNull { document ->
                            document.toObject<UserRecipe>()
                        }
                        Log.d("UserRecipeRepo", "Flow: Emitting ${recipes.size} recipes for user $userId")
                        trySend(RepositoryResult.Success(recipes))
                    } else {
                        Log.d("UserRecipeRepo", "Flow: Snapshots were null for user $userId (no error)")
                        trySend(RepositoryResult.Success(emptyList()))
                    }
                }
            awaitClose {
                Log.d("UserRecipeRepo", "Closing Firestore listener for user $userId recipes.")
                listenerRegistration.remove()
            }
        }
    }
}