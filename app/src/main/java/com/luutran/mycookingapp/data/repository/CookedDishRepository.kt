package com.luutran.mycookingapp.data.repository

import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.vector.path
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.FirebaseStorage
import com.luutran.mycookingapp.data.model.CookedDishEntry // Ensure correct import
import com.luutran.mycookingapp.data.model.DishMemory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CookedDishRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance() // For image uploads

    private fun currentUserCookedDishesCollection() =
        auth.currentUser?.uid?.let { userId ->
            firestore.collection("users").document(userId).collection("cookedDishes")
        }

    // Helper to get the 'memories' subcollection for a specific recipe
    private fun getMemoriesCollectionRef(recipeIdStr: String) =
        currentUserCookedDishesCollection()?.document(recipeIdStr)?.collection("memories")

    /**
     * Adds a recipe to the user's cooked list or increments its cooked count if it already exists.
     * This function WILL BE USED BY THE DISH MEMORIES SCREEN to log a cooking event.
     */
    suspend fun addOrIncrementCookedDish(
        recipeId: Int,
        recipeTitle: String, // Title might be useful if the entry doesn't exist yet
        recipeImageUrl: String? // Image URL might be useful if the entry doesn't exist yet
    ): Result<Unit> {
        val collection = currentUserCookedDishesCollection()
            ?: return Result.failure(Exception("User not logged in"))
        val recipeIdStr = recipeId.toString()
        val docRef = collection.document(recipeIdStr)

        return try {
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (snapshot.exists()) {
                    // Dish exists, increment timesCooked and update lastCookedAt
                    transaction.update(docRef, "timesCooked", FieldValue.increment(1))
                    transaction.update(docRef, "lastCookedAt", FieldValue.serverTimestamp())
                    // Optionally update title and imageUrl if they were missing or changed
                    // This ensures the entry has the latest details if it's being incremented.
                    val updates = mutableMapOf<String, Any?>()
                    if (snapshot.getString("title") != recipeTitle) {
                        updates["title"] = recipeTitle
                    }
                    if (snapshot.getString("imageUrl") != recipeImageUrl) {
                        updates["imageUrl"] = recipeImageUrl
                    }
                    if (updates.isNotEmpty()) {
                        transaction.update(docRef, updates)
                    }

                } else {
                    // New dish, create it with timesCooked = 1
                    val newEntry = CookedDishEntry(
                        recipeId = recipeIdStr,
                        title = recipeTitle,
                        imageUrl = recipeImageUrl,
                        timesCooked = 1, // First time cooking
                        firstAddedAt = null,
                        lastCookedAt = null
                    )
                    transaction.set(docRef, newEntry)
                }
                null // Transaction must return null or a result
            }.await()
            Log.d("CookedDishRepo", "addOrIncrementCookedDish successful for: $recipeIdStr")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CookedDishRepo", "Error in addOrIncrementCookedDish for: $recipeIdStr", e)
            Result.failure(e)
        }
    }

    suspend fun ensureCookedDishEntryExists(
        recipeId: Int,
        recipeTitle: String,
        recipeImageUrl: String?
    ): Result<Unit> {
        val collection = currentUserCookedDishesCollection()
            ?: return Result.failure(Exception("User not logged in"))
        val recipeIdStr = recipeId.toString()
        val docRef = collection.document(recipeIdStr)

        return try {
            val snapshot = docRef.get().await() // Get the document outside a transaction first
            if (!snapshot.exists()) {
                // Entry does not exist, create it with timesCooked = 0
                val newEntry = CookedDishEntry(
                    recipeId = recipeIdStr,
                    title = recipeTitle,
                    imageUrl = recipeImageUrl,
                    timesCooked = 0,
                    firstAddedAt = null,
                    lastCookedAt = null
                )
                docRef.set(newEntry).await()
                Log.d("CookedDishRepo", "ensureCookedDishEntryExists: CREATED new entry for $recipeIdStr with timesCooked = 0")
            } else {
                // Entry exists. Optionally update title/image if they've changed, but don't touch timesCooked.
                val updates = mutableMapOf<String, Any?>()
                if (snapshot.getString("title") != recipeTitle) {
                    updates["title"] = recipeTitle
                }
                if (snapshot.getString("imageUrl") != recipeImageUrl) {
                    updates["imageUrl"] = recipeImageUrl
                }
                if (updates.isNotEmpty()) {
                    docRef.update(updates).await()
                    Log.d("CookedDishRepo", "ensureCookedDishEntryExists: Updated title/image for existing entry $recipeIdStr")
                } else {
                    Log.d("CookedDishRepo", "ensureCookedDishEntryExists: Entry for $recipeIdStr already exists, no changes made.")
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CookedDishRepo", "Error in ensureCookedDishEntryExists for $recipeIdStr", e)
            Result.failure(e)
        }
    }


    /**
     * Gets a specific cooked dish entry, primarily to check its existence and current count.
     */
    suspend fun getCookedDishEntry(recipeId: Int): Result<CookedDishEntry?> {
        val collection = currentUserCookedDishesCollection()
            ?: return Result.failure(Exception("User not logged in"))
        val recipeIdStr = recipeId.toString()

        return try {
            val document = collection.document(recipeIdStr).get().await()
            if (document.exists()) {
                val entry = document.toObject(CookedDishEntry::class.java)
                Result.success(entry)
            } else {
                Result.success(null) // Not found
            }
        } catch (e: Exception) {
            Log.e("CookedDishRepo", "Error getting cooked dish entry: $recipeIdStr", e)
            Result.failure(e)
        }
    }


    suspend fun getAllCookedDishes(): Result<List<CookedDishEntry>> {
        val collection = currentUserCookedDishesCollection()
            ?: return Result.failure(Exception("User not logged in"))

        return try {
            val snapshot = collection.get().await()
            Result.success(snapshot.documents.mapNotNull { it.toObject(CookedDishEntry::class.java) })
        } catch (e: Exception) {
            Log.e("CookedDishRepo", "Error fetching all cooked dishes", e)
            Result.failure(e)
        }
    }

    // --- FUNCTIONS FOR DISH MEMORIES ---

    /**
     * Fetches all dish memories for a specific recipe as a Flow for real-time updates.
     */
    fun getDishMemories(recipeId: Int): Flow<Result<List<DishMemory>>> {
        val recipeIdStr = recipeId.toString()
        val memoriesCollection = getMemoriesCollectionRef(recipeIdStr)
            ?: return kotlinx.coroutines.flow.flowOf(Result.failure(Exception("User not logged in or path error")))

        return memoriesCollection
            .orderBy("timestamp", Query.Direction.DESCENDING) // Show newest first
            .snapshots() // This returns a Flow of QuerySnapshot
            .map { snapshot ->
                try {
                    val memories = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(DishMemory::class.java)?.copy(id = doc.id)
                    }
                    Result.success(memories)
                } catch (e: Exception) {
                    Log.e("CookedDishRepo", "Error mapping memories snapshot for recipe $recipeIdStr", e)
                    Result.failure(e)
                }
            }
            .catch { e ->
                Log.e("CookedDishRepo", "Error fetching memories for recipe $recipeIdStr", e)
                emit(Result.failure(e))
            }
    }

    suspend fun addDishMemory(
        recipeId: Int,
        recipeTitle: String,
        recipeImageUrl: String?,
        memoryDraft: DishMemory,
        imageUris: List<Uri>
    ): Result<String> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
        val recipeIdStr = recipeId.toString()
        val memoriesCollection = getMemoriesCollectionRef(recipeIdStr)
            ?: return Result.failure(Exception("Memories collection path error"))

        val newMemoryId = memoriesCollection.document().id

        return try {
            val uploadedImageUrls = mutableListOf<String>()
            if (imageUris.isNotEmpty()) {
                imageUris.forEachIndexed { index, uri ->
                    val imageRef = storage.reference.child("users/$userId/cookedDishes/$recipeIdStr/memories/$newMemoryId/image_$index.jpg")
                    val uploadTask = imageRef.putFile(uri).await()
                    val downloadUrl = uploadTask.storage.downloadUrl.await().toString()
                    uploadedImageUrls.add(downloadUrl)
                }
            }

            val finalMemory = memoryDraft.copy(
                id = newMemoryId,
                imageUrls = uploadedImageUrls,
                timestamp = null
            )

            // Firestore transaction
            firestore.runTransaction { transaction ->
                val parentDocRef = currentUserCookedDishesCollection()!!.document(recipeIdStr)
                val memoryDocRef = memoriesCollection.document(newMemoryId)

                // 1. Ensure parent CookedDishEntry exists and increment its timesCooked
                val parentSnapshot = transaction.get(parentDocRef)
                if (parentSnapshot.exists()) {
                    transaction.update(parentDocRef, "timesCooked", FieldValue.increment(1))
                    transaction.update(parentDocRef, "lastCookedAt", FieldValue.serverTimestamp())
                    if (parentSnapshot.getString("title") != recipeTitle) {
                        transaction.update(parentDocRef, "title", recipeTitle)
                    }
                    if (parentSnapshot.getString("imageUrl") != recipeImageUrl && recipeImageUrl != null) {
                        transaction.update(parentDocRef, "imageUrl", recipeImageUrl)
                    } else if (recipeImageUrl == null && parentSnapshot.getString("imageUrl") != null) {
                        transaction.update(parentDocRef, "imageUrl", null)
                    }


                } else {
                    val newEntry = CookedDishEntry(
                        recipeId = recipeIdStr,
                        title = recipeTitle,
                        imageUrl = recipeImageUrl,
                        timesCooked = 1,
                        firstAddedAt = null,
                        lastCookedAt = null
                    )
                    transaction.set(parentDocRef, newEntry)
                }

                // 2. Set the new memory document
                transaction.set(memoryDocRef, finalMemory)
                null // Transaction lambda must return null or a result if not using await() directly on it
            }.await()

            Log.d("CookedDishRepo", "Successfully added dish memory $newMemoryId for recipe $recipeIdStr")
            Result.success(newMemoryId)
        } catch (e: Exception) {
            Log.e("CookedDishRepo", "Error adding dish memory for recipe $recipeIdStr", e)
            // Attempt to delete uploaded images if memory creation failed
            if (imageUris.isNotEmpty() && newMemoryId.isNotBlank()) {
                imageUris.forEachIndexed { index, _ ->
                    try {
                        storage.reference.child("users/$userId/cookedDishes/$recipeIdStr/memories/$newMemoryId/image_$index.jpg").delete().await()
                        Log.d("CookedDishRepo", "Deleted orphaned image on failure: index $index for memory $newMemoryId")
                    } catch (delEx: Exception) {
                        Log.e("CookedDishRepo", "Failed to delete orphaned image: index $index for memory $newMemoryId", delEx)
                    }
                }
            }
            Result.failure(e)
        }
    }


    /**
     * Deletes specified dish memories and decrements the parent's timesCooked count for each.
     * Also deletes associated images from Firebase Storage.
     */
    suspend fun deleteDishMemories(
        recipeId: Int,
        memoryIdsToDelete: List<String>
    ): Result<Unit> {
        if (memoryIdsToDelete.isEmpty()) return Result.success(Unit)

        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
        val recipeIdStr = recipeId.toString()
        val memoriesCollection = getMemoriesCollectionRef(recipeIdStr)
            ?: return Result.failure(Exception("Memories collection path error"))
        val parentDocRef = currentUserCookedDishesCollection()?.document(recipeIdStr)
            ?: return Result.failure(Exception("Parent cooked dish entry path error"))

        return try {
            // First, get image URLs for all memories to be deleted to clean up storage
            val imageUrlsToDelete = mutableListOf<String>()
            memoryIdsToDelete.forEach { memoryId ->
                try {
                    val memoryDoc = memoriesCollection.document(memoryId).get().await()
                    val memoryData = memoryDoc.toObject(DishMemory::class.java)
                    memoryData?.imageUrls?.let { urls -> imageUrlsToDelete.addAll(urls) }
                } catch (e: Exception) {
                    Log.w("CookedDishRepo", "Could not fetch memory $memoryId for image deletion, proceeding without it.", e)
                }
            }


            // Firestore batch write for deleting memory documents and updating parent
            val batch = firestore.batch()
            memoryIdsToDelete.forEach { memoryId ->
                batch.delete(memoriesCollection.document(memoryId))
            }
            // Decrement timesCooked for each memory deleted
            batch.update(parentDocRef, "timesCooked", FieldValue.increment(-memoryIdsToDelete.size.toLong()))

            batch.commit().await()

            // After successful Firestore deletion, delete images from Storage
            imageUrlsToDelete.forEach { imageUrl ->
                try {
                    if (imageUrl.isNotBlank()) { // Ensure URL is not empty
                        val storageRef = storage.getReferenceFromUrl(imageUrl)
                        storageRef.delete().await()
                        Log.d("CookedDishRepo", "Deleted image from Storage: $imageUrl")
                    }
                } catch (e: Exception) {
                    // Log error but don't fail the whole operation if an image deletion fails
                    Log.e("CookedDishRepo", "Failed to delete image from Storage: $imageUrl", e)
                }
            }

            Log.d("CookedDishRepo", "Successfully deleted ${memoryIdsToDelete.size} memories for recipe $recipeIdStr")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CookedDishRepo", "Error deleting dish memories for recipe $recipeIdStr", e)
            Result.failure(e)
        }
    }

    private suspend fun deleteCookedDishEntryWithCascade(recipeId: String): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
        val cookedDishDocRef = currentUserCookedDishesCollection()?.document(recipeId)
            ?: return Result.failure(Exception("Cooked dish path error"))
        val memoriesCollectionRef = getMemoriesCollectionRef(recipeId)
            ?: return Result.failure(Exception("Memories collection path error"))

        return try {
            // 1. Get all memories to find their image URLs
            val memoriesSnapshot = memoriesCollectionRef.get().await()
            val imageUrlsToDelete = mutableListOf<String>()
            val memoryIdsToDelete = mutableListOf<String>()

            for (doc in memoriesSnapshot.documents) {
                memoryIdsToDelete.add(doc.id)
                val memory = doc.toObject(DishMemory::class.java)
                memory?.imageUrls?.let { urls -> imageUrlsToDelete.addAll(urls.filter { it.isNotBlank() }) }
            }

            // 2. Perform batched Firestore deletions
            val batch = firestore.batch()
            memoryIdsToDelete.forEach { memoryId ->
                batch.delete(memoriesCollectionRef.document(memoryId))
            }
            batch.delete(cookedDishDocRef) // Delete the parent entry
            batch.commit().await()
            Log.d("CookedDishRepo", "Firestore: Deleted cooked dish entry $recipeId and its memories.")

            // 3. Delete images from Storage
            imageUrlsToDelete.forEach { imageUrl ->
                try {
                    val storageRef = storage.getReferenceFromUrl(imageUrl)
                    storageRef.delete().await()
                    Log.d("CookedDishRepo", "Storage: Deleted image $imageUrl")
                } catch (e: Exception) {
                    Log.w("CookedDishRepo", "Storage: Failed to delete image $imageUrl. Manual cleanup might be needed.", e)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CookedDishRepo", "Error in cascade delete for $recipeId", e)
            Result.failure(e)
        }
    }

    suspend fun deleteCookedDishEntriesWithCascade(recipeIds: List<String>): Result<Unit> {
        if (recipeIds.isEmpty()) return Result.success(Unit)
        // This could be done by calling deleteCookedDishEntryWithCascade for each ID,
        // but be mindful of performing too many individual complex operations.
        // For multiple items, batching Firestore writes is good, but Storage deletions
        // are individual. A Cloud Function is still the most robust for multiple cascades.
        var allSuccessful = true
        recipeIds.forEach { recipeId ->
            val result = deleteCookedDishEntryWithCascade(recipeId)
            if (result.isFailure) {
                allSuccessful = false
                Log.e("CookedDishRepo", "Failed to cascade delete entry: $recipeId")
            }
        }
        return if (allSuccessful) Result.success(Unit) else Result.failure(Exception("One or more entries failed to delete completely."))
    }

    /**
     * Fetches a single dish memory by its ID for a specific recipe.
     */
    suspend fun getDishMemoryById(recipeId: Int, memoryId: String): Result<DishMemory?> {
        val recipeIdStr = recipeId.toString()
        val memoriesCollection = getMemoriesCollectionRef(recipeIdStr)
            ?: return Result.failure(Exception("User not logged in or path error for memories collection"))

        return try {
            val documentSnapshot = memoriesCollection.document(memoryId).get().await()
            if (documentSnapshot.exists()) {
                val memory = documentSnapshot.toObject(DishMemory::class.java)?.copy(id = documentSnapshot.id)
                Result.success(memory)
            } else {
                Log.w("CookedDishRepo", "No memory found with ID $memoryId for recipe $recipeIdStr")
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e("CookedDishRepo", "Error fetching memory $memoryId for recipe $recipeIdStr", e)
            Result.failure(e)
        }
    }

    suspend fun updateDishMemory(
        recipeId: Int,
        memory: DishMemory,
        newImageUris: List<Uri>,
        existingImageUrlsToDelete: List<String>
    ): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
        val recipeIdStr = recipeId.toString()
        val memoryId = memory.id
        if (memoryId.isBlank()) return Result.failure(IllegalArgumentException("Memory ID cannot be blank for update"))

        val memoryDocRef = getMemoriesCollectionRef(recipeIdStr)?.document(memoryId)
            ?: return Result.failure(Exception("Memory document path error"))

        return try {
            // 1. Delete specified existing images from Firebase Storage
            existingImageUrlsToDelete.forEach { imageUrl ->
                if (imageUrl.isNotBlank()) {
                    try {
                        storage.getReferenceFromUrl(imageUrl).delete().await()
                        Log.d("CookedDishRepo", "Update: Deleted image from Storage: $imageUrl")
                    } catch (e: Exception) {
                        Log.e("CookedDishRepo", "Update: Failed to delete image from Storage: $imageUrl. Continuing.", e)
                    }
                }
            }

            // 2. Upload new images and get their download URLs
            val newlyUploadedImageUrls = mutableListOf<String>()
            if (newImageUris.isNotEmpty()) {
                newImageUris.forEachIndexed { index, uri ->
                    // Create a unique name for new images to avoid conflicts if user re-adds an image they just deleted in the same session
                    val imageName = "image_${System.currentTimeMillis()}_$index.jpg"
                    val imageRef = storage.reference.child("users/$userId/cookedDishes/$recipeIdStr/memories/$memoryId/$imageName")
                    val uploadTask = imageRef.putFile(uri).await()
                    val downloadUrl = uploadTask.storage.downloadUrl.await().toString()
                    newlyUploadedImageUrls.add(downloadUrl)
                    Log.d("CookedDishRepo", "Update: Uploaded new image: $downloadUrl")
                }
            }

            // 3. Prepare the final list of image URLs for Firestore
            val finalImageUrls = memory.imageUrls + newlyUploadedImageUrls

            // 4. Update Firestore document
            val updates = mutableMapOf<String, Any>(
                "rating" to memory.rating,
                "notes" to memory.notes,
                "imageUrls" to finalImageUrls
            )

            memoryDocRef.update(updates).await()

            Log.d("CookedDishRepo", "Successfully updated dish memory $memoryId for recipe $recipeIdStr")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CookedDishRepo", "Error updating dish memory $memoryId for recipe $recipeIdStr", e)
            Result.failure(e)
        }
    }

    /**
     * User Recipe
     */
    private fun currentUserRecipeMemoriesCollection(userRecipeId: String) =
        auth.currentUser?.uid?.let { userId ->
            firestore.collection("users").document(userId)
                .collection("userRecipes").document(userRecipeId) // Path for user recipe's memories
                .collection("memories")
        }

    /**
     * Fetches all dish memories for a specific USER RECIPE as a Flow.
     */
    fun getMemoriesForUserRecipe(userRecipeId: String): Flow<Result<List<DishMemory>>> {
        val memoriesCollection = currentUserRecipeMemoriesCollection(userRecipeId)
            ?: return kotlinx.coroutines.flow.flowOf(Result.failure(Exception("User not logged in or path error for user recipe memories")))

        return memoriesCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                try {
                    val memories = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(DishMemory::class.java)
                    }
                    Result.success(memories)
                } catch (e: Exception) {
                    Log.e("CookedDishRepo", "Error mapping memories snapshot for user recipe $userRecipeId", e)
                    Result.failure(e)
                }
            }
            .catch { e ->
                Log.e("CookedDishRepo", "Error fetching memories for user recipe $userRecipeId", e)
                emit(Result.failure(e))
            }
    }

    /**
     * Adds a new dish memory for a USER RECIPE.
     */
    suspend fun addMemoryToUserRecipe(
        userRecipeId: String,
        memoryDraft: DishMemory,
        imageUris: List<Uri>
    ): Result<String> { // Return new memory ID
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
        val memoriesCollection = currentUserRecipeMemoriesCollection(userRecipeId)
            ?: return Result.failure(Exception("Memories collection path error for user recipe"))

        val newMemoryDocRef = memoriesCollection.document()
        val newMemoryId = newMemoryDocRef.id

        return try {
            val uploadedImageUrls = mutableListOf<String>()
            if (imageUris.isNotEmpty()) {
                imageUris.forEachIndexed { index, uri ->
                    val imageRef = storage.reference.child("users/$userId/userRecipes/$userRecipeId/memories/$newMemoryId/image_$index.jpg")
                    val uploadTask = imageRef.putFile(uri).await()
                    val downloadUrl = uploadTask.storage.downloadUrl.await().toString()
                    uploadedImageUrls.add(downloadUrl)
                }
            }

            val finalMemory = memoryDraft.copy(
                id = newMemoryId,
                imageUrls = uploadedImageUrls,
                timestamp = null
            )

            newMemoryDocRef.set(finalMemory).await()
            Log.d("CookedDishRepo", "Successfully added memory $newMemoryId for user recipe $userRecipeId")
            Result.success(newMemoryId)
        } catch (e: Exception) {
            Log.e("CookedDishRepo", "Error adding memory for user recipe $userRecipeId", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes specified dish memories for a USER RECIPE.
     */
    suspend fun deleteMemoriesForUserRecipe(
        userRecipeId: String,
        memoryIdsToDelete: List<String>
    ): Result<Unit> {
        if (memoryIdsToDelete.isEmpty()) return Result.success(Unit)
        val memoriesCollection = currentUserRecipeMemoriesCollection(userRecipeId)
            ?: return Result.failure(Exception("Memories collection path error"))

        return try {
            val imageUrlsToDelete = mutableListOf<String>()
            memoryIdsToDelete.forEach { memoryId ->
                try {
                    val memoryDoc = memoriesCollection.document(memoryId).get().await()
                    memoryDoc.toObject(DishMemory::class.java)?.imageUrls?.let { imageUrlsToDelete.addAll(it) }
                } catch (e: Exception) { /* Log*/ }
            }

            val batch = firestore.batch()
            memoryIdsToDelete.forEach { memoryId ->
                batch.delete(memoriesCollection.document(memoryId))
            }
            batch.commit().await()

            imageUrlsToDelete.forEach { imageUrl ->
                try {
                    if (imageUrl.isNotBlank()) storage.getReferenceFromUrl(imageUrl).delete().await()
                } catch (e: Exception) { /* Log image deletion error */ }
            }
            Log.d("CookedDishRepo", "Deleted ${memoryIdsToDelete.size} memories for user recipe $userRecipeId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CookedDishRepo", "Error deleting memories for user recipe $userRecipeId", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches a single dish memory by its ID for a specific USER RECIPE.
     */
    suspend fun getMemoryForUserRecipeById(userRecipeId: String, memoryId: String): Result<DishMemory?> {
        val memoriesCollection = currentUserRecipeMemoriesCollection(userRecipeId)
            ?: return Result.failure(Exception("User not logged in or path error"))

        return try {
            val documentSnapshot = memoriesCollection.document(memoryId).get().await()
            if (documentSnapshot.exists()) {
                Result.success(documentSnapshot.toObject(DishMemory::class.java))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates an existing dish memory for a USER RECIPE.
     */
    suspend fun updateMemoryForUserRecipe(
        userRecipeId: String,
        memory: DishMemory, // Contains ID, new data, and image URLs to KEEP
        newImageUris: List<Uri>,
        existingImageUrlsToDelete: List<String>
    ): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
        val memoryId = memory.id
        if (memoryId.isBlank()) {
            return Result.failure(IllegalArgumentException("Memory ID cannot be blank for update"))
        }

        val memoryDocRef = currentUserRecipeMemoriesCollection(userRecipeId)?.document(memoryId)
            ?: return Result.failure(Exception("Memory document path error for user recipe"))

        Log.d("CookedDishRepo", "Updating memory $memoryId for user $userRecipeId. Images to delete: ${existingImageUrlsToDelete.size}, New images: ${newImageUris.size}")
        Log.d("CookedDishRepo", "Initial image URLs to keep (from memory object): ${memory.imageUrls}")


        return try {
            // 1. Delete specified existing images from Firebase Storage
            if (existingImageUrlsToDelete.isNotEmpty()) {
                existingImageUrlsToDelete.forEach { imageUrl ->
                    try {
                        if (imageUrl.isNotBlank()) {
                            Log.d("CookedDishRepo", "Attempting to delete from Storage: $imageUrl")
                            storage.getReferenceFromUrl(imageUrl).delete().await()
                            Log.d("CookedDishRepo", "Successfully deleted from Storage: $imageUrl")
                        }
                    } catch (e: Exception) {
                        Log.e("CookedDishRepo", "Failed to delete image $imageUrl from Storage during update", e)
                    }
                }
            } else {
                Log.d("CookedDishRepo", "No existing images marked for deletion.")
            }

            // 2. Upload new images to Firebase Storage
            val newlyUploadedCloudImageUrls = mutableListOf<String>()
            if (newImageUris.isNotEmpty()) {
                newImageUris.forEachIndexed { index, uri ->
                    // Use a unique name for each image to prevent overwrites if timestamps are too close
                    val imageName = "image_${System.currentTimeMillis()}_$index.jpg"
                    val imageRef = storage.reference.child("users/$userId/userRecipes/$userRecipeId/memories/$memoryId/$imageName")
                    try {
                        Log.d("CookedDishRepo", "Uploading new image from URI: $uri to path: ${imageRef.path}")
                        val uploadTask = imageRef.putFile(uri).await()
                        val downloadUrl = uploadTask.storage.downloadUrl.await().toString()
                        newlyUploadedCloudImageUrls.add(downloadUrl)
                        Log.d("CookedDishRepo", "Successfully uploaded new image: $downloadUrl")
                    } catch (e: Exception) {
                        Log.e("CookedDishRepo", "Failed to upload new image $uri during update", e)
                        // If one upload fails, the whole update fails
                        return Result.failure(Exception("Failed to upload new image: ${e.message}", e))
                    }
                }
            } else {
                Log.d("CookedDishRepo", "No new local image URIs to upload.")
            }

            // 3. Construct the final list of image URLs for Firestore
            val finalImageUrlsForFirestore = memory.imageUrls + newlyUploadedCloudImageUrls
            Log.d("CookedDishRepo", "Final image URLs for Firestore: $finalImageUrlsForFirestore")


            // 4. Update Firestore document
            val updates = mapOf(
                "rating" to memory.rating,
                "notes" to memory.notes,
                "imageUrls" to finalImageUrlsForFirestore.distinct() // Ensure no duplicates
            )
            Log.d("CookedDishRepo", "Updating Firestore document $memoryId with: $updates")
            memoryDocRef.update(updates).await()
            Log.d("CookedDishRepo", "Successfully updated Firestore for memory $memoryId")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CookedDishRepo", "Error updating memory $memoryId for user recipe $userRecipeId", e)
            Result.failure(e)
        }
    }

}