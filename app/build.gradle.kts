import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

val keystorePropertiesFile = rootProject.file("local.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}


android {
    namespace = "com.shradhaabhishek.weddingtodos"
    compileSdk = 37

    defaultConfig {
        applicationId = "wedding.todo"
        minSdk = 28
        targetSdk = 37
        versionCode = 2
        versionName = "v1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
     // 2. Define    the signing configuration
    signingConfigs {
        val keystorePath = keystoreProperties.getProperty("KEYSTORE_PATH")
        val keystorePassword = keystoreProperties.getProperty("KEYSTORE_PASSWORD")
        val keyAliasVal = keystoreProperties.getProperty("KEY_ALIAS")
        val keyPasswordVal = keystoreProperties.getProperty("KEY_PASSWORD")

        if (keystorePath != null && keystorePassword != null && keyAliasVal != null && keyPasswordVal != null) {
            val keystoreFile = file(keystorePath)
            if (keystoreFile.exists()) {
                create("release") {
                    storeFile = keystoreFile
                    storePassword = keystorePassword
                    keyAlias = keyAliasVal
                    keyPassword = keyPasswordVal
                }
            } else {
                println("WARNING: Release keystore file not found at $keystorePath. Release build will not be signed correctly.")
            }
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    buildTypes {
        release {
            signingConfigs.findByName("release")?.let {
                signingConfig = it
            }
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
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:34.16.0"))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.play.services.auth)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}