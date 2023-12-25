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
        val sender = event.player

        // 启用聊天延迟时
        if (chatDelayEnabled) {
            GlobalScope.launch {
                val uuid = UUID.randomUUID()
                val chat = ChatMessage(sender, message)
                delayedChat[uuid] = chat
                log.info(
                    "Delayed Chat: " + chatFormat
                        .replace("%DISPLAY_NAME%", sender.displayName)
                        .replace("%NAME%", sender.name)
                        .replace("%MESSAGE%", message)
                )
                if (message.isSensitive()) {
                    log.warning("Sensitive words send by ${event.player.name} : $message")
                    sender.shadowBan()
                }

                // 如果发送者能跳过消息延迟，则立刻向发送者发送消息
                if (canBypassDelay(sender, chat)) {
                    sender.sendMessage(
                        chatFormat.replace("%DISPLAY_NAME%", sender.displayName)
                            .replace("%NAME%", sender.name)
                            .replace("%MESSAGE%", message)
                    )
                }

                // 向其余能跳过消息延迟的玩家立刻发送消息
                if (!message.isSensitive() && !sender.shadowBanned()) {
                    svr.onlinePlayers
                        .filter { it.uniqueId != sender.uniqueId }
                        .filter { canBypassDelay(it, chat) }
                        .forEach {
                            log.info("delayed chat bypassed by ${it.name} : $message")
                            it.sendMessage(
                                chatFormat
                                    .replace("%DISPLAY_NAME%", sender.displayName)
                                    .replace("%NAME%", sender.name)
                                    .replace("%MESSAGE%", message)
                            )
                        }
                }

                // 延迟后发送消息
                delay(chatDelay * 1000L)
                sendDelayedChat(uuid)
            }
            event.isCancelled = true
            return
        }

        // 未开启消息延迟情况
        // 针对shadow ban玩家发送假消息，并拦截事件
        if (sender.shadowBanned()) {
            sender.sendMessage(
                chatFormat
                    .replace("%DISPLAY_NAME%", sender.displayName)
                    .replace("%NAME%", sender.name)
                    .replace("%MESSAGE%", message)
            )
            event.isCancelled = true
            return
        }

        // 针对敏感消息，对发送者shadow ban，并拦截事件
        if (message.isSensitive()) {
            log.warning("Sensitive words send by ${event.player.name} : $message")
            sender.shadowBan()
            // shadow ban
            sender.sendMessage(
                chatFormat
                    .replace("%DISPLAY_NAME%", sender.displayName)
                    .replace("%NAME%", sender.name)
                    .replace("%MESSAGE%", message)
            )
            event.isCancelled = true
        }
    }

    // 清空延迟发送消息列表，并返回清空数量
    fun clearDelayedMessage(): Int = delayedChat.size.also { delayedChat.clear() }

    private fun canBypassDelay(player: Player, chat: ChatMessage): Boolean {
        if (player.uniqueId == chat.player.uniqueId && senderReceiveImmediately)
            return true
        return opReceiveImmediately && player.isOp
    }

    private fun sendDelayedChat(uuid: UUID) {
        val chatInfo = delayedChat[uuid] ?: return
        val sender = chatInfo.player
        val message = chatInfo.message

        // 对发送者特判，确保发送者能收到自己的延迟消息，防止因为shadow ban和敏感词导致该消息被跳过
        if (!canBypassDelay(sender, chatInfo)) {
            log.info("Delayed message to sender by ${sender.name} : $message")
            sender.sendMessage(
                chatFormat
                    .replace("%DISPLAY_NAME%", sender.displayName)
                    .replace("%NAME%", sender.name)
                    .replace("%MESSAGE%", message)
            )
        }

        // 向其余没有跳过延迟的玩家发送聊天消息
        if (sender.shadowBanned() || message.isSensitive()) return
        svr.onlinePlayers
            .filter { it.uniqueId != sender.uniqueId }
            .filter { !canBypassDelay(it, chatInfo) }
            .forEach {
                log.info("Delayed message to ${it.name} by ${sender.name} : $message")
                it.sendMessage(
                    chatFormat
                        .replace("%DISPLAY_NAME%", sender.displayName)
                        .replace("%NAME%", sender.name)
                        .replace("%MESSAGE%", message)
                )
            }
    }
}