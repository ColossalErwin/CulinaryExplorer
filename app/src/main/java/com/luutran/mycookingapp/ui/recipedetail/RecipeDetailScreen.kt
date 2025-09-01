package com.luutran.mycookingapp.ui.recipedetail

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.luutran.mycookingapp.data.network.NetworkModule
import com.luutran.mycookingapp.data.repository.RecipeRepository
import com.luutran.mycookingapp.R
import com.luutran.mycookingapp.data.local.AppDatabase
import com.luutran.mycookingapp.ui.theme.LightGreenApp
import com.luutran.mycookingapp.ui.theme.OnLightGreenApp
import com.luutran.mycookingapp.ui.utils.FormattedClickableHtmlText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import com.google.firebase.firestore.ktx.firestore
import com.luutran.mycookingapp.data.model.WinePairing
import com.luutran.mycookingapp.data.utils.getCookedDishRepositoryInstance
import com.luutran.mycookingapp.ui.auth.AuthViewModel
import kotlin.text.isNullOrBlank
import kotlin.text.isNullOrEmpty

@Composable
fun InfoSection(title: String, items: List<String>?) {
    if (!items.isNullOrEmpty()) {
        Text(
            text = title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
        )
        items.forEach { item ->
            Text(
                text = "- $item",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 24.dp, end = 16.dp, bottom = 4.dp)
            )
        }
    }
}

