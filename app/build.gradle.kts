plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    //id("com.google.devtools.ksp")
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.luutran.mycookingapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.luutran.mycookingapp"
        minSdk = 28
        //noinspection OldTargetApi
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage.ktx)

    implementation(libs.play.services.auth)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.androidx.compose.material)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Retrofit
    implementation(libs.retrofit) // Check for the latest version

    // Gson converter (you can use Moshi or Jackson too)
    implementation(libs.converter.gson) // Use the same version as Retrofit

    // OkHttp (Retrofit uses OkHttp for HTTP calls, often good to include for logging interceptor)
    implementation(libs.okhttp) // Check for the latest version
    implementation(libs.logging.interceptor) // For logging API requests/responses

    // Kotlin Coroutines (for asynchronous calls)
    implementation(libs.kotlinx.coroutines.android) // Check for the latest version
    implementation(libs.kotlinx.coroutines.core)

    //Coil image loader
    implementation(libs.coil.compose)

    //ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.datastore.preferences) // Use the latest version


    // Optional - if you use Flow extensively in your ViewModels, you might already have this
    implementation(libs.androidx.lifecycle.runtime.ktx) // For viewModelScope, collectAsStateWithLifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    implementation(libs.androidx.navigation.compose) // Or the latest version

    // For Lifecycle ViewModel Compose (often used with Navigation)
    implementation(libs.androidx.lifecycle.viewmodel.compose) // Or the latest version

    // For collecting Flows as State with Lifecycle awareness (recommended)
    implementation(libs.lifecycle.runtime.compose)

    implementation(libs.androidx.material.icons.extended)

    // ...
    implementation(libs.androidx.room.runtime)
    //To use Kotlin Symbol Processing (KSP) for Room
    ksp(libs.androidx.room.compiler)
    annotationProcessor(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    // Add this for LiveData <-> Compose State integration
    implementation("androidx.compose.runtime:runtime-livedata")
}