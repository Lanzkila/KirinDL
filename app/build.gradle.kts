plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.chaquo.python")
}

val kirinKeystorePath = System.getenv("KIRIN_KEYSTORE_FILE")
val kirinKeystorePassword = System.getenv("KIRIN_KEYSTORE_PASSWORD")
val kirinKeyAlias = System.getenv("KIRIN_KEY_ALIAS")
val kirinKeyPassword = System.getenv("KIRIN_KEY_PASSWORD")

android {
    namespace = "com.kirin.downloader"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kirin.downloader"
        minSdk = 24
        targetSdk = 35
        versionCode = 8
        versionName = "1.0.0-full-batch7"

        // Universal APK: package every ABI supplied by native dependencies.
    }

    signingConfigs {
        create("release") {
            if (!kirinKeystorePath.isNullOrBlank()) {
                storeFile = file(kirinKeystorePath)
            }
            storePassword = kirinKeystorePassword
            keyAlias = kirinKeyAlias
            keyPassword = kirinKeyPassword
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isDebuggable = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

chaquopy {
    defaultConfig {
        version = "3.13"

        pip {
            install("gallery-dl==1.32.9")
        }
    }
}

dependencies {
    val youtubedlAndroid = "0.18.1"

    implementation("io.github.junkfood02.youtubedl-android:library:$youtubedlAndroid")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:$youtubedlAndroid")
    implementation("io.github.junkfood02.youtubedl-android:aria2c:$youtubedlAndroid")
}

kotlin {
    jvmToolchain(17)
}
