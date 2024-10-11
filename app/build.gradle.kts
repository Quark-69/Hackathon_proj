plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.hack.hackathon_proj"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.hack.hackathon_proj"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.rootbeer)
    implementation(libs.play.services.mlkit.face.detection)

    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation ("org.jmdns:jmdns:3.5.12")
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    implementation ("org.json:json:20240303")
    implementation ("commons-io:commons-io:2.17.0")
}