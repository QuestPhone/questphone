package nethical.questphone.data

enum class EvaluationStep(val message: String, val progress: Float) {
    INITIALIZING("Initializing...", 0.1f),
    LOADING_IMAGE("Loading image...", 0.2f),
    CHECKING_MODEL("Checking model availability...", 0.3f),
    LOADING_MODEL("Loading AI model...", 0.4f),
    PREPROCESSING("Preprocessing image...", 0.6f),
    TOKENIZING("Processing text...", 0.7f),
    EVALUATING("Running AI evaluation...", 0.9f),
    COMPLETED("Evaluation completed", 1.0f)
}
