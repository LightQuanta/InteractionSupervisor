package tech.lq0.interactionsupervisor.event

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import tech.lq0.interactionsupervisor.*
import java.util.*

data class ChatMessage(val player: Player, val message: String)
object ChatHandler : Listener {
    private val delayedChat = mutableMapOf<UUID, ChatMessage>()

    @EventHandler
    fun onChat(event: AsyncPlayerChatEvent) {
        val message = event.message
        val player = event.player

        if (chatDelayEnabled) {
            GlobalScope.launch {
                val uuid = UUID.randomUUID()
                val chat = ChatMessage(player, message)
                delayedChat[uuid] = chat
                log.info(
                    "Delayed Chat: " + chatFormat
                        .replace("%DISPLAY_NAME%", player.displayName)
                        .replace("%NAME%", player.name)
                        .replace("%MESSAGE%", message)
                )
                delay(chatDelay * 1000L)
                sendChatMessage(uuid)
            }
            event.isCancelled = true
            return
        }

        if (player.shadowBanned()) {
            event.isCancelled = true
            player.sendMessage(
                chatFormat
                    .replace("%DISPLAY_NAME%", player.displayName)
                    .replace("%NAME%", player.name)
                    .replace("%MESSAGE%", message)
            )
            return
        }

        if (message.isSensitive()) {
            log.warning("Sensitive words send by ${event.player.name} : $message")
            player.shadowBan()
            // shadow ban
            player.sendMessage("<${player.displayName}> $message")
            event.isCancelled = true
        }
    }

    fun clearDelayedMessage(): Int = delayedChat.size.also { delayedChat.clear() }

    private fun sendChatMessage(uuid: UUID) {
        if (delayedChat.contains(uuid)) {
            val chatInfo = delayedChat[uuid]!!
            val player = chatInfo.player
            val message = chatInfo.message
            if (player.shadowBanned()) {
                player.sendMessage(
                    chatFormat
                        .replace("%DISPLAY_NAME%", player.displayName)
                        .replace("%NAME%", player.name)
                        .replace("%MESSAGE%", message)
                )
            } else {
                svr.onlinePlayers.forEach {
                    it.sendMessage(
                        chatFormat
                            .replace("%DISPLAY_NAME%", player.displayName)
                            .replace("%NAME%", player.name)
                            .replace("%MESSAGE%", message)
                    )
                }
            }
        }
    }
}