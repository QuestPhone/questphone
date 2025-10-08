cmake_minimum_required(VERSION 3.18.1)
project("sentencepiece_jni")

# Use prebuilt shared library
add_library(sentencepiece SHARED IMPORTED)
set_target_properties(sentencepiece PROPERTIES
        IMPORTED_LOCATION "${CMAKE_SOURCE_DIR}/../sentencepiece/build_${ANDROID_ABI}/src/libsentencepiece.so"
)

# Build JNI wrapper
add_library(sentencepiece_jni SHARED sentencepiece_jni.cpp)

# Add include path
target_include_directories(sentencepiece_jni PRIVATE
        "${CMAKE_SOURCE_DIR}/../sentencepiece/include"
)

# Android log lib
find_library(log-lib log)

# Link JNI -> prebuilt .so
target_link_libraries(sentencepiece_jni
        sentencepiece
        ${log-lib}
)
