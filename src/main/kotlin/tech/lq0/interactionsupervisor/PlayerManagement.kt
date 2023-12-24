package tech.lq0.interactionsupervisor

import org.bukkit.OfflinePlayer
import java.util.*

val blacklist = mutableMapOf<UUID, String>()

fun OfflinePlayer.ban() = blacklist.set(uniqueId, name!!)
fun OfflinePlayer.unban() = blacklist.remove(uniqueId)
fun OfflinePlayer.banned() = blacklist.contains(uniqueId)