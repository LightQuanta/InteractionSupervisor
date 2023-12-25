package tech.lq0.interactionsupervisor.event

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerEditBookEvent
import tech.lq0.interactionsupervisor.shadowBanned
import tech.lq0.interactionsupervisor.isSensitive
import tech.lq0.interactionsupervisor.log

object BookHandler : Listener {
    @EventHandler
    fun editBook(event: PlayerEditBookEvent) {
        val player = event.player
        if (player.shadowBanned()) {
            event.isCancelled = true
            return
        }
        val meta = event.newBookMeta
        if (meta.pages.any { it.isSensitive() } || meta.author?.isSensitive() == true) {
            // TODO 修复书本编辑无法拦截的bug
            event.newBookMeta = event.previousBookMeta
            event.isCancelled = true
            log.warning("Sensitive book edited by ${player.name}\n-----\n${meta.pages.joinToString("\n")}\n-----")
        }
    }
}