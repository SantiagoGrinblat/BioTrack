plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.jetbrains.kotlin.serialization)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.santidev.biotrack"
  compileSdk {
    version = release(37) {
      minorApiLevel = 1
    }
  }
  
  defaultConfig {
    applicationId = "com.santidev.biotrack"
    minSdk = 24
    targetSdk = 37
    versionCode = 1
    versionName = "1.0"
    
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }
  
  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
  }
}

dependencies {
  
  implementation(libs.material.icons.extended)
  
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.auth)
  implementation(libs.firebase.firestore)
  
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)
  
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.kotlinx.serialization.json)
  
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.play.services)
  implementation(libs.kotlinx.coroutines.test)
  
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services.auth)
  implementation(libs.googleid)
  
  implementation(libs.mlkit.document.scanner)
  implementation(libs.mlkit.text.recognition)
  
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  testImplementation(libs.junit)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
}