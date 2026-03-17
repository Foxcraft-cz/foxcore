package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class WorldTimeShortcutCommand(
    private val plugin: FoxCorePlugin,
    private val permission: String,
    private val commandKey: String,
    private val time: Long,
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
            player.sendMessage(plugin.messages.text("command.$commandKey.usage"))
            return true
        }

        player.world.time = time
        player.sendMessage(plugin.messages.text("command.$commandKey.success", "world" to player.world.name))
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> = emptyList()
}
