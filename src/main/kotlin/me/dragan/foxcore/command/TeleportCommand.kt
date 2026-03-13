package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.back.OfflineLocationLookup
import me.dragan.foxcore.teleport.SafeTeleportResult
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class TeleportCommand(
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

        if (!player.hasPermission("foxcore.tp")) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        if (args.size != 1) {
            player.sendMessage(plugin.messages.text("command.tp.usage"))
            return true
        }

        if (args[0].equals(player.name, ignoreCase = true)) {
            player.sendMessage(plugin.messages.text("command.tp.self"))
            return true
        }

        val target = findOnlinePlayer(args[0])
        if (target != null) {
            return teleportToOnlineTarget(player, target)
        }

        if (!player.hasPermission("foxcore.tp.offline")) {
            player.sendMessage(plugin.messages.text("command.tp.not-found", "player" to args[0]))
            return true
        }

        player.sendMessage(plugin.messages.text("command.tp.offline-searching", "player" to args[0]))
        plugin.backService.findOfflineLastLocationByName(args[0]) { result ->
            if (!player.isOnline) {
                return@findOfflineLastLocationByName
            }

            when (result) {
                is OfflineLocationLookup.NotFound -> {
                    player.sendMessage(plugin.messages.text("command.tp.not-found", "player" to args[0]))
                }

                is OfflineLocationLookup.NoStoredLocation -> {
                    player.sendMessage(plugin.messages.text("command.tp.offline-no-location", "player" to result.playerName))
                }

                is OfflineLocationLookup.MissingWorld -> {
                    player.sendMessage(
                        plugin.messages.text(
                            "command.tp.offline-missing-world",
                            "player" to result.playerName,
                            "world" to result.worldName,
                        ),
                    )
                }

                is OfflineLocationLookup.Success -> {
                    when (plugin.safeTeleports.teleport(player, result.location)) {
                        SafeTeleportResult.SUCCESS -> {
                            player.sendMessage(plugin.messages.text("command.tp.offline-success", "player" to result.playerName))
                        }

                        SafeTeleportResult.NO_SAFE_GROUND -> {
                            player.sendMessage(plugin.messages.text("error.no-safe-ground"))
                        }

                        SafeTeleportResult.FAILED -> {
                            player.sendMessage(plugin.messages.text("error.teleport-failed"))
                        }
                    }
                }
            }
        }

        return true
    }

    private fun teleportToOnlineTarget(player: Player, target: Player): Boolean {
        when (plugin.safeTeleports.teleport(player, target.location)) {
            SafeTeleportResult.SUCCESS -> Unit
            SafeTeleportResult.NO_SAFE_GROUND -> {
                player.sendMessage(plugin.messages.text("error.no-safe-ground"))
                return true
            }

            SafeTeleportResult.FAILED -> {
                player.sendMessage(plugin.messages.text("error.teleport-failed"))
                return true
            }
        }

        player.sendMessage(plugin.messages.text("command.tp.success", "player" to target.name))
        if (plugin.config.getBoolean("teleport.notify-target", true) && !plugin.vanishService.isVanished(player)) {
            target.sendMessage(plugin.messages.text("command.tp.notify-target", "player" to player.name))
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
