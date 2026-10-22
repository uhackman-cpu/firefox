plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.uhackman.tinyfish"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.uhackman.tinyfish"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0-gecko"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // GeckoView is Mozilla's embeddable Gecko engine, the engine used by Firefox.
    implementation("org.mozilla.geckoview:geckoview:158.0.20261019185856")
}
