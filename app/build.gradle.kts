plugins {
    alias(libs.plugins.android.application)
    // This is a Java-only project, no Kotlin plugins needed
}

// Make sure to apply the Android Gradle Plugin correctly

// Configure Java toolchain for AGP 8.2.0
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

android {
    namespace = "com.example.shedulytic"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.shedulytic"
        minSdk = 24
        targetSdk = 35



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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Removed kotlinOptions block as this is a Java-only project

    buildFeatures {
        dataBinding = true
        viewBinding = true
        buildConfig = true
    }

    sourceSets {
        getByName("main") {
            java {
                srcDirs("src/main/java")
            }
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE*"
            excludes += "META-INF/NOTICE*"
        }
    }

    // Clean task configuration - simplified
    tasks.register("cleanBuildCache") {
        doLast {
            project.buildDir.deleteRecursively()
        }
    }
    
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    // Core Android dependencies
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.activity:activity:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.annotation:annotation:1.6.0")
    
    // Lifecycle components (Java versions)
    implementation("androidx.lifecycle:lifecycle-livedata:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.6.2")
    
    // Google Maps
    implementation("com.google.android.gms:play-services-maps:18.1.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    
    // Network & Data
    implementation("com.android.volley:volley:1.2.1")
    implementation("com.google.code.gson:gson:2.10.1")
    
    // UI Components
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.play.services.location)
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.core:core:1.12.0")
    
    // Test dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.3.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Room database
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    
    // OSMDroid for OpenStreetMap
    implementation("org.osmdroid:osmdroid-android:6.1.18")
}