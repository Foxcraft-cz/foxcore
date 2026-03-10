package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.teleport.SafeTeleportResult
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class TeleportHereCommand(
    private val plugin: FoxCorePlugin,
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

        if (!player.hasPermission("foxcore.tphere")) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        if (args.size != 1) {
            player.sendMessage(plugin.messages.text("command.tphere.usage"))
            return true
        }

        val target = findOnlinePlayer(args[0])
        if (target == null) {
            player.sendMessage(plugin.messages.text("command.tphere.not-found", "player" to args[0]))
            return true
        }

        if (target.uniqueId == player.uniqueId) {
            player.sendMessage(plugin.messages.text("command.tphere.self"))
            return true
        }

        when (plugin.safeTeleports.teleport(target, player.location)) {
            SafeTeleportResult.SUCCESS -> Unit
            SafeTeleportResult.NO_SAFE_GROUND -> {
                player.sendMessage(plugin.messages.text("error.no-safe-ground-other", "player" to target.name))
                return true
            }

            SafeTeleportResult.FAILED -> {
                player.sendMessage(plugin.messages.text("error.teleport-failed-other", "player" to target.name))
                return true
            }
        }

        player.sendMessage(plugin.messages.text("command.tphere.success", "player" to target.name))

        if (plugin.config.getBoolean("teleport.notify-target", true)) {
            target.sendMessage(plugin.messages.text("command.tphere.notify-target", "player" to player.name))
        }

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> {
        if (args.size != 1) {
            return emptyList()
        }

        return Bukkit.getOnlinePlayers()
            .asSequence()
            .map(Player::getName)
            .filter { it.startsWith(args[0], ignoreCase = true) }
            .sorted()
            .toList()
    }

    private fun findOnlinePlayer(input: String): Player? =
        Bukkit.getPlayerExact(input)
            ?: Bukkit.getOnlinePlayers().firstOrNull { it.name.equals(input, ignoreCase = true) }
}
