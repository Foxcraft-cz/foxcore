package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.home.HomeIconChangeResult
import me.dragan.foxcore.home.HomeNames
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class SetHomeIconCommand(
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

        if (!player.hasPermission("foxcore.sethomeicon")) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        if (args.isEmpty() || args.size > 2) {
            player.sendMessage(plugin.messages.text("command.sethomeicon.usage"))
            return true
        }

        val homeName = HomeNames.normalize(args[0])
        if (!HomeNames.isValid(homeName)) {
            player.sendMessage(plugin.messages.text("command.sethomeicon.invalid-home"))
            return true
        }

        val material = resolveMaterial(player, args.getOrNull(1)) ?: run {
            player.sendMessage(plugin.messages.text("command.sethomeicon.invalid-icon"))
            return true
        }

        return when (val result = plugin.backService.setHomeIcon(player, homeName, material)) {
            HomeIconChangeResult.Loading -> {
                player.sendMessage(plugin.messages.text("command.sethomeicon.loading"))
                true
            }

            is HomeIconChangeResult.NotFound -> {
                player.sendMessage(plugin.messages.text("command.sethomeicon.not-found", "home" to result.homeName))
                true
            }

            is HomeIconChangeResult.Success -> {
                player.sendMessage(
                    plugin.messages.text(
                        "command.sethomeicon.success",
                        "home" to result.homeName,
                        "icon" to material.name.lowercase(),
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
            2 -> Material.entries
                .asSequence()
                .filter(Material::isItem)
                .map { it.name.lowercase() }
                .filter { it.startsWith(args[1].lowercase()) }
                .sorted()
                .toList()
            else -> emptyList()
        }
    }

    private fun resolveMaterial(player: Player, materialInput: String?): Material? {
        if (materialInput != null) {
            return Material.matchMaterial(materialInput, true)?.takeIf(Material::isItem)
        }

        val held = player.inventory.itemInMainHand.type
        return held.takeIf { it != Material.AIR && it.isItem }
    }
}
