package tech.lq0.interactionsupervisor.command

import org.bukkit.command.CommandSender

data class CommandInfo(val command: Command? = null, val usage: String? = null, val depth: Int = 1)
data class ExecuteResult(val success: Boolean, val usage: String? = null)

class CommandExecutor(private val executable: List<Command>) {
    fun process(
        sender: CommandSender,
        command: org.bukkit.command.Command,
        label: String,
        args: Array<out String>
    ): ExecuteResult {
        if (args.isEmpty()) return ExecuteResult(false)
        val commandInfo = findSubCommand(executable, args.toList())
        return when {
            // 执行成功
            commandInfo.command != null -> {
                commandInfo.command.execute?.invoke(
                    CommandExecuteParams(
                        sender,
                        command,
                        label,
                        args.drop(commandInfo.depth)
                    )
                )
                ExecuteResult(true)
            }

            // 执行失败，但是有上一级命令的帮助
            commandInfo.usage != null -> ExecuteResult(false, commandInfo.usage)

            // 执行失败，使用顶层命令帮助
            else -> ExecuteResult(false)
        }
    }

    fun tabComplete(args: Array<out String>): MutableList<String> {
        val arg = args.dropLast(1)
        if (arg.isEmpty()) return executable.map { it.name }.toMutableList()

        var lastList = executable
        for (i in 0..arg.lastIndex) {
            val cmd = lastList.firstOrNull { it.name == args[i] } ?: break
            if (cmd.subCommands.isEmpty()) return cmd.tabComplete()  // 遇到命令则返回该命令的补全
            lastList = cmd.subCommands  // 遇到命令组则继续查找
        }
        return mutableListOf(*lastList.map { it.name }.toTypedArray())
    }

    private tailrec fun findSubCommand(
        commands: List<Command>,
        args: List<String>,
        lastUsage: String? = null,  // 上一条有效帮助信息
        depth: Int = 0
    ): CommandInfo {
        if (args.isEmpty()) return CommandInfo(usage = lastUsage)

        // 当前参数同名命令（组）
        val cmd =
            commands.firstOrNull { it.name == args[0] } ?: return CommandInfo(usage = lastUsage, depth = depth + 1)

        if (cmd.subCommands.isEmpty()) return CommandInfo(cmd, depth = depth + 1)
        return findSubCommand(cmd.subCommands, args.drop(1), cmd.usage, depth + 1)
    }
}

data class CommandExecuteParams(
    val sender: CommandSender,
    val command: org.bukkit.command.Command,
    val label: String,
    val args: List<String>
)

class Command(val name: String) {
    val subCommands = mutableListOf<Command>()
    var usage: String? = null
    var tabComplete: (() -> MutableList<String>) = { mutableListOf() }
    var execute: ((CommandExecuteParams) -> Unit)? = null

    fun tabComplete(init: () -> MutableList<String>) {
        tabComplete = init
    }

    fun execute(init: CommandExecuteParams.() -> Unit) {
        execute = init
    }

    fun usage(init: String) {
        usage = init
    }

    operator fun String.invoke(init: Command.() -> Unit) = Command(this).apply(init).also(subCommands::add)
}

class CommandBuilder {
    private val commands = mutableListOf<Command>()
    operator fun String.invoke(init: Command.() -> Unit) = Command(this).apply(init).also(commands::add)
    fun build() = CommandExecutor(commands)
}

fun buildCommand(init: CommandBuilder.() -> Unit) = CommandBuilder().apply(init).build()