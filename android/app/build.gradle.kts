plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.oauth.otp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.oauth.otp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    // --- Signing configuration ---
    // Loads key credentials from gradle.properties or environment variables.
    // Falls back to the development keystore committed alongside the project.
    signingConfigs {
        create("release") {
            val keystoreFile = file("../oauth2-release.keystore")
            storeFile = keystoreFile
            storePassword = providers.gradleProperty("OAUTH2_KEYSTORE_PASSWORD")
                .getOrElse("oauth2pass")
            keyAlias = providers.gradleProperty("OAUTH2_KEY_ALIAS")
                .getOrElse("oauth2-release")
            keyPassword = providers.gradleProperty("OAUTH2_KEY_PASSWORD")
                .getOrElse("oauth2pass")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Apply the release signing config
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Debug builds use the default debug keystore — no config needed.
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-ktx:1.9.1")
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("commons-codec:commons-codec:1.17.1")
}
