package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.home.HomeBrowseResult
import me.dragan.foxcore.home.HomesGuiScreen
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class HomesCommand(
    private val plugin: FoxCorePlugin,
) : TabExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        return when (args.size) {
            0 -> listSelf(sender)
            1 -> listOther(sender, args[0])
            else -> {
                sender.sendMessage(plugin.messages.text("command.homes.usage"))
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
        if (args.size != 1 || !sender.hasPermission("foxcore.homes.others")) {
            return emptyList()
        }

        return Bukkit.getOnlinePlayers()
            .asSequence()
            .map(Player::getName)
            .filter { it.startsWith(args[0], ignoreCase = true) }
            .sorted()
            .toList()
    }

    private fun listSelf(sender: CommandSender): Boolean {
        val player = sender as? Player ?: run {
            sender.sendMessage(plugin.messages.text("error.player-only"))
            return true
        }

        if (!player.hasPermission("foxcore.homes")) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        return openHomesGui(player, plugin.backService.browseHomes(player), self = true)
    }

    private fun listOther(sender: CommandSender, requestedName: String): Boolean {
        if (!sender.hasPermission("foxcore.homes.others")) {
            sender.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        val player = sender as? Player ?: run {
            sender.sendMessage(plugin.messages.text("error.player-only"))
            return true
        }

        plugin.backService.browseHomesByLastKnownName(requestedName) { result ->
            openHomesGui(player, result, self = false)
        }
        return true
    }

    private fun openHomesGui(sender: Player, result: HomeBrowseResult, self: Boolean): Boolean {
        when (result) {
            HomeBrowseResult.Loading -> {
                sender.sendMessage(plugin.messages.text("command.homes.loading"))
            }

            is HomeBrowseResult.NotFound -> {
                sender.sendMessage(plugin.messages.text("command.homes.player-not-found", "player" to result.requestedPlayerName))
            }

            is HomeBrowseResult.Empty -> {
                val key = if (self) "command.homes.empty-self" else "command.homes.empty-other"
                sender.sendMessage(plugin.messages.text(key, "player" to result.playerName))
            }

            is HomeBrowseResult.Success -> {
                plugin.guiManager.open(sender, HomesGuiScreen(plugin, result.playerName, result.homes, self))
            }
        }
        return true
    }
}
