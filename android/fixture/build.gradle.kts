// Test-only stand-in for Instagram, used to exercise the real accessibility pipeline on an
// emulator. It borrows Instagram's package name so the service will act on it, so install it
// only on an emulator without the real app. Never publish it.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "fake.fixture"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.instagram.android"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "test"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { jvmToolchain(17) }
}
