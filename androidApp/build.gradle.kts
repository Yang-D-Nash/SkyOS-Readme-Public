import org.gradle.api.GradleException
import java.util.Properties

plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.plugin.compose")
    kotlin("plugin.serialization")
}

val releaseKeystoreProperties = Properties()
val releaseKeystorePropertiesFile = rootProject.file("keystore.properties")

if (releaseKeystorePropertiesFile.exists()) {
    releaseKeystorePropertiesFile.inputStream().use(releaseKeystoreProperties::load)
}

fun releaseSigningValue(primaryKey: String, legacyKey: String): String? =
    releaseKeystoreProperties.getProperty(primaryKey)
        ?: providers.environmentVariable(primaryKey).orNull
        ?: releaseKeystoreProperties.getProperty(legacyKey)
        ?: providers.environmentVariable(legacyKey).orNull

val releaseStoreFilePath = releaseSigningValue("SKYOS_UPLOAD_STORE_FILE", "SKYDOWN_UPLOAD_STORE_FILE")
val releaseStorePassword = releaseSigningValue("SKYOS_UPLOAD_STORE_PASSWORD", "SKYDOWN_UPLOAD_STORE_PASSWORD")
val releaseKeyAlias = releaseSigningValue("SKYOS_UPLOAD_KEY_ALIAS", "SKYDOWN_UPLOAD_KEY_ALIAS")
val releaseKeyPassword = releaseSigningValue("SKYOS_UPLOAD_KEY_PASSWORD", "SKYDOWN_UPLOAD_KEY_PASSWORD")
val allowDebugReleaseSigning =
    providers.gradleProperty("allowDebugReleaseSigning")
        .map { value -> value.equals("true", ignoreCase = true) }
        .orElse(false)
        .get()
val hasReleaseSigning =
    listOf(
        releaseStoreFilePath,
        releaseStorePassword,
        releaseKeyAlias,
        releaseKeyPassword,
    ).all { !it.isNullOrBlank() }

android {
    namespace = "com.skydown.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.skydown.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 10000
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFilePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig =
                when {
                    hasReleaseSigning -> signingConfigs.getByName("release")
                    allowDebugReleaseSigning -> signingConfigs.getByName("debug")
                    else -> null
                }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

val releaseSigningErrorMessage =
    """
    Android release signing is not configured.
    Copy keystore.properties.example to keystore.properties or set SKYOS_UPLOAD_* env vars.
    Legacy SKYDOWN_UPLOAD_* values are still accepted for existing local setups.
    For local non-store smoke tests only, you can pass -PallowDebugReleaseSigning=true.
    """.trimIndent()

tasks.configureEach {
    if (name in setOf("preReleaseBuild", "assembleRelease", "bundleRelease", "packageRelease", "validateSigningRelease")) {
        doFirst {
            if (!hasReleaseSigning && !allowDebugReleaseSigning) {
                throw GradleException(releaseSigningErrorMessage)
            }
        }
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(platform("com.google.firebase:firebase-bom:34.11.0"))

    val composeBom = platform("androidx.compose:compose-bom:2026.02.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.9.4")
    implementation("androidx.media3:media3-exoplayer:1.8.0")
    implementation("androidx.media3:media3-ui:1.8.0")
    implementation("com.google.android.material:material:1.13.0")
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    implementation("com.google.firebase:firebase-ai")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-config")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-functions")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.android.billingclient:billing-ktx:7.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("com.google.firebase:firebase-appcheck-debug")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
