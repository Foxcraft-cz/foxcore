package me.dragan.foxcore.back

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.back.BackMode.AUTO
import me.dragan.foxcore.back.BackMode.DEATH
import me.dragan.foxcore.back.BackMode.TELEPORT
import me.dragan.foxcore.teleport.SafeTeleportResult
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class BackCommand(
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

        val mode = when (args.size) {
            0 -> AUTO
            1 -> when (args[0].lowercase()) {
                "teleport", "tp" -> TELEPORT
                "death" -> DEATH
                else -> {
                    player.sendMessage(plugin.messages.text("command.back.usage"))
                    return true
                }
            }

            else -> {
                player.sendMessage(plugin.messages.text("command.back.usage"))
                return true
            }
        }

        val canUseTeleport = player.hasPermission(TELEPORT_PERMISSION)
        val canUseDeath = player.hasPermission(DEATH_PERMISSION)

        when (val result = plugin.backService.findBackDestination(player, mode, canUseTeleport, canUseDeath)) {
            is BackLookupResult.Loading -> player.sendMessage(plugin.messages.text("command.back.loading"))
            is BackLookupResult.NoEligiblePermission -> player.sendMessage(plugin.messages.text("error.no-permission"))
            is BackLookupResult.None -> player.sendMessage(plugin.messages.text("command.back.none"))
            is BackLookupResult.MissingLocation -> {
                val key = when (result.type) {
                    BackType.TELEPORT -> "command.back.none-teleport"
                    BackType.DEATH -> "command.back.none-death"
                }
                player.sendMessage(plugin.messages.text(key))
            }
            is BackLookupResult.MissingWorld -> {
                player.sendMessage(plugin.messages.text("command.back.missing-world", "world" to result.worldName))
            }

            is BackLookupResult.Success -> {
                plugin.backService.recordManualBackOrigin(player)
                when (plugin.safeTeleports.teleport(player, result.location)) {
                    SafeTeleportResult.SUCCESS -> {
                        val key = when (result.type) {
                            BackType.TELEPORT -> "command.back.success-teleport"
                            BackType.DEATH -> "command.back.success-death"
                        }
                        player.sendMessage(plugin.messages.text(key))
                    }
                    SafeTeleportResult.NO_SAFE_GROUND -> player.sendMessage(plugin.messages.text("error.no-safe-ground"))
                    SafeTeleportResult.FAILED -> player.sendMessage(plugin.messages.text("error.teleport-failed"))
                }
            }
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

        return listOf("teleport", "death")
            .filter { it.startsWith(args[0], ignoreCase = true) }
            .sorted()
    }

    companion object {
        private const val TELEPORT_PERMISSION = "foxcore.back.teleport"
        private const val DEATH_PERMISSION = "foxcore.back.death"
    }
}
