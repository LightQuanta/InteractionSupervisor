package tech.lq0.interactionsupervisor

import org.bukkit.Server
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import tech.lq0.interactionsupervisor.event.BookHandler
import tech.lq0.interactionsupervisor.event.ChatHandler
import tech.lq0.interactionsupervisor.event.SignHandler
import java.util.logging.Logger

lateinit var log: Logger
lateinit var svr: Server

@Suppress("unused")
class Main : JavaPlugin() {
    override fun onEnable() {
        log = logger
        svr = server

        with(server.pluginManager) {
            registerEvents(ChatHandler, this@Main)
            registerEvents(BookHandler, this@Main)
            registerEvents(SignHandler, this@Main)
        }

        loadConfig()
        logger.info("InteractionSupervisor Enabled")
    }

    override fun onDisable() {
        logger.info("InteractionSupervisor Disabled")
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) return false
        when (args[0]) {
            "reload" -> {
                loadConfig()
                sender.sendMessage("已加载${keywords.size}条关键词和${regexKeywords.size}条正则")
            }

            "test" -> {
                if (args.size < 2) return false
                sender.sendMessage(if (args[1].isSensitive()) "存在敏感词" else "不存在敏感词")
            }

            "ban" -> {
                if (args.size < 2) return false
                val player = server.onlinePlayers.firstOrNull { it.name == args[1] }
                    ?: server.offlinePlayers.firstOrNull { it.name == args[1] }
                player?.let {
                    it.ban()
                    sender.sendMessage("已封禁${it.name}")
                } ?: sender.sendMessage("未找到玩家${args[1]}！")
            }

            "unban" -> {
                if (args.size < 2) return false
                val player = server.onlinePlayers.firstOrNull { it.name == args[1] }
                    ?: server.offlinePlayers.firstOrNull { it.name == args[1] }
                player?.let {
                    it.unban()
                    sender.sendMessage("已解封${it.name}")
                } ?: sender.sendMessage("未找到玩家${args[1]}！")
            }

            "banlist" -> {
                sender.sendMessage("封禁玩家列表：${blacklist.values.joinToString()}")
            }

            else -> return false
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String>? {
        return super.onTabComplete(sender, command, alias, args)
    }

    private fun loadConfig() {
        keywords.clear()
        regexKeywords.clear()

        config.getStringList("keywords").forEach { keywords.add(it as String) }
        config.getStringList("regex").forEach { regexKeywords.add(Regex(it as String)) }
        logger.info("Loaded ${keywords.size} keyword(s) and ${regexKeywords.size} regex keyword(s)")
    }
}
