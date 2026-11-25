plugins {
    id("com.android.application")
}

android {
    namespace = "br.com.leitorqr"
    compileSdk = 37

    defaultConfig {
        applicationId = "br.com.leitorqr"
        minSdk = 26
        targetSdk = 37
        versionCode = 5
        versionName = "1.3.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("com.google.android.material:material:1.14.0")

    // Câmera (CameraX)
    val cameraxVersion = "1.7.0"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // Leitor de QR code do Google (modelo embutido no app, funciona offline)
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
}
