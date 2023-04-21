plugins {
    kotlin("multiplatform")
    id("com.android.library")
}


kotlin {
	jvm()
	android()
}

android {
	namespace = "ro.bankar.lib"
    compileSdk = 33

    defaultConfig {
        minSdk = 24
        targetSdk = 33
    }
	compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}