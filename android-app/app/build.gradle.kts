import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.uhackman.tinyfish"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.uhackman.tinyfish"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "0.6.0-gecko"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

configurations.configureEach {
    resolutionStrategy.force(
        "androidx.core:core:1.18.0",
        "androidx.core:core-ktx:1.18.0",
        "androidx.annotation:annotation:1.9.1"
    )
}

dependencies {
    // GeckoView embeds Mozilla's Gecko engine, also used in Firefox.
    implementation("org.mozilla.geckoview:geckoview:157.0.20261005135250")
}
