package tech.lq0.interactionsupervisor.event

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import tech.lq0.interactionsupervisor.ban
import tech.lq0.interactionsupervisor.banned
import tech.lq0.interactionsupervisor.isSensitive
import tech.lq0.interactionsupervisor.log

object ChatHandler : Listener {
    @EventHandler
    fun onChat(event: AsyncPlayerChatEvent) {
        val message = event.message
        val player = event.player

        if (player.banned()) {
            event.isCancelled = true
            player.sendMessage("<${player.displayName}> $message")
            return
        }

        if (message.isSensitive()) {
            log.warning("Sensitive words send by ${event.player.name} : $message")
            player.ban()
            // shadow ban
            player.sendMessage("<${player.displayName}> $message")
            event.isCancelled = true
        }
    }
}