import org.gradle.internal.jvm.Jvm
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.jvm.JvmTargetValidationMode
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("androidx.navigation.safeargs.kotlin")
    id("kotlin-kapt")
    alias(libs.plugins.google.gms.google.services)
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
}

val properties = Properties()
properties.load(FileInputStream(rootProject.file("local.properties")))

android {
    namespace = "com.example.grocerly"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.grocerly"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        resValue("string", "google_server_client_id", properties.getProperty("GOOGLE_SERVER_CLIENT_ID"))
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

   tasks.withType<KotlinJvmCompile>().configureEach {
       compilerOptions{
          jvmTarget.set(JvmTarget.JVM_11)
           freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
       }
   }

    buildFeatures{
        viewBinding = true
        dataBinding = true
    }

    kapt {
        correctErrorTypes = true
    }
}

dependencies {



    //navigation
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.androidx.navigation.dynamic.features.fragment)


    //firebase

    implementation(platform(libs.firebase.bom))
    implementation(libs.google.firebase.dynamic.links)
    implementation(libs.google.firebase.analytics)

    //livedata

        // ViewModel
        implementation(libs.androidx.lifecycle.viewmodel.ktx)
        // ViewModel utilities for Compose
        implementation(libs.androidx.lifecycle.viewmodel.compose)
        // LiveData
        implementation(libs.androidx.lifecycle.livedata.ktx)

        // Saved state module for ViewModel
        implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.storage)
    implementation(libs.appcompat)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)
    implementation(libs.firebase.functions)

    // Annotation processor
        kapt(libs.androidx.lifecycle.compiler)

    //dagger and hilt

    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)

    //glide
    implementation(libs.glide)

    //datastore
    implementation(libs.androidx.datastore.preferences)


    //lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    //coroutines
    implementation(libs.kotlinx.coroutines.android)

    //glide
    implementation(libs.glide)
    implementation(libs.ksp)

    //coil
    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)

    //shimmer
    implementation(libs.shimmer)

    implementation(libs.ccp)

    //gif
    implementation(libs.android.gif.drawable)

    //maps
    implementation (libs.play.services.location)
    implementation (libs.play.services.maps)

    //gson
    implementation(libs.gson)


    implementation(libs.stagestepbar)

   //room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    //razorpay
    implementation(libs.checkout)

    //facebook
    implementation(libs.facebook.login)

    //google play
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)


    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}