package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class ReplyCommand(
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

        if (!player.hasPermission("foxcore.reply")) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        if (args.isEmpty()) {
            player.sendMessage(plugin.messages.text("command.reply.usage"))
            return true
        }

        val target = plugin.privateMessages.replyTarget(player)
        if (target == null) {
            val messageKey = if (plugin.privateMessages.hasReplyTarget(player)) {
                "command.reply.unavailable"
            } else {
                "command.reply.none"
            }
            player.sendMessage(plugin.messages.text(messageKey))
            return true
        }

        val result = plugin.privateMessages.send(player, target, args.joinToString(" "))
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
    ): List<String> = emptyList()
}
