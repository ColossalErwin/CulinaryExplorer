package com.luutran.mycookingapp.ui.recipedetail


import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.set
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.luutran.mycookingapp.R // For placeholder/error drawables
import com.luutran.mycookingapp.data.model.UserRecipe
import com.luutran.mycookingapp.data.repository.UserRecipeRepository // For ViewModel factory
import com.luutran.mycookingapp.data.utils.getUserRecipeRepositoryInstance
import com.luutran.mycookingapp.navigation.NavDestinations
import com.luutran.mycookingapp.ui.theme.LightGreenApp // Your theme colors
import com.luutran.mycookingapp.ui.theme.OnLightGreenApp // Your theme colors
import kotlin.collections.remove


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserRecipeDetailScreen(
    onNavigateUp: () -> Unit,
    navController: NavController,
    onNavigateToEditUserRecipe: (userRecipeId: String) -> Unit,
    viewModel: UserRecipeDetailViewModel = viewModel(
        factory = UserRecipeDetailViewModel.provideFactory(
            userRecipeRepository = getUserRecipeRepositoryInstance()
        )
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val currentBackStackEntry = navController.currentBackStackEntry
    val recipeUpdatedForDetail = currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<Boolean>(NavDestinations.NavigationResultKeys.RECIPE_UPDATED_FOR_DETAIL_KEY)
        ?.observeAsState()

    val updatedRecipeIdFromEdit = currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<String>(NavDestinations.NavigationResultKeys.RECIPE_ID_KEY)
        ?.observeAsState()


    LaunchedEffect(recipeUpdatedForDetail?.value, updatedRecipeIdFromEdit?.value) {
        if (recipeUpdatedForDetail?.value == true && updatedRecipeIdFromEdit?.value != null) {
            val recipeId = updatedRecipeIdFromEdit.value!!
            Log.d("UserRecipeDetailScreen", "Recipe (ID: $recipeId) updated result received. Refreshing.")

            // 1. Refresh its own details
            if (viewModel.userRecipeId == recipeId) {
                viewModel.fetchUserRecipeDetails()
            } else {
                Log.w("UserRecipeDetailScreen", "Received update for $recipeId, but VM is for ${viewModel.userRecipeId}")
                viewModel.fetchUserRecipeDetails()
            }


            // 2. Set a result for UserRecipeMemoriesScreen
            navController.previousBackStackEntry
                ?.savedStateHandle
                ?.set(NavDestinations.NavigationResultKeys.RECIPE_UPDATED_FOR_MEMORIES_KEY, true)
            navController.previousBackStackEntry // Also pass the ID
                ?.savedStateHandle
                ?.set(NavDestinations.NavigationResultKeys.RECIPE_ID_KEY, recipeId)

            Log.d("UserRecipeDetailScreen", "Set update result for Memories screen for recipe ID: $recipeId")

            // 3.
            currentBackStackEntry.savedStateHandle.remove<Boolean>(NavDestinations.NavigationResultKeys.RECIPE_UPDATED_FOR_DETAIL_KEY)
            currentBackStackEntry.savedStateHandle.remove<String>(NavDestinations.NavigationResultKeys.RECIPE_ID_KEY)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val titleText = when (val currentUiState = uiState) {
                        is UserRecipeDetailUiState.Success -> currentUiState.userRecipe.title.take(30) + if (currentUiState.userRecipe.title.length > 30) "..." else ""
                        else -> "Recipe Details"
                    }
                    Text(text = titleText)
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Show Edit action only when recipe details are successfully loaded
                    if (uiState is UserRecipeDetailUiState.Success) {
                        val recipeId = (uiState as UserRecipeDetailUiState.Success).userRecipe.id
                        if (recipeId.isNotBlank()) { // Ensure ID is valid
                            IconButton(onClick = {
                                onNavigateToEditUserRecipe(recipeId)
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Edit Recipe"
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LightGreenApp,
                    titleContentColor = OnLightGreenApp,
                    navigationIconContentColor = OnLightGreenApp
                )
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (val currentUiState = uiState) {
                is UserRecipeDetailUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is UserRecipeDetailUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Error: ${currentUiState.message}",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                is UserRecipeDetailUiState.Success -> {
                    UserRecipeDetailContent(userRecipe = currentUiState.userRecipe)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserRecipeDetailContent(userRecipe: UserRecipe) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
        // Image Slider
        if (userRecipe.imageUrls.isNotEmpty()) {
            val pagerState = rememberPagerState(pageCount = { userRecipe.imageUrls.size })
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(userRecipe.imageUrls[pageIndex])
                            .crossfade(true)
                            .placeholder(R.drawable.placeholder_dish)
                            .error(R.drawable.placeholder_dish)
                            .build(),
                        contentDescription = "${userRecipe.title} image ${pageIndex + 1}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Page Indicator
                if (userRecipe.imageUrls.size > 1) {
                    Row(
                        Modifier
                            .height(20.dp)
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(pagerState.pageCount) { iteration ->
                            val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .size(8.dp)
                            )
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.placeholder_dish),
                    contentDescription = "No image available",
                    modifier = Modifier.size(100.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = userRecipe.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (!userRecipe.description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = userRecipe.description!!,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Timings and Servings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            userRecipe.prepTimeMinutes?.let { InfoChip("Prep: $it min") }
            userRecipe.cookTimeMinutes?.let { InfoChip("Cook: $it min") }
            userRecipe.totalTimeMinutes?.let { InfoChip("Total: $it min") }
        }
        userRecipe.servings?.let {
            Spacer(modifier = Modifier.height(8.dp))
            InfoChip("Servings: $it", modifier = Modifier.padding(horizontal = 16.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Ingredients
        if (userRecipe.ingredients.isNotEmpty()) {
            SectionTitle("Ingredients", modifier = Modifier.padding(horizontal = 16.dp))
            userRecipe.ingredients.forEach { ingredient ->
                ListItem(text = ingredient, modifier = Modifier.padding(start = 24.dp, end = 16.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Instructions
        if (userRecipe.instructions.isNotEmpty()) {
            SectionTitle("Instructions", modifier = Modifier.padding(horizontal = 16.dp))
            userRecipe.instructions.forEachIndexed { index, instruction ->
                ListItem(text = "${index + 1}. $instruction", modifier = Modifier.padding(start = 24.dp, end = 16.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Category, Cuisine, Tags, Notes, etc. (add similarly)
        DetailItem("Category", userRecipe.category)
        DetailItem("Cuisine", userRecipe.cuisine)
        if (userRecipe.tags.isNotEmpty()) {
            DetailItem("Tags", userRecipe.tags.joinToString(", "))
        }
        if (userRecipe.dishTypes?.isNotEmpty() == true) {
            DetailItem("Dish Types", userRecipe.dishTypes!!.joinToString(", "))
        }
        if (userRecipe.diets?.isNotEmpty() == true) {
            DetailItem("Diets", userRecipe.diets!!.joinToString(", "))
        }
        DetailItem("Notes", userRecipe.notes, isMultiline = true)

        userRecipe.createdAt?.let {
            DetailItem("Created", DateUtils.formatTimestamp(it, "dd MMM yyyy, hh:mm a"))
        }
        userRecipe.updatedAt?.let {
            DetailItem("Last Updated", DateUtils.formatTimestamp(it, "dd MMM yyyy, hh:mm a"))
        }

    }
}

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun ListItem(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier.padding(bottom = 4.dp)
    )
}

@Composable
fun InfoChip(text: String, modifier: Modifier = Modifier) {
    if (text.isNotEmpty() && !text.contains("null")) {
        Surface(
            modifier = modifier.padding(vertical = 4.dp),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            tonalElevation = 1.dp
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun DetailItem(label: String, value: String?, isMultiline: Boolean = false, @SuppressLint("ModifierParameter") modifier: Modifier = Modifier) {
    if (!value.isNullOrBlank()) {
        Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = if (isMultiline) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                maxLines = if (isMultiline) Int.MAX_VALUE else 10 // Allow more lines for notes
            )
        }
    }
}

object DateUtils {
    fun formatTimestamp(timestamp: com.google.firebase.Timestamp?, pattern: String): String {
        return timestamp?.toDate()?.let {
            java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault()).format(it)
        } ?: "N/A"
    }
}