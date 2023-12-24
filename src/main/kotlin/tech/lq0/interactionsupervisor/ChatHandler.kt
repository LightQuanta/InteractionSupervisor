package tech.lq0.interactionsupervisor

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent

object ChatHandler : Listener {
    @EventHandler
    fun onChat(event: AsyncPlayerChatEvent) {
        val message = event.message
        if (message.isSensitive()) {
            event.player.sendMessage("敏感词汇")
            event.isCancelled = true
        }
    }
}