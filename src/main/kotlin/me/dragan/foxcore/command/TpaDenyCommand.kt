package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCraftPlugin
import me.dragan.foxcore.tpa.TpaResolveStatus
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class TpaDenyCommand(
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

        if (!player.hasPermission("foxcore.tpadeny")) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        if (args.size > 1) {
            player.sendMessage(plugin.messages.text("command.tpadeny.usage"))
            return true
        }

        val result = if (args.isEmpty()) {
            plugin.tpaRequests.denyLatest(player)
        } else {
            plugin.tpaRequests.denyFrom(player, args[0])
        }

        return when (result.status) {
            TpaResolveStatus.SUCCESS -> {
                val requester = result.requester
                val requesterName = requester?.name ?: result.request?.requesterName ?: args.firstOrNull().orEmpty()
                requester?.sendMessage(plugin.messages.text("command.tpadeny.requester-denied", "player" to player.name))
                player.sendMessage(plugin.messages.text("command.tpadeny.denied", "player" to requesterName))
                true
            }

            TpaResolveStatus.NO_PENDING -> {
                player.sendMessage(plugin.messages.text("command.tpadeny.none"))
                true
            }

            TpaResolveStatus.NOT_FOUND_FOR_REQUESTER -> {
                player.sendMessage(plugin.messages.text("command.tpadeny.not-found", "player" to args[0]))
                true
            }

            TpaResolveStatus.REQUESTER_OFFLINE -> {
                player.sendMessage(plugin.messages.text("command.tpadeny.denied", "player" to requireNotNull(result.request).requesterName))
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

