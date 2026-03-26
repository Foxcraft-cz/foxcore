package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.feedback.PlayerFeedback
import me.dragan.foxcore.home.HomeBrowseResult
import me.dragan.foxcore.home.HomesGuiScreen
import me.dragan.foxcore.home.HomeLookupResult
import me.dragan.foxcore.home.HomeNames
import me.dragan.foxcore.teleport.SafeTeleportResult
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class HomeCommand(
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

        if (!player.hasPermission("foxcore.home")) {
            PlayerFeedback.error(player)
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        return when (args.size) {
            0 -> openHomesGui(player)
            1 -> teleportToHome(player, args[0])
            else -> {
                PlayerFeedback.error(player)
                player.sendMessage(plugin.messages.text("command.home.usage"))
                true
            }
        }
    }

    private fun teleportToHome(player: Player, requestedHomeName: String): Boolean {
        val homeName = HomeNames.normalize(requestedHomeName)
        if (!HomeNames.isValid(homeName)) {
            PlayerFeedback.error(player)
            player.sendMessage(plugin.messages.text("command.home.invalid-name"))
            return true
        }

        return when (val result = plugin.backService.findHome(player, homeName)) {
            HomeLookupResult.Loading -> {
                PlayerFeedback.error(player)
                player.sendMessage(plugin.messages.text("command.home.loading"))
                true
            }

            is HomeLookupResult.NotFound -> {
                PlayerFeedback.error(player)
                player.sendMessage(plugin.messages.text("command.home.not-found", "home" to result.homeName))
                true
            }

            is HomeLookupResult.MissingWorld -> {
                PlayerFeedback.error(player)
                player.sendMessage(
                    plugin.messages.text(
                        "command.home.missing-world",
                        "home" to result.homeName,
                        "world" to result.worldName,
                    ),
                )
                true
            }

            is HomeLookupResult.Success -> {
                when (plugin.safeTeleports.teleport(player, result.location)) {
                    SafeTeleportResult.SUCCESS -> {
                        PlayerFeedback.teleport(player)
                        player.sendMessage(plugin.messages.text("command.home.success", "home" to result.homeName))
                    }

                    SafeTeleportResult.NO_SAFE_GROUND -> {
                        PlayerFeedback.error(player)
                        player.sendMessage(plugin.messages.text("error.no-safe-ground"))
                    }

                    SafeTeleportResult.FAILED -> {
                        PlayerFeedback.error(player)
                        player.sendMessage(plugin.messages.text("error.teleport-failed"))
                    }
                }
                true
            }
        }
    }

    private fun openHomesGui(player: Player): Boolean {
        return when (val result = plugin.backService.browseHomes(player)) {
            HomeBrowseResult.Loading -> {
                PlayerFeedback.error(player)
                player.sendMessage(plugin.messages.text("command.home.loading"))
                true
            }

            is HomeBrowseResult.Empty -> {
                PlayerFeedback.error(player)
                player.sendMessage(plugin.messages.text("command.homes.empty-self"))
                true
            }

            is HomeBrowseResult.Success -> {
                PlayerFeedback.guiOpen(player)
                plugin.guiManager.open(player, HomesGuiScreen(plugin, result.playerName, result.homes, true))
                true
            }

            is HomeBrowseResult.NotFound -> {
                PlayerFeedback.error(player)
                player.sendMessage(plugin.messages.text("command.homes.empty-self"))
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
        val player = sender as? Player ?: return emptyList()
        if (args.size != 1) {
            return emptyList()
        }

        return plugin.backService.getCachedHomeNames(player)
            .filter { it.startsWith(args[0], ignoreCase = true) }
    }
}
