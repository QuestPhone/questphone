plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "nethical.questphone.ai"
    compileSdk = 36

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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

    sourceSets["main"].jniLibs.srcDirs("src/main/jniLibs")

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

    ndkVersion = "29.0.13599879 rc2"
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
tasks.register<Exec>("buildSentencePiece") {
    workingDir = file("$projectDir/src/main/sentencepiece")
    environment("ANDROID_NDK", android.ndkDirectory.absolutePath)
    environment("ANDROID_NDK", android.ndkDirectory.absolutePath)
    environment("OUTPUT_DIR", "${layout.buildDirectory}/sentencepiece_output")
    environment("SENTENCEPIECE_SRC", workingDir.absolutePath)
    environment("ANDROID_API", "21")
    environment("ABIS", "armeabi-v7a arm64-v8a x86 x86_64")
    commandLine("bash", "build.sh")
    onlyIf {
        val outputDir = file("$projectDir/src/main/sentencepiece/")
        val abis = listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        abis.any { abi ->
            !file("$outputDir/build_$abi/src/libsentencepiece.so").exists()
        }
    }
}

// Make preBuild depend on it
tasks.named("preBuild") {
    dependsOn("buildSentencePiece")
}
