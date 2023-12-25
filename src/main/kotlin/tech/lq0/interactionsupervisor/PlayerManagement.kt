package tech.lq0.interactionsupervisor

import org.bukkit.OfflinePlayer
import org.bukkit.Sound
import java.util.*

val blacklist = mutableMapOf<UUID, String>()

fun OfflinePlayer.shadowBan() = blacklist.set(uniqueId, name!!).also {
    log.info("Banned $name")
    svr.onlinePlayers.filter { it.isOp }.forEach {
        it.playSound(it.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f)
        it.sendMessage("§c§l$name 尝试发送敏感信息，已被屏蔽，请查看服务器控制台决定下一步操作".withPluginPrefix())
    }
}

fun OfflinePlayer.unShadowBan() = blacklist.remove(uniqueId)
fun OfflinePlayer.shadowBanned() = blacklist.contains(uniqueId)