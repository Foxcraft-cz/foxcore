package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class FoxCoreCommand(
    private val plugin: FoxCorePlugin,
) : TabExecutor {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage(plugin.messages.text("command.foxcore.usage"))
            return true
        }

        return when (args[0].lowercase()) {
            "reload" -> handleReload(sender)
            else -> {
                sender.sendMessage(plugin.messages.text("command.foxcore.usage"))
                true
            }
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> {
        if (args.size == 1 && hasReloadAccess(sender)) {
            return listOf("reload").filter { it.startsWith(args[0], ignoreCase = true) }
        }

        return emptyList()
    }

    private fun handleReload(sender: CommandSender): Boolean {
        if (!hasReloadAccess(sender)) {
            sender.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        plugin.reloadPlugin()
        sender.sendMessage(plugin.messages.text("command.foxcore.reload-success"))
        return true
    }

    private fun hasReloadAccess(sender: CommandSender): Boolean =
        sender !is Player || sender.isOp || sender.hasPermission("foxcore.reload")
}
