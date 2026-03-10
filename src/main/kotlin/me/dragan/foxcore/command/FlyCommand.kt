package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCraftPlugin
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class FlyCommand(
    private val plugin: FoxCraftPlugin,
) : TabExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        return when (args.size) {
            0 -> toggleSelf(sender)
            1 -> toggleOther(sender, args[0])
            else -> {
                sender.sendMessage(plugin.messages.text("command.fly.usage"))
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
        if (args.size != 1 || !sender.hasPermission("foxcore.fly.others")) {
            return emptyList()
        }

        return Bukkit.getOnlinePlayers()
            .asSequence()
            .map(Player::getName)
            .filter { it.startsWith(args[0], ignoreCase = true) }
            .sorted()
            .toList()
    }

    private fun toggleSelf(sender: CommandSender): Boolean {
        val player = sender as? Player ?: run {
            sender.sendMessage(plugin.messages.text("error.player-only"))
            return true
        }

        if (!player.hasPermission("foxcore.fly")) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        if (!player.allowFlight && !player.hasPermission(worldPermission(player))) {
            player.sendMessage(plugin.messages.text("command.fly.world-blocked", "world" to player.world.name))
            return true
        }

        val enabled = applyFlightState(player)
        val key = if (enabled) "command.fly.enabled-self" else "command.fly.disabled-self"
        player.sendMessage(plugin.messages.text(key))
        return true
    }

    private fun toggleOther(sender: CommandSender, targetName: String): Boolean {
        if (!sender.hasPermission("foxcore.fly.others")) {
            sender.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        val target = Bukkit.getPlayerExact(targetName)
            ?: Bukkit.getOnlinePlayers().firstOrNull { it.name.equals(targetName, ignoreCase = true) }

        if (target == null) {
            sender.sendMessage(plugin.messages.text("command.fly.not-found", "player" to targetName))
            return true
        }

        if (!target.allowFlight && !target.hasPermission(worldPermission(target))) {
            sender.sendMessage(plugin.messages.text("command.fly.world-blocked-other", "player" to target.name, "world" to target.world.name))
            return true
        }

        val enabled = applyFlightState(target)
        val senderKey = if (enabled) "command.fly.enabled-other" else "command.fly.disabled-other"
        val targetKey = if (enabled) "command.fly.enabled-by-other" else "command.fly.disabled-by-other"

        sender.sendMessage(plugin.messages.text(senderKey, "player" to target.name))
        target.sendMessage(plugin.messages.text(targetKey, "player" to sender.name))
        return true
    }

    private fun applyFlightState(player: Player): Boolean {
        val enabled = !player.allowFlight
        player.allowFlight = enabled

        if (!enabled && !player.isOp && !player.gameMode.name.contains("CREATIVE", ignoreCase = true) && !player.gameMode.name.contains("SPECTATOR", ignoreCase = true)) {
            player.isFlying = false
        } else if (enabled) {
            player.isFlying = true
        }

        return enabled
    }

    private fun worldPermission(player: Player): String =
        "foxcore.fly.world.${player.world.name.lowercase()}"
}
