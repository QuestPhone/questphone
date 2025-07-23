cmake_minimum_required(VERSION 3.18.1)
project("sentencepiece_jni")

add_library(sentencepiece SHARED IMPORTED)
set_target_properties(sentencepiece PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jniLibs/arm64-v8a/libsentencepiece.so)


add_library(sentencepiece_jni SHARED sentencepiece_jni.cpp)

find_library(log-lib log)

target_include_directories(sentencepiece_jni PRIVATE
        ${CMAKE_SOURCE_DIR}/../sentencepiece/include/sentencepiece)

target_link_libraries(sentencepiece_jni sentencepiece ${log-lib})