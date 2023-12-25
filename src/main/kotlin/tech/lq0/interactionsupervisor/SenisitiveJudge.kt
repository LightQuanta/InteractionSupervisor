package tech.lq0.interactionsupervisor

fun String.isSensitive(): Boolean {
    val processed = preprocess.toRegex().replace(this, "")
    return normalKeywords.any { it in processed } || regexpKeywords.any { it.matches(processed) }
}