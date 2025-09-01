package com.luutran.mycookingapp.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


object NetworkModule {

    private fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor) // Add for debugging
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(SpoonacularApiService.BASE_URL)
            .client(provideOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun provideSpoonacularApiService(retrofit: Retrofit): SpoonacularApiService {
        return retrofit.create(SpoonacularApiService::class.java)
    }

    val spoonacularApiService: SpoonacularApiService by lazy {
        provideSpoonacularApiService(provideRetrofit())
    }
}