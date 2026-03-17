package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class InventoryOpenerCommand(
    private val plugin: FoxCorePlugin,
    private val permission: String,
    private val messageKey: String,
    private val opener: (Player) -> Unit,
) : TabExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        val player = sender as? Player ?: run {
            sender.sendMessage(plugin.messages.text("error.player-only"))
            return true
        }

        if (!player.hasPermission(permission)) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        if (args.isNotEmpty()) {
            player.sendMessage(plugin.messages.text("$messageKey.usage"))
            return true
        }

        opener(player)
        player.sendMessage(plugin.messages.text("$messageKey.success"))
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> = emptyList()
}
