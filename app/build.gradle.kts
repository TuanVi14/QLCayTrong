plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.project.qlcaytrong"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.project.qlcaytrong"
        minSdk = 24
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Firebase BOM - quản lý version tập trung
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")

    // Room
    implementation("androidx.room:room-runtime:2.7.1")
    annotationProcessor("androidx.room:room-compiler:2.7.1")

    // ExifInterface — đọc EXIF rotation để rotate ảnh đúng chiều (ImageUtils)
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // ViewModel + LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime:2.8.7")

    // WorkManager
    implementation("androidx.work:work-runtime:2.10.1")

    // Navigation Component
    implementation("androidx.navigation:navigation-fragment:2.8.9")
    implementation("androidx.navigation:navigation-ui:2.8.9")

    // SwipeRefreshLayout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // ZXing QR Code
    // android-embedded = scanner UI (IntentIntegrator / CaptureActivity)
    implementation("com.journeyapps:zxing-android-embedded:4.3.0") { isTransitive = false }
    // core = generate QR Bitmap via MultiFormatWriter
    implementation("com.google.zxing:core:3.5.3")

    // Activity Result API (registerForActivityResult)
    implementation("androidx.activity:activity:1.10.1")

    // Glide — image loading + placeholder + error fallback
    // Firebase Storage đã có qua BOM (firebase-storage đã khai báo dòng 49)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Fragment KTX — viewModels() extension, fragment result API
    implementation("androidx.fragment:fragment:1.8.6")

    // WorkManager test (dev only)
    androidTestImplementation("androidx.work:work-testing:2.10.1")

    // SplashScreen API (Android 12+ back-compat)
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}