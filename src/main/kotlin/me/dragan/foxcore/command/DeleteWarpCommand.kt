package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.warp.WarpDeleteResult
import me.dragan.foxcore.warp.WarpScope
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor

class DeleteWarpCommand(
    private val plugin: FoxCorePlugin,
) : TabExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("foxcore.warp.server.manage")) {
            sender.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }
        if (args.size != 1) {
            sender.sendMessage(plugin.messages.text("command.delwarp.usage"))
            return true
        }

        val warp = plugin.warps.getWarp(args[0]) ?: run {
            sender.sendMessage(plugin.messages.text("command.warp.not-found", "warp" to args[0]))
            return true
        }
        if (warp.scope != WarpScope.SERVER) {
            sender.sendMessage(plugin.messages.text("command.delwarp.not-server", "warp" to warp.name))
            return true
        }

        return when (plugin.warps.deleteWarp(warp.name)) {
            is WarpDeleteResult.NotFound -> {
                sender.sendMessage(plugin.messages.text("command.warp.not-found", "warp" to args[0]))
                true
            }
            is WarpDeleteResult.Success -> {
                sender.sendMessage(plugin.messages.text("command.delwarp.success", "warp" to warp.name))
                true
            }
        }
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> =
        if (args.size == 1) {
            plugin.warps.listWarps()
                .filter { it.scope == WarpScope.SERVER }
                .map { it.name }
                .filter { it.startsWith(args[0], ignoreCase = true) }
        } else {
            emptyList()
        }
}
