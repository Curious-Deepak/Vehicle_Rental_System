plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.eazymile.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.eazymile.app"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
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
        kotlin {
            compilerOptions {
                jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
            }
        }
        dependenciesInfo {
            includeInApk = true
            includeInBundle = true
        }
    }


    dependencies {

        implementation(platform(libs.firebase.bom))

        // Firebase dependencies (auto-managed versions by BOM)
        implementation(libs.firebase.analytics)
        implementation(libs.firebase.auth.ktx)
        implementation(libs.firebase.database)

        // AndroidX and UI
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.appcompat)
        implementation(libs.material)
        implementation(libs.androidx.activity)
        implementation(libs.androidx.constraintlayout)
        implementation(libs.swiperefresh)
        implementation(libs.glide)
        implementation(libs.firebase.auth)
        annotationProcessor(libs.glide.compiler)

        // Lifecycle
        implementation(libs.androidx.lifecycle.livedata.ktx)
        implementation(libs.androidx.lifecycle.viewmodel.ktx)

        // Tests
        testImplementation(libs.junit)
        androidTestImplementation(libs.androidx.junit)
        androidTestImplementation(libs.androidx.espresso.core)
    }
}