@file:Suppress("UnstableApiUsage")

import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.room)
    alias(libs.plugins.ktfmt.gradle)
    alias(libs.plugins.chaquopy)
}

val keystorePropertiesFile: File = rootProject.file("keystore.properties")

val splitApks = !project.hasProperty("noSplits")

val abiFilterList = (properties["ABI_FILTERS"] as String).split(';')

val abiCodes = mapOf("armeabi-v7a" to 1, "arm64-v8a" to 2, "x86" to 3, "x86_64" to 4)

val baseVersionName = currentVersion.name
val currentVersionCode = currentVersion.code.toInt()

android {
    compileSdk = 37

    signingConfigs {
        // Public debug-only key committed with the fork.
        //
        // This is intentional: GitHub Actions runners are ephemeral and otherwise create a new
        // ~/.android/debug.keystore on different runs. Android then rejects the next debug APK as
        // an update because its certificate changed.
        //
        // NEVER use this config for release builds.
        create("kirinDebug") {
            keyAlias = "kirin-debug"
            keyPassword = "kirindebug"
            storeFile = file("keystore/kirin-debug.jks")
            storePassword = "kirindebug"
        }

        if (keystorePropertiesFile.exists()) {
            val keystoreProperties = Properties()
            keystoreProperties.load(FileInputStream(keystorePropertiesFile))
            create("githubPublish") {
                keyAlias = keystoreProperties["keyAlias"].toString()
                keyPassword = keystoreProperties["keyPassword"].toString()
                storeFile = file(keystoreProperties["storeFile"]!!)
                storePassword = keystoreProperties["storePassword"].toString()
            }
        }
    }

    buildFeatures { buildConfig = true }

    defaultConfig {
        applicationId = "com.kirin.downloader"
        minSdk = 24
        targetSdk = 37
        versionCode = 301_000_400
        check(versionCode == currentVersionCode)

        versionName = baseVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // Chaquopy embeds a native Python runtime, so it needs an explicit ABI list.
        // Keep this identical to the existing APK split coverage.
        ndk {
            abiFilters += if (splitApks) {
                listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            } else {
                abiFilterList
            }
        }

        if (splitApks) {
            splits {
                abi {
                    isEnable = true
                    reset()
                    include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
                    isUniversalApk = true
                }
            }
        }
    }

    room { schemaDirectory("$projectDir/schemas") }
    ksp { arg("room.incremental", "true") }

    androidComponents {
        onVariants { variant ->
            variant.outputs.forEach { output ->
                val name =
                    if (splitApks) {
                        output.filters
                            .find {
                                it.filterType ==
                                    com.android.build.api.variant.FilterConfiguration.FilterType.ABI
                            }
                            ?.identifier
                    } else {
                        abiFilterList.firstOrNull()
                    }

                val baseAbiCode = abiCodes[name]

                if (baseAbiCode != null) {
                    output.versionCode.set(baseAbiCode + (output.versionCode.get() ?: 0))
                }

                output.outputFileName.set(
                    "KirinDL-${baseVersionName}-${name ?: "universal"}.apk"
                )
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )

            // Release signing stays completely separate from the public debug key.
            // A real release is signed only when this fork later provides its own
            // keystore.properties.
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("githubPublish")
            }
        }

        debug {
            // Stable certificate for every GitHub Actions debug build of this fork.
            signingConfig = signingConfigs.getByName("kirinDebug")
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    flavorDimensions += "publishChannel"

    productFlavors {
        create("generic") {
            dimension = "publishChannel"
            isDefault = true
        }
    }

    lint { disable.addAll(listOf("MissingTranslation", "ExtraTranslation", "MissingQuantity")) }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
        jniLibs { useLegacyPackaging = true }
    }
    androidResources { generateLocaleConfig = true }

    namespace = "com.junkfood.seal"
}

chaquopy {
    defaultConfig {
        // Python 3.11 keeps both 32-bit and 64-bit Android ABIs available.
        version = "3.11"
        pip {
            // requests is the required Python dependency bundled for gallery-dl.
            install("requests==2.32.5")
        }
    }
}

ktfmt { kotlinLangStyle() }

kotlin {
    jvmToolchain(21)

    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
    }
}

dependencies {
    implementation(project(":color"))

    implementation(libs.bundles.core)

    implementation(libs.androidx.lifecycle.runtimeCompose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.androidxCompose)

    implementation(libs.coil.kt.compose)
    implementation(libs.coil.kt.network.okhttp)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.okhttp)

    implementation(libs.bundles.youtubedlAndroid)

    implementation(libs.mmkv)

    implementation(libs.androidx.documentfile)

    // AndroidX WebKit — provides WebViewCompat.addDocumentStartJavaScript().
    implementation("androidx.webkit:webkit:1.16.0")

    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.espresso.core)
    implementation(libs.androidx.compose.ui.tooling)
}
