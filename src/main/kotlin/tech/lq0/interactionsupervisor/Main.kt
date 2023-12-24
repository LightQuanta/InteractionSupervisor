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

    private fun loadConfig() {
        SensitiveWords.keywords.clear()
        SensitiveWords.regexKeywords.clear()

        config.getStringList("keywords").forEach {
            SensitiveWords.keywords.add(it as String)
        }
        config.getStringList("regex").forEach {
            SensitiveWords.regexKeywords.add(Regex(it as String))
        }
        logger.info("Loaded ${SensitiveWords.keywords.size} keyword(s) and ${SensitiveWords.regexKeywords.size} regex keyword(s)")
    }
}
