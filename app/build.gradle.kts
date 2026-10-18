import java.util.Properties

// Módulo principal de la aplicación Wallance.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

// La clave de firma NO se versiona. Si existe un archivo local firma.properties
// se usa para firmar la versión de producción; si no, se compila en depuración.
val archivoFirma = rootProject.file("firma.properties")
val propiedadesFirma = Properties().apply {
    if (archivoFirma.exists()) {
        archivoFirma.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.andrespar2.wallance"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.andrespar2.wallance"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (propiedadesFirma.isNotEmpty()) {
            create("produccion") {
                storeFile = file(propiedadesFirma.getProperty("almacen"))
                storePassword = propiedadesFirma.getProperty("claveAlmacen")
                keyAlias = propiedadesFirma.getProperty("alias")
                keyPassword = propiedadesFirma.getProperty("claveLlave")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".dep"
            isMinifyEnabled = false
            versionNameSuffix = "-depuración"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (propiedadesFirma.isNotEmpty()) {
                signingConfig = signingConfigs.getByName("produccion")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
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

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Núcleo AndroidX
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Persistencia
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Inyección de dependencias
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Tareas en segundo plano
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Seguridad
    implementation("androidx.biometric:biometric:1.1.0")

    // Red y serialización
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Publicidad Start.io
    implementation("com.startapp:inapp-sdk:5.3.1")

    // Desaforamiento de java.time para dispositivos antiguos
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")

    // Pruebas unitarias
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")

    // Pruebas instrumentadas
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
