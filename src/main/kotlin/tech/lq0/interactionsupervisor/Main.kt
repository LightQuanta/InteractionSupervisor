package tech.lq0.interactionsupervisor

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin

@Suppress("unused")
class Main : JavaPlugin() {
    override fun onEnable() {
        with(server.pluginManager) {
            registerEvents(ChatHandler, this@Main)
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
                sender.sendMessage("已重新加载${keywords.size}条关键词和${regexKeywords.size}条正则")
            }
        }

        return true
    }

    private fun loadConfig() {
        keywords.clear()
        regexKeywords.clear()

        config.getStringList("keywords").forEach {
            keywords.add(it as String)
        }
        config.getStringList("regex").forEach {
            regexKeywords.add(Regex(it as String))
        }
        logger.info("Loaded ${keywords.size} keyword(s) and ${regexKeywords.size} regex keyword(s)")
    }
}
