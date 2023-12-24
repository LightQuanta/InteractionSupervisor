package tech.lq0.interactionsupervisor

import org.bukkit.Server
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import tech.lq0.interactionsupervisor.event.BookHandler
import tech.lq0.interactionsupervisor.event.ChatHandler
import tech.lq0.interactionsupervisor.event.SignHandler
import java.io.File
import java.util.logging.Logger

lateinit var log: Logger
lateinit var svr: Server

fun String.withPluginPrefix() = "§7[§bI§4S§7]§r $this"

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

    override fun onDisable() = logger.info("InteractionSupervisor Disabled")

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) return false
        when (args[0]) {
            "reload" -> {
                loadConfig()
                sender.sendMessage("已加载${keywords.size}条关键词和${regexKeywords.size}条正则".withPluginPrefix())
            }

            "test" -> {
                if (args.size < 2) return false
                sender.sendMessage(
                    (if (args.drop(2).joinToString(" ").isSensitive()) {
                        "内容存在敏感词"
                    } else {
                        "内容不存在敏感词"
                    }).withPluginPrefix()
                )
            }

            "ban" -> {
                if (args.size < 2) return false
                val player = server.onlinePlayers.firstOrNull { it.name == args[1] }
                    ?: server.offlinePlayers.firstOrNull { it.name == args[1] }
                player?.let {
                    it.ban()
                    sender.sendMessage("已封禁${it.name}".withPluginPrefix())
                } ?: sender.sendMessage("未找到玩家${args[1]}！".withPluginPrefix())
            }

            "unban" -> {
                if (args.size < 2) return false
                val player = server.onlinePlayers.firstOrNull { it.name == args[1] }
                    ?: server.offlinePlayers.firstOrNull { it.name == args[1] }
                player?.let {
                    it.unban()
                    sender.sendMessage("已解封${it.name}".withPluginPrefix())
                } ?: sender.sendMessage("未找到玩家${args[1]}！".withPluginPrefix())
            }

            "banlist" -> sender.sendMessage("封禁玩家列表：${blacklist.values.joinToString()}".withPluginPrefix())
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
        if (args.size == 1) return mutableListOf("reload", "test", "ban", "unban", "banlist")
        return when (args[1]) {
            "ban" -> server.onlinePlayers.map { it.name }.filter { it !in blacklist.values }.toMutableList()
            "unban" -> blacklist.values.toMutableList()
            else -> null
        }
    }

    private fun loadConfig() {
        keywords.clear()
        regexKeywords.clear()

        dataFolder.mkdirs()
        val file = File(dataFolder, "config.yml")
        if (file.exists()) {
            config.load(file)
        } else {
            config.save(file)
        }

        config.getStringList("keywords").forEach { keywords.add(it as String) }
        config.getStringList("regex").forEach { regexKeywords.add(Regex(it as String)) }
        logger.info("Loaded ${keywords.size} keyword(s) and ${regexKeywords.size} regex keyword(s)")
    }
}
