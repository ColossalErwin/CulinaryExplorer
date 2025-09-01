package com.luutran.mycookingapp.data.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.luutran.mycookingapp.data.datastore.UserPreferencesRepository
import com.luutran.mycookingapp.data.repository.CookedDishRepository
import com.luutran.mycookingapp.data.repository.UserRecipeRepository

@Composable
fun getCookedDishRepositoryInstance(): CookedDishRepository {
    return CookedDishRepository()
}
fun getUserRecipeRepositoryInstance(): UserRecipeRepository {
    return UserRecipeRepository()
}

@Composable
fun getUserPreferencesRepositoryInstanceComposable(): UserPreferencesRepository {
    val context = LocalContext.current.applicationContext
    return remember {
        UserPreferencesRepository(context)
    }
}