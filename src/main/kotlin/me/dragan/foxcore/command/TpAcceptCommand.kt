package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCraftPlugin
import me.dragan.foxcore.teleport.SafeTeleportResult
import me.dragan.foxcore.tpa.TpaResolveStatus
import me.dragan.foxcore.tpa.TpaRequestType
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class TpAcceptCommand(
    private val plugin: FoxCraftPlugin,
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

        if (!player.hasPermission("foxcore.tpaccept")) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        if (args.size > 1) {
            player.sendMessage(plugin.messages.text("command.tpaccept.usage"))
            return true
        }

        val result = if (args.isEmpty()) {
            plugin.tpaRequests.acceptLatest(player)
        } else {
            plugin.tpaRequests.acceptFrom(player, args[0])
        }

        return when (result.status) {
            TpaResolveStatus.SUCCESS -> {
                val request = requireNotNull(result.request)
                val requester = requireNotNull(result.requester)
                val teleportedPlayer: Player
                val targetLocation = when (request.type) {
                    TpaRequestType.TELEPORT_TO_TARGET -> {
                        teleportedPlayer = requester
                        player.location
                    }
                    TpaRequestType.TELEPORT_TARGET_HERE -> {
                        teleportedPlayer = player
                        requester.location
                    }
                }

                when (plugin.safeTeleports.teleport(teleportedPlayer, targetLocation)) {
                    SafeTeleportResult.SUCCESS -> Unit
                    SafeTeleportResult.NO_SAFE_GROUND -> {
                        player.sendMessage(plugin.messages.text("error.no-safe-ground"))
                        requester.sendMessage(plugin.messages.text("error.no-safe-ground"))
                        return true
                    }

                    SafeTeleportResult.FAILED -> {
                        player.sendMessage(plugin.messages.text("error.teleport-failed"))
                        requester.sendMessage(plugin.messages.text("error.teleport-failed"))
                        return true
                    }
                }

                val acceptedKey = when (request.type) {
                    TpaRequestType.TELEPORT_TO_TARGET -> "command.tpaccept.accepted"
                    TpaRequestType.TELEPORT_TARGET_HERE -> "command.tpaccept.accepted-here"
                }
                val requesterAcceptedKey = when (request.type) {
                    TpaRequestType.TELEPORT_TO_TARGET -> "command.tpaccept.requester-accepted"
                    TpaRequestType.TELEPORT_TARGET_HERE -> "command.tpaccept.requester-accepted-here"
                }

                requester.sendMessage(plugin.messages.text(requesterAcceptedKey, "player" to player.name))
                player.sendMessage(plugin.messages.text(acceptedKey, "player" to requester.name))
                true
            }

            TpaResolveStatus.NO_PENDING -> {
                player.sendMessage(plugin.messages.text("command.tpaccept.none"))
                true
            }

            TpaResolveStatus.NOT_FOUND_FOR_REQUESTER -> {
                player.sendMessage(plugin.messages.text("command.tpaccept.not-found", "player" to args[0]))
                true
            }

            TpaResolveStatus.REQUESTER_OFFLINE -> {
                player.sendMessage(plugin.messages.text("command.tpaccept.requester-offline"))
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
        if (sender !is Player || args.size != 1) {
            return emptyList()
        }

        return plugin.tpaRequests.pendingRequesterNames(sender)
            .filter { it.startsWith(args[0], ignoreCase = true) }
            .sorted()
    }
}
