import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// En GitHub Actions cada compilación sube el número de versión, así el APK nuevo
// se instala encima del anterior sin perder los datos.
val numeroCompilacion = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

android {
    namespace = "com.elprofe.despiece"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.elprofe.despiece"
        minSdk = 26
        targetSdk = 35
        versionCode = numeroCompilacion
        versionName = "2.0.$numeroCompilacion"
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
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.webkit:webkit:1.11.0")
}
