package tech.lq0.interactionsupervisor

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent

object ChatHandler : Listener {
    @EventHandler
    fun onChat(event: AsyncPlayerChatEvent) {
        val message = event.message
        val player = event.player
        if (message.isSensitive()) {
            log.warning("Sensitive words send by ${event.player.name} : $message")

            // shadow ban
            player.sendMessage("<${player.displayName}> $message")
            event.isCancelled = true
        }
    }
}