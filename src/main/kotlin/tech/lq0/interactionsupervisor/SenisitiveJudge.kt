package tech.lq0.interactionsupervisor

fun String.isSensitive(): Boolean {
    val processed = preprocess.toRegex().replace(this, "")
    return keywords.any { it in processed } || regexKeywords.any { it.matches(processed) }
}