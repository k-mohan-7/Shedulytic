plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.android)
}

// Apply the custom force clean script
apply(from = "forceclean.gradle.kts")

android {
    namespace = "com.example.shedulytic"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.shedulytic"
        minSdk = 24
        targetSdk = 34
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

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true
    }
    

    // Clean task configuration to force delete locked files
    tasks.withType<Delete> {
        delete.apply {
            setFollowSymlinks(true)
        }
    }
    
    // Prevent test task failures
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            all {
                it.jvmArgs("-XX:MaxMetaspaceSize=1536m", "-Xmx1536m", "--add-opens=java.base/java.lang=ALL-UNNAMED")
                it.maxHeapSize = "1536m"
            }
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.annotation)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.play.services.maps)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Data binding is now handled by the Android Gradle Plugin
    // Removed explicit databinding compiler dependency
}