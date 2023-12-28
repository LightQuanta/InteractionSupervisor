package tech.lq0.interactionsupervisor

import com.google.common.base.Charsets
import org.bukkit.Server
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import tech.lq0.interactionsupervisor.command.buildCommand
import tech.lq0.interactionsupervisor.event.BookHandler
import tech.lq0.interactionsupervisor.event.ChatHandler
import tech.lq0.interactionsupervisor.event.SignHandler
import java.io.File
import java.io.InputStreamReader
import java.util.*
import java.util.logging.Logger

lateinit var log: Logger
lateinit var svr: Server

fun String.withPluginPrefix() = "§7[§bI§4S§7]§r $this"
fun String.withFullPluginPrefix() = "§7[§bInteraction§4Supervisor§7]§r\n$this"

@Suppress("unused")
class Main : JavaPlugin() {

    private val commands = buildCommand {
        command("reload") {
            execute = { sender, _, _, _ ->
                loadConfig()
                sender.sendMessage("已加载${normalKeywords.size}条关键词和${regexpKeywords.size}条正则".withPluginPrefix())
            }
        }

        command("test") {
            execute = { sender, _, _, args ->
                if (args.size < 2) {
                    sender.sendMessage("使用方法: /is test <关键词>".withFullPluginPrefix())
                } else {
                    sender.sendMessage(
                        (if (args.drop(1).joinToString(" ").isSensitive()) {
                            "内容存在敏感词"
                        } else {
                            "内容不存在敏感词"
                        }).withPluginPrefix()
                    )
                }
            }
        }

        command("ban") {
            execute = { sender, _, _, args ->
                if (args.isEmpty()) {
                    sender.sendMessage("用法: /is ban <玩家名>".withPluginPrefix())
                } else {
                    val player = server.onlinePlayers.firstOrNull { it.name == args[0] }
                        ?: server.offlinePlayers.firstOrNull { it.name == args[0] }
                    player?.let {
                        it.shadowBan()
                        sender.sendMessage("已封禁${it.name}".withPluginPrefix())
                    } ?: sender.sendMessage("未找到玩家${args[1]}！".withPluginPrefix())
                }
            }
            tabComplete = { server.onlinePlayers.map { it.name }.filter { it !in blacklist.values }.toMutableList() }
        }

        command("unban") {
            execute = { sender, _, _, args ->
                if (args.isEmpty()) {
                    sender.sendMessage("用法: /is unban <玩家名>".withPluginPrefix())
                } else {
                    val player = server.onlinePlayers.firstOrNull { it.name == args[0] }
                        ?: server.offlinePlayers.firstOrNull { it.name == args[0] }
                    player?.let {
                        it.unShadowBan()
                        sender.sendMessage("已解封${it.name}".withPluginPrefix())
                    } ?: sender.sendMessage("未找到玩家${args[1]}！".withPluginPrefix())
                }
            }
            tabComplete = blacklist.values::toMutableList
        }

        command("banlist") {
            execute = { sender, _, _, _ ->
                sender.sendMessage("封禁玩家列表：${blacklist.values.joinToString()}".withPluginPrefix())
            }
        }

        command("clear") {
            execute = { sender, _, _, _ ->
                if (chatDelayEnabled) {
                    sender.sendMessage("已清空${ChatHandler.clearDelayedMessage()}条未发送消息".withPluginPrefix())
                } else {
                    sender.sendMessage("未启用消息延迟！".withPluginPrefix())
                }
            }
        }

        group("delay") {
            usage = "用法：/is delay [enable|disable|status|set]".trimIndent().withFullPluginPrefix()
            command("enable") {
                execute = { sender, _, _, _ ->
                    chatDelayEnabled = true
                    sender.sendMessage("已启用消息延迟".withPluginPrefix())
                }
            }

            command("disable") {
                execute = { sender, _, _, _ ->
                    chatDelayEnabled = false
                    sender.sendMessage("已禁用消息延迟".withPluginPrefix())
                }
            }

            command("status") {
                execute = { sender, _, _, _ ->
                    sender.sendMessage((if (chatDelayEnabled) "消息延迟已启用" else "消息延迟未启用").withPluginPrefix() + "，当前延迟为${chatDelay}秒")
                }
            }

            command("set") {
                execute = { sender, _, _, args ->
                    val delay = args.getOrNull(0)?.toIntOrNull()?.coerceIn(1..300)
                    if (delay != null) {
                        chatDelay = delay
                        log.info("Set chat delay to $chatDelay second(s)")
                        sender.sendMessage("已将消息延迟设置为${chatDelay}秒".withPluginPrefix())
                    } else {
                        sender.sendMessage("消息延迟范围应设置为1-300秒！".withPluginPrefix())
                    }
                }
                tabComplete = { mutableListOf("5", "10", "20", "30", "60") }
            }
        }
    }

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
        // TODO 修改为正确的权限判断
        if (!sender.isOp) return false

        val er = commands.process(sender, command, label, args)
        when {
            er.success -> return true
            er.usage != null -> {
                sender.sendMessage(er.usage)
                return true
            }

            else -> return false
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> = commands.tabComplete(args)

    private fun loadConfig() {
        normalKeywords.clear()
        regexpKeywords.clear()
        chatDelayWhitelist.clear()

        dataFolder.mkdirs()
        with(loadOrCreateConfig("config.yml")) {
            chatDelayEnabled = getBoolean("chat-delay-enabled")
            chatDelay = getInt("chat-delay").coerceIn(1..300)
            chatFormat = getString("chat-format") ?: ""
            senderReceiveImmediately = getBoolean("sender-receive-immediately")
            opReceiveImmediately = getBoolean("op-receive-immediately")
            chatDelayWhitelistEnabled = getBoolean("chat-delay-whitelist-enabled")
            getStringList("chat-delay-whitelist").map { it.split(":") }
                .forEach { (a, b) -> chatDelayWhitelist.add(PlayerUUID(a, UUID.fromString(b))) }
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
