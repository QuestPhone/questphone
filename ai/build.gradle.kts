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

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

    ndkVersion = "25.2.9519653"
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
    val spDir = file("$projectDir/src/main/sentencepiece")
    val outDir = "${layout.buildDirectory.get().asFile.absolutePath}/sentencepiece_output"
    val ndkDir = android.ndkDirectory.absolutePath
    val sdkCmakeBin = file("${android.sdkDirectory}/cmake")
        .listFiles()
        ?.sortedByDescending { it.name }
        ?.map { file("${it.absolutePath}/bin") }
        ?.firstOrNull { file("${it.absolutePath}/cmake").exists() }
        ?.absolutePath
    val llvmStrip = file("$ndkDir/toolchains/llvm/prebuilt")
        .listFiles()
        ?.map { file("${it.absolutePath}/bin/llvm-strip") }
        ?.firstOrNull { it.exists() }
        ?.absolutePath

    workingDir = spDir
    environment("ANDROID_NDK", ndkDir)
    environment("OUTPUT_DIR", outDir)
    environment("SENTENCEPIECE_SRC", spDir.absolutePath)
    environment("ANDROID_API", "21")
    environment("ABIS", "armeabi-v7a arm64-v8a x86 x86_64")
    environment("CMAKE_POLICY_VERSION_MINIMUM", "3.5")
    environment("LDFLAGS", "-Wl,-z,max-page-size=16384")
    if (llvmStrip != null) {
        environment("LLVM_STRIP", llvmStrip)
    }
    if (sdkCmakeBin != null && !System.getProperty("os.name").lowercase().contains("win")) {
        val currentPath = System.getenv("PATH") ?: ""
        environment("PATH", if (currentPath.isEmpty()) sdkCmakeBin else "$currentPath:$sdkCmakeBin")
    }

    // NDK 25 android.toolchain.cmake clears CMAKE_SHARED_LINKER_FLAGS in cache before CMake reads LDFLAGS,
    // so pass -DCMAKE_SHARED_LINKER_FLAGS explicitly to guarantee 16 KB ELF page alignment on clean builds.
    commandLine(
        "bash", "-euo", "pipefail", "-c",
        """
        rm -rf "${'$'}OUTPUT_DIR"
        mkdir -p "${'$'}OUTPUT_DIR"
        NPROC=$(nproc 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || echo 4)
        for ABI in ${'$'}ABIS; do
            BUILD_DIR="${'$'}SENTENCEPIECE_SRC/build_${'$'}ABI"
            mkdir -p "${'$'}BUILD_DIR"
            cmake -S "${'$'}SENTENCEPIECE_SRC" -B "${'$'}BUILD_DIR" \
                -DCMAKE_TOOLCHAIN_FILE="${'$'}ANDROID_NDK/build/cmake/android.toolchain.cmake" \
                -DANDROID_ABI="${'$'}ABI" \
                -DANDROID_PLATFORM=android-"${'$'}ANDROID_API" \
                -DCMAKE_BUILD_TYPE=Release \
                -DBUILD_SHARED_LIBS=ON \
                -DSPM_DISABLE_PROTOBUF=ON \
                -DCMAKE_SHARED_LINKER_FLAGS="-Wl,-z,max-page-size=16384"
            cmake --build "${'$'}BUILD_DIR" -- -j"${'$'}NPROC"
            SO_SRC="${'$'}BUILD_DIR/src/libsentencepiece.so"
            SO_DEST="${'$'}OUTPUT_DIR/${'$'}ABI/libsentencepiece.so"
            mkdir -p "${'$'}OUTPUT_DIR/${'$'}ABI"
            if [ -n "${'$'}{LLVM_STRIP:-}" ]; then
                "${'$'}LLVM_STRIP" "${'$'}SO_SRC"
            fi
            cp "${'$'}SO_SRC" "${'$'}SO_DEST"
        done
        """.trimIndent()
    )
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
