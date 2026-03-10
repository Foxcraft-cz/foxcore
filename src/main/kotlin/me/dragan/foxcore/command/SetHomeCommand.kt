package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.home.HomeNames
import me.dragan.foxcore.home.HomeSetResult
import org.bukkit.permissions.PermissionAttachmentInfo
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class SetHomeCommand(
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

        if (!player.hasPermission("foxcore.sethome")) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        if (args.size > 1) {
            player.sendMessage(plugin.messages.text("command.sethome.usage"))
            return true
        }

        val homeName = if (args.isEmpty()) HomeNames.DEFAULT else HomeNames.normalize(args[0])
        if (!HomeNames.isValid(homeName)) {
            player.sendMessage(plugin.messages.text("command.sethome.invalid-name"))
            return true
        }

        return when (val result = plugin.backService.setHome(player, homeName, resolveMaxHomes(player))) {
            HomeSetResult.Loading -> {
                player.sendMessage(plugin.messages.text("command.sethome.loading"))
                true
            }

            is HomeSetResult.LimitReached -> {
                player.sendMessage(plugin.messages.text("command.sethome.limit-reached", "max" to result.maxHomes.toString()))
                true
            }

            is HomeSetResult.Success -> {
                player.sendMessage(plugin.messages.text("command.sethome.success", "home" to result.homeName))
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

    private fun resolveMaxHomes(player: Player): Int {
        if (player.hasPermission("foxcore.sethome.limit.unlimited")) {
            return Int.MAX_VALUE
        }

        val permissionMax = player.effectivePermissions
            .asSequence()
            .filter(PermissionAttachmentInfo::getValue)
            .map { it.permission.lowercase() }
            .filter { it.startsWith("foxcore.sethome.limit.") }
            .mapNotNull { it.removePrefix("foxcore.sethome.limit.").toIntOrNull() }
            .filter { it >= 0 }
            .maxOrNull()

        val configMax = plugin.config.getInt("homes.default-max-count", 1).coerceAtLeast(0)
        return permissionMax ?: configMax
    }
}
