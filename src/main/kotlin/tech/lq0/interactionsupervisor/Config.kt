package tech.lq0.interactionsupervisor

var chatDelayEnabled = false
var chatDelay = 5
var chatFormat = "§r<%DISPLAY_NAME%>§r %MESSAGE%"
var senderReceiveImmediately = false

var preprocess = ""
val normalKeywords = mutableListOf<String>()
val regexpKeywords = mutableListOf<Regex>()