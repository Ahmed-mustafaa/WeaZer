import org.gradle.internal.execution.caching.CachingState.enabled
import org.jetbrains.kotlin.storage.CacheResetOnProcessCanceled.enabled

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id ("kotlin-kapt")
    id ("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "com.example.weatherapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.weatherapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    dataBinding{
        isEnabled = true
    }

    buildFeatures {
        viewBinding = true
        dataBinding =true
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.games.activity)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.ui.desktop)
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.swiperefreshlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    val navVersion = "2.8.3"


// Views/Fragments Integration
    implementation("androidx.navigation:navigation-fragment:$navVersion")
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navVersion")

// Testing Navigation
    androidTestImplementation("androidx.navigation:navigation-testing:$navVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation("com.github.bumptech.glide:glide:4.15.1")
    kapt("com.github.bumptech.glide:compiler:4.15.1")
    implementation("com.github.bumptech.glide:okhttp3-integration:4.15.1")

    implementation("com.jakewharton.retrofit:retrofit2-kotlin-coroutines-adapter:0.9.2")
    implementation ("com.google.android.gms:play-services-location:21.0.1")


    // ViewModel
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")

    // LiveData
    implementation ("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")

    // Optional: if you are using SavedStateHandle with ViewModel
    implementation ("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.6.1")

    // Optional: for lifecycle and LiveData observation
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")

    implementation ("org.osmdroid:osmdroid-android:6.1.10")
    implementation ("org.osmdroid:osmdroid-android:6.1.10" )// Check for the latest version

}