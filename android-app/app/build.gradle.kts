plugins {
    id("com.android.application")
}

android {
    namespace = "com.uhackman.tinyfish"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.uhackman.tinyfish"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "0.4.0-gecko"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// The newest GeckoView metadata currently pulls AndroidX artifacts that ask
// for an unpublished API 37 platform. Keep compatible 1.18/1.9 artifacts
// while compiling against the available API 36 SDK.
configurations.configureEach {
    resolutionStrategy.force(
        "androidx.core:core:1.18.0",
        "androidx.core:core-ktx:1.18.0",
        "androidx.annotation:annotation:1.9.1"
    )
}

dependencies {
    implementation("org.mozilla.geckoview:geckoview:158.0.20261019185856")
}
