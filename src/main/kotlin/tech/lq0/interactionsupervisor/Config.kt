package tech.lq0.interactionsupervisor

import java.util.*

data class PlayerUUID(val name: String, val uuid: UUID)

var chatDelayEnabled = false
var chatDelay = 5
var chatFormat = "§r<%DISPLAY_NAME%>§r %MESSAGE%"
var senderReceiveImmediately = true
var opReceiveImmediately = true
var chatDelayWhitelistEnabled = true
val chatDelayWhitelist = mutableSetOf<PlayerUUID>()

var preprocess = ""
val normalKeywords = mutableListOf<String>()
val regexpKeywords = mutableListOf<Regex>()