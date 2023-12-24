package tech.lq0.interactionsupervisor

fun String.isSensitive() = keywords.any { it in this } || regexKeywords.any { it.matches(this) }