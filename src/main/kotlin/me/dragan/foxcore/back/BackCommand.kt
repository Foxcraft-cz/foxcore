package me.dragan.foxcore.back

import me.dragan.foxcore.FoxCraftPlugin
import me.dragan.foxcore.teleport.SafeTeleportResult
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class BackCommand(
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

        if (!player.hasPermission("foxcore.back")) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        if (args.isNotEmpty()) {
            player.sendMessage(plugin.messages.text("command.back.usage"))
            return true
        }

        when (val result = plugin.backService.findBackDestination(player)) {
            is BackLookupResult.Loading -> player.sendMessage(plugin.messages.text("command.back.loading"))
            is BackLookupResult.None -> player.sendMessage(plugin.messages.text("command.back.none"))
            is BackLookupResult.MissingWorld -> {
                player.sendMessage(plugin.messages.text("command.back.missing-world", "world" to result.worldName))
            }

            is BackLookupResult.Success -> {
                plugin.backService.recordManualBackOrigin(player)
                when (plugin.safeTeleports.teleport(player, result.location)) {
                    SafeTeleportResult.SUCCESS -> player.sendMessage(plugin.messages.text("command.back.success"))
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
    ): List<String> = emptyList()
}
