#include <jni.h>
#include <sentencepiece_processor.h>
#include <string>
#include <vector>
#include <mutex>

static sentencepiece::SentencePieceProcessor sp;
static std::mutex sp_mutex;
static std::string loaded_model_path;

extern "C" JNIEXPORT jint JNICALL
Java_nethical_questphone_ai_SentencePieceProcessor_load(JNIEnv *env, jobject, jstring modelPath) {
    if (modelPath == nullptr) {
        return -1;
    }
    const char *path = env->GetStringUTFChars(modelPath, nullptr);
    if (path == nullptr) {
        return -1;
    }
    std::string path_str(path);
    env->ReleaseStringUTFChars(modelPath, path);

    std::lock_guard<std::mutex> lock(sp_mutex);
    if (!loaded_model_path.empty() && loaded_model_path == path_str && sp.status().ok()) {
        return 0;
    }

    auto status = sp.Load(path_str);
    if (status.ok()) {
        loaded_model_path = path_str;
        return 0;
    }
    loaded_model_path.clear();
    return -1;
}

extern "C" JNIEXPORT jintArray JNICALL
Java_nethical_questphone_ai_SentencePieceProcessor_encodeAsIds(JNIEnv *env, jobject, jstring input) {
    if (input == nullptr) {
        return env->NewIntArray(0);
    }
    const char *text = env->GetStringUTFChars(input, nullptr);
    if (text == nullptr) {
        return env->NewIntArray(0);
    }
    std::string text_str(text);
    env->ReleaseStringUTFChars(input, text);

    std::vector<int> ids;
    {
        std::lock_guard<std::mutex> lock(sp_mutex);
        if (loaded_model_path.empty() || !sp.status().ok()) {
            return env->NewIntArray(0);
        }
        sp.Encode(text_str, &ids);
    }

    jsize size = static_cast<jsize>(ids.size());
    jintArray result = env->NewIntArray(size);
    if (result != nullptr && size > 0) {
        env->SetIntArrayRegion(result, 0, size, ids.data());
    }
    return result;
}
