plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = 34
    defaultConfig {
        applicationId = "com.example.android.notepad"
        minSdk = 19
        targetSdk = 34
        testApplicationId = "com.example.android.notepad.tests"
        testInstrumentationRunner = "android.test.InstrumentationTestRunner"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    namespace = "com.example.android.notepad"
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
}
