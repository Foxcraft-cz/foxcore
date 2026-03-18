package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class MessageCommand(
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

        if (!player.hasPermission("foxcore.message")) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        if (args.size < 2) {
            player.sendMessage(plugin.messages.text("command.message.usage"))
            return true
        }

        val target = plugin.privateMessages.findVisibleTarget(player, args[0])
        if (target == null) {
            player.sendMessage(plugin.messages.text("command.message.not-found", "player" to args[0]))
            return true
        }

        if (target.uniqueId == player.uniqueId) {
            player.sendMessage(plugin.messages.text("command.message.self"))
            return true
        }

        val result = plugin.privateMessages.send(player, target, args.drop(1).joinToString(" "))
        if (!result.success) {
            player.sendMessage(plugin.messages.text(result.denialKey ?: "chat.private.blocked", *result.placeholders))
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> {
        val player = sender as? Player ?: return emptyList()
        return if (args.size == 1) {
            plugin.privateMessages.visiblePlayerNames(player, args[0])
        } else {
            emptyList()
        }
    }
}
