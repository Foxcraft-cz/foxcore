package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class WorldWeatherShortcutCommand(
    private val plugin: FoxCorePlugin,
    private val permission: String,
    private val commandKey: String,
    private val storming: Boolean,
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

        val world = player.world
        world.setStorm(storming)
        world.isThundering = false
        if (!storming) {
            world.weatherDuration = 6000
            world.thunderDuration = 0
        }

        player.sendMessage(plugin.messages.text("command.$commandKey.success", "world" to world.name))
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> = emptyList()
}
