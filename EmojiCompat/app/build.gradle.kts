import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.android.emojicompat"
        minSdk = 21
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }
    namespace = "com.example.android.emojicompat"
    configurations.configureEach {
        resolutionStrategy.force("com.android.support:support-annotations:28.0.0")
    }
}

dependencies {
    implementation("androidx.activity:activity-ktx:1.10.1")
    // Support Libraries
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.emoji:emoji:1.1.0")
    implementation("androidx.emoji:emoji-appcompat:1.1.0")
    implementation("androidx.emoji:emoji-bundled:1.1.0")
    implementation("androidx.core:core-ktx:1.16.0")

    // Test
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}
