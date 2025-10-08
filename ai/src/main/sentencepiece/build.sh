#!/usr/bin/bash
set -euo pipefail

# -------------------------------
# CONFIGURATION
# -------------------------------

# NDK path (from env or local.properties)
if [[ -z "${ANDROID_NDK:-}" ]]; then
    if [[ -f "../local.properties" ]]; then
        ANDROID_NDK=$(grep "ndk.dir" ../local.properties | cut -d'=' -f2)
    else
        echo "❌ ANDROID_NDK not set and local.properties not found."
        exit 1
    fi
fi

# Source dir
SENTENCEPIECE_SRC="${SENTENCEPIECE_SRC:-$(pwd)}"

# API level
ANDROID_API="${ANDROID_API:-21}"

# Output dir
OUTPUT_DIR="${OUTPUT_DIR:-$SENTENCEPIECE_SRC/android_build_output}"

# ABIs
ABIS=(${ABIS:-"armeabi-v7a arm64-v8a x86 x86_64"})

echo "📁 Source: $SENTENCEPIECE_SRC"
echo "📦 Output: $OUTPUT_DIR"
echo "🛠️  NDK: $ANDROID_NDK"

# -------------------------------
# CLEAN OUTPUT
# -------------------------------
rm -rf "$OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"

# -------------------------------
# BUILD LOOP PER ABI
# -------------------------------
for ABI in "${ABIS[@]}"; do
    echo "🔨 Building for ABI: $ABI"

    BUILD_DIR="$SENTENCEPIECE_SRC/build_$ABI"
    mkdir -p "$BUILD_DIR"

    cmake -S "$SENTENCEPIECE_SRC" -B "$BUILD_DIR" \
        -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK/build/cmake/android.toolchain.cmake" \
        -DANDROID_ABI="$ABI" \
        -DANDROID_PLATFORM=android-"$ANDROID_API" \
        -DCMAKE_BUILD_TYPE=Release \
        -DBUILD_SHARED_LIBS=ON \
        -DSPM_DISABLE_PROTOBUF=ON

    cmake --build "$BUILD_DIR" -- -j$(nproc)

    # -------------------------------
    # STRIP .so
    # -------------------------------
    SO_SRC="$BUILD_DIR/src/libsentencepiece.so"
    SO_DEST="$OUTPUT_DIR/$ABI/libsentencepiece.so"

    mkdir -p "$OUTPUT_DIR/$ABI"

    "$ANDROID_NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip" "$SO_SRC"
    cp "$SO_SRC" "$SO_DEST"

    echo "✅ Done: $SO_DEST"
done

echo "🎉 All ABIs built and stripped. Find .so files in $OUTPUT_DIR/<ABI>/libsentencepiece.so"
