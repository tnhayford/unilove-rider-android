plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.serialization")
  id("com.google.devtools.ksp")
}

fun stringBuildConfig(name: String): String {
  val value = (project.findProperty(name) as String?)
    ?: System.getenv(name)
    ?: ""
  val escaped = value.replace("\\", "\\\\").replace("\"", "\\\"")
  return "\"$escaped\""
}

android {
  namespace = "com.unilove.rider"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.unilove.rider"
    minSdk = 26
    targetSdk = 35
    versionCode = 14
    versionName = "1.7.5"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    vectorDrawables.useSupportLibrary = true
    buildConfigField("String", "BASE_URL", "\"https://unilove.iderwell.com/\"")
    buildConfigField("String", "FIREBASE_PROJECT_ID", stringBuildConfig("FIREBASE_PROJECT_ID"))
    buildConfigField("String", "FIREBASE_APP_ID", stringBuildConfig("FIREBASE_APP_ID"))
    buildConfigField("String", "FIREBASE_API_KEY", stringBuildConfig("FIREBASE_API_KEY"))
    buildConfigField("String", "FIREBASE_SENDER_ID", stringBuildConfig("FIREBASE_SENDER_ID"))
  }

  flavorDimensions += "env"
  productFlavors {
    create("staging") {
      dimension = "env"
      applicationIdSuffix = ".staging"
      versionNameSuffix = "-staging"
    }
    create("production") {
      dimension = "env"
    }
  }

  signingConfigs {
    create("releasePlaceholder") {
      storeFile = file("../keystore/release.keystore")
      storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
      keyAlias = System.getenv("ANDROID_KEY_ALIAS")
      keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
    }
  }

  buildTypes {
    debug {
      applicationIdSuffix = ".debug"
      versionNameSuffix = "-debug"
    }
    release {
      isMinifyEnabled = true
      signingConfig = signingConfigs.getByName("releasePlaceholder")
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
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
    compose = true
    buildConfig = true
  }

  composeOptions {
    kotlinCompilerExtensionVersion = "1.5.14"
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

dependencies {
  val composeBom = platform("androidx.compose:compose-bom:2025.02.00")

  implementation("androidx.core:core-ktx:1.15.0")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
  implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
  implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
  implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
  implementation("androidx.activity:activity-compose:1.10.1")
  implementation("androidx.navigation:navigation-compose:2.8.8")

  implementation(composeBom)
  androidTestImplementation(composeBom)
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-tooling-preview")
  implementation("androidx.compose.material3:material3:1.3.1")
  implementation("androidx.compose.material:material-icons-extended")
  implementation("com.google.android.material:material:1.12.0")

  implementation("androidx.datastore:datastore-preferences:1.1.2")
  implementation("androidx.security:security-crypto-ktx:1.1.0-alpha06")
  implementation("androidx.work:work-runtime-ktx:2.9.1")

  implementation("com.squareup.retrofit2:retrofit:2.11.0")
  implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")
  implementation("com.squareup.okhttp3:okhttp:4.12.0")
  implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

  implementation("androidx.room:room-runtime:2.6.1")
  implementation("androidx.room:room-ktx:2.6.1")
  ksp("androidx.room:room-compiler:2.6.1")

  implementation("com.google.firebase:firebase-messaging-ktx:24.1.0")

  testImplementation("junit:junit:4.13.2")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
  testImplementation("com.google.truth:truth:1.4.4")

  androidTestImplementation("androidx.test.ext:junit:1.2.1")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
  androidTestImplementation("androidx.compose.ui:ui-test-junit4")

  debugImplementation("androidx.compose.ui:ui-tooling")
  debugImplementation("androidx.compose.ui:ui-test-manifest")
}
