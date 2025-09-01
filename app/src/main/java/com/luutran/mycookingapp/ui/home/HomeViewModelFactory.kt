package com.luutran.mycookingapp.ui.home

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.luutran.mycookingapp.data.datastore.UserPreferencesRepository
import com.luutran.mycookingapp.data.network.SpoonacularApiService
import com.luutran.mycookingapp.data.repository.RecipeRepository
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiServiceInstance {
    val instance: SpoonacularApiService by lazy {
        Retrofit.Builder()
            .baseUrl(SpoonacularApiService.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SpoonacularApiService::class.java)
    }
}


class HomeViewModelFactory(private val application: Application,
                           private val recipeRepository: RecipeRepository,
                           private val userPreferencesRepository: UserPreferencesRepository,
                           //private val authViewModel: AuthViewModel
                           ) :

    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(
                application,
                recipeRepository,
                userPreferencesRepository,
                //authViewModel
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}