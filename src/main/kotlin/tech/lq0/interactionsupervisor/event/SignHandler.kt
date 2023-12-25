package tech.lq0.interactionsupervisor.event

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.SignChangeEvent
import tech.lq0.interactionsupervisor.shadowBanned
import tech.lq0.interactionsupervisor.isSensitive
import tech.lq0.interactionsupervisor.log

object SignHandler : Listener {
    @EventHandler
    fun editSign(event: SignChangeEvent) {
        val player = event.player
        if (player.shadowBanned()) {
            event.isCancelled = true
            return
        }
        val lines = event.lines
        if (lines.any { it.isSensitive() }) {
            event.isCancelled = true
            log.warning("Sensitive sign edited by ${player.name}\n-----\n${lines.joinToString("\n")}\n-----")
        }
    }
}