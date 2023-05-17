plugins {
    @Suppress("DSL_SCOPE_VIOLATION")
    alias(libs.plugins.com.android.application)
    @Suppress("DSL_SCOPE_VIOLATION")
    alias(libs.plugins.org.jetbrains.kotlin.android)

    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "ro.bankar.app"
    compileSdk = 33

    defaultConfig {
        applicationId = "ro.bankar.app"
        minSdk = 24
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            manifestPlaceholders["useCleartextTraffic"] = true
        }
        release {
            manifestPlaceholders["useCleartextTraffic"] = false
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.6"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
	implementation(project(":common"))

    coreLibraryDesugaring(libs.com.android.tools.desugaring)
    implementation(libs.maxkeppeler.compose.core)
    implementation(libs.maxkeppeler.compose.calendar)
    implementation(libs.io.coil)
    implementation(libs.image.cropper)
    implementation(libs.shimmer.compose)
    implementation(libs.compose.settings)
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.activity.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.androidx.constraintlayout.compose)
    implementation(libs.accompanist.navigation.animation)
    implementation(libs.accompanist.swipe.refresh)
    implementation(libs.io.ktor.client)
    implementation(libs.io.ktor.client.okhttp)
    implementation(libs.io.ktor.content.negotiation)
    implementation(libs.io.ktor.serialization.json)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}