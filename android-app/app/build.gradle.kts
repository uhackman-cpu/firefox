plugins {
    id("com.android.application")
}

android {
    namespace = "com.uhackman.tinyfish"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.uhackman.tinyfish"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0-gecko"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Mozilla GeckoView bundles Gecko, the rendering engine used by Firefox.
    implementation("org.mozilla.geckoview:geckoview:158.0.20261019185856")
}
