package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class FeedCommand(
    private val plugin: FoxCorePlugin,
) : TabExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        return when (args.size) {
            0 -> feedSelf(sender)
            1 -> feedOther(sender, args[0])
            else -> {
                sender.sendMessage(plugin.messages.text("command.feed.usage"))
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
        if (args.size != 1 || !sender.hasPermission("foxcore.feed.others")) {
            return emptyList()
        }

        return Bukkit.getOnlinePlayers()
            .asSequence()
            .map(Player::getName)
            .filter { it.startsWith(args[0], ignoreCase = true) }
            .sorted()
            .toList()
    }

    private fun feedSelf(sender: CommandSender): Boolean {
        val player = sender as? Player ?: run {
            sender.sendMessage(plugin.messages.text("error.player-only"))
            return true
        }

        if (!player.hasPermission("foxcore.feed")) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        applyFeed(player)
        player.sendMessage(plugin.messages.text("command.feed.success-self"))
        return true
    }

    private fun feedOther(sender: CommandSender, targetName: String): Boolean {
        if (!sender.hasPermission("foxcore.feed.others")) {
            sender.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        val target = Bukkit.getPlayerExact(targetName)
            ?: Bukkit.getOnlinePlayers().firstOrNull { it.name.equals(targetName, ignoreCase = true) }

        if (target == null) {
            sender.sendMessage(plugin.messages.text("command.feed.not-found", "player" to targetName))
            return true
        }

        applyFeed(target)
        sender.sendMessage(plugin.messages.text("command.feed.success-other", "player" to target.name))
        target.sendMessage(plugin.messages.text("command.feed.success-by-other", "player" to sender.name))
        return true
    }

    private fun applyFeed(player: Player) {
        player.foodLevel = 20
        player.saturation = 20.0f
        player.exhaustion = 0.0f
    }
}
