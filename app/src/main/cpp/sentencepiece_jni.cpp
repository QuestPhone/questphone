#include <jni.h>
#include <sentencepiece_processor.h>
#include <string>

sentencepiece::SentencePieceProcessor sp;

extern "C" JNIEXPORT jint JNICALL
Java_com_example_siglip_SentencePieceProcessor_load(JNIEnv *env, jobject, jstring modelPath) {
    const char *path = env->GetStringUTFChars(modelPath, nullptr);
    auto status = sp.Load(path);
    env->ReleaseStringUTFChars(modelPath, path);
    return status.ok() ? 0 : -1;
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_example_siglip_SentencePieceProcessor_encodeAsIds(JNIEnv *env, jobject, jstring input) {
    const char *text = env->GetStringUTFChars(input, nullptr);
    std::vector<int> ids;
    sp.Encode(text, &ids);
    env->ReleaseStringUTFChars(input, text);

    jintArray result = env->NewIntArray(ids.size());
    env->SetIntArrayRegion(result, 0, ids.size(), ids.data());
    return result;
}
