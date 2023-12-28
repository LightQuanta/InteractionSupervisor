package tech.lq0.interactionsupervisor.command

import org.bukkit.command.CommandSender

abstract class Executable(open val name: String)

data class CommandInfo(val command: Command? = null, val usage: String? = null, val depth: Int = 1)
data class ExecuteResult(val success: Boolean, val usage: String? = null)

class Commands(private val executable: List<Executable>) {
    fun process(
        sender: CommandSender,
        command: org.bukkit.command.Command,
        label: String,
        args: Array<out String>
    ): ExecuteResult {
        if (args.isEmpty()) return ExecuteResult(false)
        val commandInfo = findSubCommand(executable, args.toList())
        return when {
            commandInfo.command != null -> {
                commandInfo.command.execute?.invoke(
                    sender,
                    command,
                    label,
                    args.drop(commandInfo.depth)
                )
                ExecuteResult(true)
            }

            commandInfo.usage != null -> ExecuteResult(false, commandInfo.usage)
            else -> ExecuteResult(false)
        }
    }

    fun tabComplete(args: Array<out String>): MutableList<String> {
        val arg = args.dropLast(1)
        if (arg.isEmpty()) return executable.map { it.name }.toMutableList()

        var lastList = executable
        for (i in 0..arg.lastIndex) {
            val cmd = lastList.firstOrNull { it.name == args[i] }
            when (cmd) {
                is CommandGroup -> lastList = cmd.subCommands
                is Command -> return cmd.tabComplete()
                else -> break
            }
        }
        return mutableListOf(*lastList.map { it.name }.toTypedArray())
    }

    private tailrec fun findSubCommand(
        commands: List<Executable>,
        args: List<String>,
        lastUsage: String? = null,
        depth: Int = 0
    ): CommandInfo {
        if (args.isEmpty()) return CommandInfo(usage = lastUsage)

        val cmd = commands.firstOrNull { it.name == args[0] }
        if (cmd is Command) return CommandInfo(cmd, depth = depth + 1)
        if (cmd is CommandGroup) return findSubCommand(cmd.subCommands, args.drop(1), cmd.usage, depth + 1)
        return CommandInfo(usage = lastUsage, depth = depth + 1)
    }
}

class Command(commandName: String) : Executable(commandName) {
    var usage: String? = null
    var tabComplete: (() -> MutableList<String>) = { mutableListOf() }
    var execute: ((CommandSender, org.bukkit.command.Command, String, List<String>) -> Unit)? = null
}

class CommandGroup(groupName: String) : Executable(groupName) {
    val subCommands = mutableListOf<Executable>()
    var usage: String? = null

    fun command(subName: String, init: Command.() -> Unit) = Command(subName).apply(init).also(subCommands::add)
    fun group(subName: String, init: CommandGroup.() -> Unit) =
        CommandGroup(subName).apply(init).also(subCommands::add)
}

class CommandBuilder {
    private val executables = mutableListOf<Executable>()
    fun build() = Commands(executables)

    fun command(name: String, init: Command.() -> Unit) = Command(name).apply(init).also(executables::add)
    fun group(name: String, init: CommandGroup.() -> Unit) = CommandGroup(name).apply(init).also(executables::add)
}

fun buildCommand(init: CommandBuilder.() -> Unit) = CommandBuilder().apply(init).build()