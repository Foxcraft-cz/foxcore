package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.home.HomeDeleteResult
import me.dragan.foxcore.home.HomeNames
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class DeleteHomeCommand(
    private val plugin: FoxCorePlugin,
) : TabExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        return when (args.size) {
            1 -> deleteOwnHome(sender, args[0])
            2 -> deleteOtherHome(sender, args[0], args[1])
            else -> {
                sender.sendMessage(plugin.messages.text("command.delhome.usage"))
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
        return when (args.size) {
            1 -> {
                val player = sender as? Player ?: return emptyList()
                plugin.backService.getCachedHomeNames(player)
                    .filter { it.startsWith(args[0], ignoreCase = true) }
            }

            2 -> {
                if (!sender.hasPermission("foxcore.delhome.others")) {
                    return emptyList()
                }

                val target = Bukkit.getPlayerExact(args[0])
                    ?: Bukkit.getOnlinePlayers().firstOrNull { it.name.equals(args[0], ignoreCase = true) }

                if (target != null) {
                    plugin.backService.getCachedHomeNames(target)
                        .filter { it.startsWith(args[1], ignoreCase = true) }
                } else {
                    emptyList()
                }
            }

            else -> emptyList()
        }
    }

    private fun deleteOwnHome(sender: CommandSender, requestedHomeName: String): Boolean {
        val player = sender as? Player ?: run {
            sender.sendMessage(plugin.messages.text("error.player-only"))
            return true
        }

        if (!player.hasPermission("foxcore.delhome")) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        val homeName = HomeNames.normalize(requestedHomeName)
        if (!HomeNames.isValid(homeName)) {
            player.sendMessage(plugin.messages.text("command.delhome.invalid-name"))
            return true
        }

        return when (val result = plugin.backService.deleteHome(player, homeName)) {
            HomeDeleteResult.Loading -> {
                player.sendMessage(plugin.messages.text("command.delhome.loading"))
                true
            }

            is HomeDeleteResult.HomeNotFound -> {
                player.sendMessage(plugin.messages.text("command.delhome.not-found", "home" to result.homeName))
                true
            }

            is HomeDeleteResult.Success -> {
                player.sendMessage(plugin.messages.text("command.delhome.success-self", "home" to result.homeName))
                true
            }

            is HomeDeleteResult.PlayerNotFound -> {
                player.sendMessage(plugin.messages.text("command.delhome.not-found", "home" to homeName))
                true
            }
        }
    }

    private fun deleteOtherHome(sender: CommandSender, requestedPlayerName: String, requestedHomeName: String): Boolean {
        if (!sender.hasPermission("foxcore.delhome.others")) {
            sender.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        val homeName = HomeNames.normalize(requestedHomeName)
        if (!HomeNames.isValid(homeName)) {
            sender.sendMessage(plugin.messages.text("command.delhome.invalid-name"))
            return true
        }

        plugin.backService.deleteHomeByLastKnownName(requestedPlayerName, homeName) { result ->
            when (result) {
                HomeDeleteResult.Loading -> {
                    sender.sendMessage(plugin.messages.text("command.delhome.loading"))
                }

                is HomeDeleteResult.PlayerNotFound -> {
                    sender.sendMessage(plugin.messages.text("command.delhome.player-not-found", "player" to result.playerName))
                }

                is HomeDeleteResult.HomeNotFound -> {
                    sender.sendMessage(
                        plugin.messages.text(
                            "command.delhome.not-found-other",
                            "player" to result.playerName,
                            "home" to result.homeName,
                        ),
                    )
                }

                is HomeDeleteResult.Success -> {
                    sender.sendMessage(
                        plugin.messages.text(
                            "command.delhome.success-other",
                            "player" to result.playerName,
                            "home" to result.homeName,
                        ),
                    )
                }
            }
        }
        return true
    }
}
