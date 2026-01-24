plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.ksp)
}

// * Charger les propriétés locales depuis local.properties
val localProperties = java.util.Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

// * Fonction helper pour lire une propriété avec une valeur par défaut
fun getLocalProperty(key: String, defaultValue: String = ""): String {
    return localProperties.getProperty(key, defaultValue)
}

android {
    namespace = "com.frombeyond.r2sl"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.frombeyond.r2sl"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("../r2sl-release-key.keystore")
            storePassword = "r2sl2025!sign"
            keyAlias = "r2sl-key-alias"
            keyPassword = "r2sl2025!sign"
        }
    }

    buildTypes {
        debug {
            // Client ID pour le mode debug
            val debugClientId = getLocalProperty("GOOGLE_CLIENT_ID_DEBUG", "YOUR_DEBUG_CLIENT_ID_HERE")
            buildConfigField("String", "GOOGLE_CLIENT_ID_DEBUG", "\"$debugClientId\"")
            
            // Google API Key
            val apiKey = getLocalProperty("GOOGLE_API_KEY", "")
            buildConfigField("String", "GOOGLE_API_KEY", if (apiKey.isNotEmpty()) "\"$apiKey\"" else "null")
        }
        
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            signingConfig = signingConfigs.getByName("release")
            
            // Client ID pour le mode release
            val releaseClientId = getLocalProperty("GOOGLE_CLIENT_ID_RELEASE", "YOUR_RELEASE_CLIENT_ID_HERE")
            buildConfigField("String", "GOOGLE_CLIENT_ID_RELEASE", "\"$releaseClientId\"")
            
            // Google API Key
            val apiKey = getLocalProperty("GOOGLE_API_KEY", "")
            buildConfigField("String", "GOOGLE_API_KEY", if (apiKey.isNotEmpty()) "\"$apiKey\"" else "null")
        }
    }
    
    buildFeatures {
        viewBinding = true
        dataBinding = false  // Désactiver explicitement le dataBinding
        buildConfig = true  // Activer BuildConfig pour accéder aux constantes
    }
    
    // Configuration des noms d'APK
    // Note: Cette configuration est effectuée dans afterEvaluate pour éviter les problèmes de résolution
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    // Désactiver temporairement Lint pour la compilation
    lint {
        abortOnError = false
        checkReleaseBuilds = false  // Désactiver lint pour les builds release
    }
    
    // Résoudre les conflits de dépendances
    packaging {
        resources {
            excludes += setOf("META-INF/DEPENDENCIES", "META-INF/LICENSE", "META-INF/LICENSE.txt", "META-INF/NOTICE", "META-INF/NOTICE.txt")
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    
    // Base de données Room avec chiffrement
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("net.zetetic:android-database-sqlcipher:4.5.4")
    
    // Authentification Google OAuth
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.google.firebase:firebase-auth-ktx:22.3.1")
    implementation("com.google.firebase:firebase-analytics-ktx:21.5.0")
    
    // Chiffrement et sécurité
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    
    // Chargement d'images
    implementation("com.github.bumptech.glide:glide:4.16.0")
    
    // Google Drive API
    implementation("com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0")
    implementation("com.google.api-client:google-api-client-android:2.0.0")
    implementation("com.google.http-client:google-http-client-gson:1.43.3")
    
    // PDF Generation
    implementation("com.itextpdf:itext7-core:8.0.2")
    implementation("com.itextpdf:html2pdf:5.0.1")
    
    // Tests unitaires
    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    
    // Tests instrumentés
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.fragment:fragment-testing:1.6.2")
    androidTestImplementation("org.mockito:mockito-android:5.8.0")
}