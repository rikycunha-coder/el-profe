import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// En GitHub Actions cada compilación sube el número de versión, así el APK nuevo
// se instala encima del anterior sin perder las obras guardadas.
val numeroCompilacion = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

android {
    namespace = "com.elprofe.despiece"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.elprofe.despiece"
        minSdk = 26
        targetSdk = 35
        versionCode = numeroCompilacion
        versionName = "1.0.$numeroCompilacion"
    }

    signingConfigs {
        // Clave fija incluida en el repositorio para que todas las compilaciones tengan la
        // misma firma y se puedan actualizar. Para publicar en Google Play usa una clave propia.
        create("despiece") {
            storeFile = file("despiece.keystore")
            storePassword = "despiece"
            keyAlias = "despiece"
            keyPassword = "despiece"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("despiece")
        }
        debug {
            signingConfig = signingConfigs.getByName("despiece")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core"))

    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
}
