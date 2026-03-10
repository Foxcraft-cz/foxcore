package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.home.HomeNames
import me.dragan.foxcore.home.HomeRenameResult
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class RenameHomeCommand(
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

        if (!player.hasPermission("foxcore.renamehome")) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        if (args.size != 2) {
            player.sendMessage(plugin.messages.text("command.renamehome.usage"))
            return true
        }

        val oldHomeName = HomeNames.normalize(args[0])
        val newHomeName = HomeNames.normalize(args[1])
        if (!HomeNames.isValid(oldHomeName) || !HomeNames.isValid(newHomeName)) {
            player.sendMessage(plugin.messages.text("command.renamehome.invalid-name"))
            return true
        }

        return when (val result = plugin.backService.renameHome(player, oldHomeName, newHomeName)) {
            HomeRenameResult.Loading -> {
                player.sendMessage(plugin.messages.text("command.renamehome.loading"))
                true
            }

            is HomeRenameResult.NotFound -> {
                player.sendMessage(plugin.messages.text("command.renamehome.not-found", "home" to result.homeName))
                true
            }

            is HomeRenameResult.AlreadyExists -> {
                player.sendMessage(plugin.messages.text("command.renamehome.already-exists", "home" to result.homeName))
                true
            }

            is HomeRenameResult.SameName -> {
                player.sendMessage(plugin.messages.text("command.renamehome.same-name", "home" to result.homeName))
                true
            }

            is HomeRenameResult.Success -> {
                player.sendMessage(
                    plugin.messages.text(
                        "command.renamehome.success",
                        "old" to result.oldHomeName,
                        "new" to result.newHomeName,
                    ),
                )
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
        return when (args.size) {
            1 -> plugin.backService.getCachedHomeNames(player)
                .filter { it.startsWith(args[0], ignoreCase = true) }
            else -> emptyList()
        }
    }
}
