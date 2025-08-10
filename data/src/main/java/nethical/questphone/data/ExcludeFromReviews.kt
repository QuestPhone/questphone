package nethical.questphone.data

// Define a custom annotation for excluding fields from review
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY)
annotation class ExcludeFromReviewDialog
