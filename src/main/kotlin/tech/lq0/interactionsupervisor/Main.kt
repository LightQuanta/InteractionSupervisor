package tech.lq0.interactionsupervisor

import com.google.common.base.Charsets
import org.bukkit.Server
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import tech.lq0.interactionsupervisor.event.BookHandler
import tech.lq0.interactionsupervisor.event.ChatHandler
import tech.lq0.interactionsupervisor.event.SignHandler
import java.io.File
import java.io.InputStreamReader
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
                sender.sendMessage("已加载${normalKeywords.size}条关键词和${regexpKeywords.size}条正则".withPluginPrefix())
            }

            "test" -> {
                if (args.size < 2) return false
                sender.sendMessage(
                    (if (args.drop(1).joinToString(" ").isSensitive()) {
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
                    it.shadowBan()
                    sender.sendMessage("已封禁${it.name}".withPluginPrefix())
                } ?: sender.sendMessage("未找到玩家${args[1]}！".withPluginPrefix())
            }

            "unban" -> {
                if (args.size < 2) return false
                val player = server.onlinePlayers.firstOrNull { it.name == args[1] }
                    ?: server.offlinePlayers.firstOrNull { it.name == args[1] }
                player?.let {
                    it.unShadowBan()
                    sender.sendMessage("已解封${it.name}".withPluginPrefix())
                } ?: sender.sendMessage("未找到玩家${args[1]}！".withPluginPrefix())
            }

            "banlist" -> sender.sendMessage("封禁玩家列表：${blacklist.values.joinToString()}".withPluginPrefix())

            "clear" -> {
                if (chatDelayEnabled) {
                    sender.sendMessage("已清空${ChatHandler.clearDelayedMessage()}条未发送消息".withPluginPrefix())
                } else {
                    sender.sendMessage("未启用消息延迟！".withPluginPrefix())
                }
            }

            "delay" -> {
                with(loadOrCreateConfig("config.yml")) {
                    when (args[1]) {
                        "info" -> sender.sendMessage((if (chatDelayEnabled) "消息延迟已启用" else "消息延迟未启用").withPluginPrefix() + "，当前延迟为${chatDelay}秒")
                        "enable" -> {
                            chatDelayEnabled = true
                            set("chat-delay-enabled", true)
                            save(File(dataFolder, "config.yml"))
                            sender.sendMessage("已启用消息延迟".withPluginPrefix())
                        }

                        "disable" -> {
                            chatDelayEnabled = false
                            set("chat-delay-enabled", false)
                            save(File(dataFolder, "config.yml"))
                            sender.sendMessage("已禁用消息延迟".withPluginPrefix())
                        }

                        "set" -> {
                            chatDelay = args.getOrNull(2)?.toIntOrNull()?.coerceIn(1..300) ?: return false
                            set("chat-delay", chatDelay)
                            save(File(dataFolder, "config.yml"))
                            log.info("Set chat delay to $chatDelay second(s)")
                            sender.sendMessage("已将消息延迟设置为${chatDelay}秒".withPluginPrefix())
                        }

                        else -> return false
                    }
                }
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
        if (args.size == 1) return mutableListOf(
            "reload",
            "test",
            "ban",
            "unban",
            "banlist",
            "clear",
            "delay"
        )
        if (args.size == 2) {
            return when (args[0]) {
                "ban" -> server.onlinePlayers.map { it.name }.filter { it !in blacklist.values }.toMutableList()
                "unban" -> blacklist.values.toMutableList()
                "delay" -> mutableListOf("info", "enable", "disable", "set")
                else -> null
            }
        }
        return null
    }

    private fun loadConfig() {
        normalKeywords.clear()
        regexpKeywords.clear()

        dataFolder.mkdirs()
        with(loadOrCreateConfig("config.yml")) {
            chatDelayEnabled = getBoolean("chat-delay-enabled")
            chatDelay = getInt("chat-delay").coerceIn(1..300)
            chatFormat = getString("chat-format") ?: ""
        }

        with(loadOrCreateConfig("sensitive.yml")) {
            getStringList("keywords").forEach { normalKeywords.add(it as String) }
            getStringList("regex").forEach { regexpKeywords.add(Regex(it as String)) }
            preprocess = getString("preprocess") ?: ""
        }

        logger.info("Loaded ${normalKeywords.size} keyword(s) and ${regexpKeywords.size} regex keyword(s)")
    }

    private fun loadOrCreateConfig(name: String): YamlConfiguration {
        val f = File(dataFolder, name)
        return if (f.exists()) {
            val newConfig = YamlConfiguration.loadConfiguration(f)
            getResource("config/$name")?.let {
                newConfig.setDefaults(YamlConfiguration.loadConfiguration(InputStreamReader(it, Charsets.UTF_8)))
            }
            newConfig
        } else {
            getResource("config/$name")?.let {
                YamlConfiguration.loadConfiguration(InputStreamReader(it, Charsets.UTF_8))
                    .also { conf -> conf.save(f) }
            } ?: throw Exception("config file $name not found")
        }
    }
}
