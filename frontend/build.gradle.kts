@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.plugin.serialization)

    // For Firebase
    id("com.google.gms.google-services") version "4.4.2"
}

android {
    namespace = "ro.bankar.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "ro.bankar.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 3
        versionName = "0.2.0"

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
            signingConfig = signingConfigs["debug"]
        }
    }
    applicationVariants.all {
        val variant = this
        variant.outputs.map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }.forEach { output ->
            output.outputFileName = "BanKAR-${variant.versionName}-${variant.name}.apk"
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
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    bundle {
        language.enableSplit = false
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
    implementation(libs.maxkeppeler.compose.state)
    implementation(libs.maxkeppeler.compose.info)
    implementation(libs.maxkeppeler.compose.input)
    implementation(libs.io.coil)
    implementation(libs.image.cropper)
    implementation(libs.shimmer.compose)
    implementation(libs.compose.settings)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.navigation)
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
    implementation(libs.accompanist.swipe.refresh)
    implementation(libs.accompanist.permissions)
    implementation(libs.io.ktor.client)
    implementation(libs.io.ktor.client.okhttp)
    implementation(libs.io.ktor.content.negotiation)
    implementation(libs.io.ktor.serialization.json)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)
    implementation(libs.vico.compose.m3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}