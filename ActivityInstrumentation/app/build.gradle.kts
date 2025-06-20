plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = 35
    defaultConfig {
        minSdk = 21
        targetSdk = 35
    }
    defaultConfig {
        applicationId = "com.example.android.activityinstrumentation"
    }
    dependencies {
        implementation("androidx.activity:activity-ktx:1.10.1")
        implementation ("androidx.appcompat:appcompat:1.7.1")
        implementation ("androidx.legacy:legacy-support-v4:1.0.0")
        implementation ("androidx.core:core-ktx:1.16.0")
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.txt"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    namespace = "com.example.android.activityinstrumentation"
}
