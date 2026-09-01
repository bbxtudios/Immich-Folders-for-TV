import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val versionPropsFile = file("version.properties")
val versionProps = Properties()

if (!versionPropsFile.exists()) {
    versionProps["VERSION_CODE"] = "1"
    versionProps["VERSION_MAJOR"] = "1"
    versionProps["VERSION_MINOR"] = "0"
    versionProps["VERSION_PATCH"] = "0"
    versionProps.store(FileOutputStream(versionPropsFile), "Version Configuration")
} else {
    versionProps.load(FileInputStream(versionPropsFile))
}

val isBuildingApk = gradle.startParameter.taskNames.any { 
    it.contains("assemble", ignoreCase = true) || it.contains("bundle", ignoreCase = true) 
}

var currentVersionCode = (versionProps["VERSION_CODE"] as? String)?.toIntOrNull() ?: 1
val major = (versionProps["VERSION_MAJOR"] as? String)?.toIntOrNull() ?: 1
val minor = (versionProps["VERSION_MINOR"] as? String)?.toIntOrNull() ?: 0
var patch = (versionProps["VERSION_PATCH"] as? String)?.toIntOrNull() ?: 0

if (isBuildingApk) {
    currentVersionCode += 1
    patch += 1
    versionProps["VERSION_CODE"] = currentVersionCode.toString()
    versionProps["VERSION_PATCH"] = patch.toString()
    versionProps.store(FileOutputStream(versionPropsFile), "Auto-incremented during build")
}

val currentVersionName = "$major.$minor.$patch"

android {
    namespace = "com.bbxtudios.immichtv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bbxtudios.immichtv"
        minSdk = 21
        targetSdk = 35
        versionCode = currentVersionCode
        versionName = currentVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ""
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.tv.material3.ExperimentalTvMaterial3Api",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.coil.compose)
    implementation(libs.coil.video)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.common)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.zxing.core)

    debugImplementation(libs.androidx.ui.tooling)
}
