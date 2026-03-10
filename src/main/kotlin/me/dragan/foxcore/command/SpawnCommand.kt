package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCraftPlugin
import me.dragan.foxcore.teleport.SafeTeleportResult
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class SpawnCommand(
    private val plugin: FoxCraftPlugin,
) : TabExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        return when (args.size) {
            0 -> teleportSelf(sender)
            1 -> teleportOther(sender, args[0])
            else -> {
                sender.sendMessage(plugin.messages.text("command.spawn.usage"))
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
        if (args.size != 1 || !sender.hasPermission("foxcore.spawn.others")) {
            return emptyList()
        }

        return Bukkit.getOnlinePlayers()
            .asSequence()
            .map(Player::getName)
            .filter { it.startsWith(args[0], ignoreCase = true) }
            .sorted()
            .toList()
    }

    private fun teleportSelf(sender: CommandSender): Boolean {
        val player = sender as? Player ?: run {
            sender.sendMessage(plugin.messages.text("error.player-only"))
            return true
        }

        if (!player.hasPermission("foxcore.spawn")) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        val spawn = plugin.spawnService.getSpawn()
        if (spawn == null) {
            player.sendMessage(plugin.messages.text("command.spawn.not-set"))
            return true
        }

        return handleTeleport(player, spawn, self = true)
    }

    private fun teleportOther(sender: CommandSender, targetName: String): Boolean {
        if (!sender.hasPermission("foxcore.spawn.others")) {
            sender.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        val spawn = plugin.spawnService.getSpawn()
        if (spawn == null) {
            sender.sendMessage(plugin.messages.text("command.spawn.not-set"))
            return true
        }

        val target = Bukkit.getPlayerExact(targetName)
            ?: Bukkit.getOnlinePlayers().firstOrNull { it.name.equals(targetName, ignoreCase = true) }

        if (target == null) {
            sender.sendMessage(plugin.messages.text("command.spawn.not-found", "player" to targetName))
            return true
        }

        return handleTeleport(target, spawn, self = false, senderName = sender.name)
    }

    private fun handleTeleport(player: Player, spawn: org.bukkit.Location, self: Boolean, senderName: String? = null): Boolean {
        return when (plugin.safeTeleports.teleport(player, spawn)) {
            SafeTeleportResult.SUCCESS -> {
                if (self) {
                    player.sendMessage(plugin.messages.text("command.spawn.success"))
                } else {
                    requireNotNull(senderName)
                    player.sendMessage(plugin.messages.text("command.spawn.success-by-other", "player" to senderName))
                }
                true
            }

            SafeTeleportResult.NO_SAFE_GROUND -> {
                if (self) {
                    player.sendMessage(plugin.messages.text("error.no-safe-ground"))
                } else {
                    player.sendMessage(plugin.messages.text("error.no-safe-ground"))
                }
                true
            }

            SafeTeleportResult.FAILED -> {
                if (self) {
                    player.sendMessage(plugin.messages.text("error.teleport-failed"))
                } else {
                    player.sendMessage(plugin.messages.text("error.teleport-failed"))
                }
                true
            }
        }
    }
}

