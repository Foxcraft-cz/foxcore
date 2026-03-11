package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.warp.WarpSetServerResult
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class SetWarpCommand(
    private val plugin: FoxCorePlugin,
) : TabExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val player = sender as? Player ?: run {
            sender.sendMessage(plugin.messages.text("error.player-only"))
            return true
        }
        if (!player.hasPermission("foxcore.warp.server.manage")) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }
        if (args.size != 1) {
            player.sendMessage(plugin.messages.text("command.setwarp.usage"))
            return true
        }

        return when (val result = plugin.warps.setServerWarp(player, args[0])) {
            WarpSetServerResult.InvalidName -> {
                player.sendMessage(plugin.messages.text("command.warp.invalid-name"))
                true
            }
            is WarpSetServerResult.AlreadyExists -> {
                player.sendMessage(plugin.messages.text("command.warp.already-exists", "warp" to result.name))
                true
            }
            is WarpSetServerResult.Success -> {
                val key = if (result.existed) "command.setwarp.updated" else "command.setwarp.created"
                player.sendMessage(plugin.messages.text(key, "warp" to result.warp.name))
                true
            }
        }
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> =
        emptyList()
}
