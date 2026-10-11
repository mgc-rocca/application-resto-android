import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}

val googleMapsApiKey = localProperties
    .getProperty("GOOGLE_MAPS_API_KEY", "")
val googlePlacesApiKey = localProperties
    .getProperty("GOOGLE_PLACES_API_KEY", googleMapsApiKey)
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

android {
    namespace = "fr.martinrocca.resto"
    compileSdk = 35

    defaultConfig {
        applicationId = "fr.martinrocca.resto"
        minSdk = 26
        targetSdk = 35
        versionCode = 8
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] = googleMapsApiKey
        buildConfigField("String", "GOOGLE_PLACES_API_KEY", "\"$googlePlacesApiKey\"")

        val publicDocumentsRef = localProperties.getProperty("RESTO_PUBLIC_DOCUMENTS_REF", "master")
        buildConfigField("String", "PUBLIC_DOCUMENTS_BASE_URL", "\"https://github.com/mgc-rocca/application-resto-android/blob/$publicDocumentsRef/docs/\"")

        vectorDrawables.useSupportLibrary = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    lint {
        checkTestSources = false
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.google.maps)
    implementation(libs.google.places)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.appcompat)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

android.sourceSets.getByName("androidTest").assets.srcDirs("$projectDir/schemas")

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