@Composable
fun WinePairingSection(winePairing: WinePairing?) {
    winePairing?.let {
        Text(
            text = "Wine Pairing",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
        )
        if (!it.pairingText.isNullOrBlank()) {
            Text(
                text = it.pairingText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
        }
        if (!it.pairedWines.isNullOrEmpty()) {
            Text(
                text = "Suggested Wines:",
                style = MaterialTheme.typography.titleSmall, // Slightly smaller for sub-section
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
            )
            it.pairedWines.forEach { wine ->
                Text(
                    text = "- $wine",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 24.dp, end = 16.dp, bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
fun getRecipeRepository(): RecipeRepository {
    val context = LocalContext.current
    val apiService = NetworkModule.spoonacularApiService
    val recipeDao = AppDatabase.getDatabase(context.applicationContext).recipeDao()
    val suggestedRecipeDao = AppDatabase.getDatabase(context.applicationContext).suggestedRecipeDao()
    val favoriteRecipeDao = AppDatabase.getDatabase(context.applicationContext).favoriteRecipeDao()
    val firebaseAuth = Firebase.auth
    val firestore = Firebase.firestore

    val customScope = CoroutineScope(Dispatchers.IO)
    return RecipeRepository(
        apiService = apiService,
        recipeDao = recipeDao,
        favoriteRecipeDao = favoriteRecipeDao,
        suggestedRecipeDao = suggestedRecipeDao,
        firebaseAuth = firebaseAuth,
        externalScope = customScope,
        firestore = firestore
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    authViewModel: AuthViewModel,
    onNavigateUp: () -> Unit,
    onNavigateToDishMemories: (recipeId: Int, recipeTitle: String, recipeImageUrl: String?) -> Unit,
    viewModel: RecipeDetailViewModel = viewModel(
        factory = RecipeDetailViewModelFactory(
            owner = LocalSavedStateRegistryOwner.current,
            recipeRepository = getRecipeRepository(),
            defaultArgs = (LocalViewModelStoreOwner.current as? NavBackStackEntry)?.arguments,
            cookedDishRepository = getCookedDishRepositoryInstance()
        )
    )

) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val isEmailVerified by authViewModel.isEmailVerified.collectAsStateWithLifecycle()

    val user = currentUser
    // Determine if the user is signed in with email/password and needs verification
    val needsVerification = user != null && !isEmailVerified && user.providerData.any { it.providerId == "password" }
    // Determine if user can interact with features requiring sign-in and verification
    val canInteractWithUserFeatures = user != null && !needsVerification

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Refresh cook count only if user is in a state to see it
                if (canInteractWithUserFeatures) {
                    Log.d("RecipeDetailScreen", "ON_RESUME: Calling refreshCookCountIfNeeded (user verified)")
                    viewModel.refreshCookCountIfNeeded()
                } else {
                    Log.d("RecipeDetailScreen", "ON_RESUME: Skipping refreshCookCountIfNeeded (user not verified or null)")
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val titleText = when (val currentUiState = uiState) {
                        is RecipeDetailUiState.Success -> currentUiState.recipe.title
                        else -> "Recipe Detail"
                    }
                    Text(text = titleText.toString(),)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Show favorite toggle only if recipe loaded, user signed in, and verified
                    if (uiState is RecipeDetailUiState.Success && canInteractWithUserFeatures) {
                        val successState = uiState as RecipeDetailUiState.Success
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                imageVector = if (successState.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (successState.isFavorite) "Remove from favorites" else "Add to favorites",
                                tint = OnLightGreenApp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LightGreenApp,
                    titleContentColor = OnLightGreenApp,
                    navigationIconContentColor = OnLightGreenApp,
                    actionIconContentColor = OnLightGreenApp
                )
            )
        },
        floatingActionButton = {
            // Conditionally display FAB based on uiState
            val currentSuccessState = uiState as? RecipeDetailUiState.Success
            // Show FAB only if recipe loaded, showFab flag is true, user signed in, and verified
            if (currentSuccessState != null && currentSuccessState.showFab && canInteractWithUserFeatures) { // Check showFab flag
                val recipe = currentSuccessState.recipe
                if (recipe.title != null) { // Ensure title is not null for safety
                    FloatingActionButton(
                        onClick = {
                            // recipe.id, recipe.title, recipe.image are from the current UI state
                            viewModel.onCookDishFabClicked(
                                recipeId = recipe.id,      // Pass data TO the ViewModel
                                recipeTitle = recipe.title,
                                recipeImageUrl = recipe.image
                            ) { recipeIdFromVm, recipeTitleFromVm, recipeImageUrlFromVm ->
                                // This lambda is the onSuccessNavigation callback from the ViewModel.
                                // It RECEIVES data FROM the ViewModel.
                                // Now call the screen's navigation function with THIS data.
                                onNavigateToDishMemories(
                                    recipeIdFromVm,
                                    recipeTitleFromVm,
                                    recipeImageUrlFromVm
                                )
                            }
                        },
                        containerColor = LightGreenApp,
                        contentColor = OnLightGreenApp
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Log Cooked Dish / View Memories"
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // Check auth state before rendering main content or verification screen
        when (val currentUiState = uiState) {
            is RecipeDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is RecipeDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentUiState.message,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            is RecipeDetailUiState.Success -> {
                val recipe = currentUiState.recipe
                val timesCooked = currentUiState.timesCooked

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 16.dp)
                ) {
                    AsyncImage(
                        model = recipe.image,
                        contentDescription = recipe.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = R.drawable.image_placeholder_error),
                        placeholder = painterResource(id = R.drawable.image_placeholder_error)
                    )

                    SelectionContainer {
                        Column {
                            // Display "Times Cooked"
                            if (timesCooked > 0) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Restaurant,
                                        contentDescription = "Times cooked",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "You've made this $timesCooked time${if (timesCooked == 1) "" else "s"}!",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Text(
                                text = "Servings: ${recipe.servings}",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
                            )
                            Text(
                                text = "Ready in: ${recipe.readyInMinutes} minutes",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp)
                            )

                            // Ingredients
                            InfoSection(title = "Ingredients", items = recipe.extendedIngredients?.mapNotNull { it.original })

                            InfoSection(title = "Cuisines", items = recipe.cuisines)
                            InfoSection(title = "Diets/Intolerances", items = recipe.diets) // Diets often represent intolerances or lifestyle choices
                            InfoSection(title = "Dish Types", items = recipe.dishTypes)

                            WinePairingSection(winePairing = recipe.winePairing)

                            // Instructions
                            Text(
                                text = "Instructions",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                            )
                            recipe.analyzedInstructions?.forEach { instructionStepParent ->
                                if (instructionStepParent.name!!.isNotEmpty()) {
                                    Text(
                                        text = instructionStepParent.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
                                    )
                                }
                                instructionStepParent.steps!!.forEach { step ->
                                    Text(
                                        text = "${step.number}. ${step.step}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(start = if (instructionStepParent.name.isNotEmpty()) 24.dp else 16.dp, end = 16.dp, bottom = 8.dp)
                                    )
                                }
                            }

                            // Summary
                            if (!recipe.summary.isNullOrBlank()) {
                                Text(
                                    text = "Summary",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                                )
                                ProvideTextStyle(value = MaterialTheme.typography.bodyMedium) {
                                    FormattedClickableHtmlText(
                                        htmlContent = recipe.summary, // Pass the HTML string
                                        modifier = Modifier
                                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                                            .fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
