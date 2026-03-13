package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor

class BroadcastCommand(
    private val plugin: FoxCorePlugin,
) : TabExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage(plugin.messages.text("command.broadcast.usage"))
            return true
        }

        plugin.broadcasts.broadcastRaw(listOf(args.joinToString(" ")))
        sender.sendMessage(plugin.messages.text("command.broadcast.success"))
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> = emptyList()

    companion object {
        private const val PERMISSION = "foxcore.broadcast"
    }
}
