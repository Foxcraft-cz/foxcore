package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.vote.VoteAction
import me.dragan.foxcore.vote.VoteStartResult
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class WorldVoteCommand(
    private val plugin: FoxCorePlugin,
    private val action: VoteAction,
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

        if (args.isEmpty()) {
            return handleStart(player)
        }

        if (args.size != 1) {
            player.sendMessage(plugin.messages.text("command.${action.commandLabel}.usage"))
            return true
        }

        return if (args[0].equals("force", ignoreCase = true)) {
            handleForce(player)
        } else {
            player.sendMessage(plugin.messages.text("command.${action.commandLabel}.usage"))
            true
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> {
        if (args.size != 1) {
            return emptyList()
        }

        if (sender is Player && !sender.hasPermission(action.forcePermission)) {
            return emptyList()
        }

        return listOf("force").filter { it.startsWith(args[0], ignoreCase = true) }
    }

    private fun handleStart(player: Player): Boolean {
        if (!player.hasPermission(action.startPermission)) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        return when (val result = plugin.votes.startVote(player, action)) {
            VoteStartResult.Disabled -> {
                player.sendMessage(plugin.messages.text("command.vote.disabled"))
                true
            }
            is VoteStartResult.Cooldown -> {
                player.sendMessage(
                    plugin.messages.text(
                        "command.vote.cooldown",
                        "command" to "/${action.commandLabel}",
                        "seconds" to result.secondsRemaining.toString(),
                    ),
                )
                true
            }
            is VoteStartResult.Active -> {
                player.sendMessage(
                    plugin.messages.text(
                        "command.vote.active",
                        "action" to plugin.votes.actionLabel(result.action),
                        "yes-command" to "/voteyes",
                        "no-command" to "/voteno",
                    ),
                )
                true
            }
            is VoteStartResult.Started -> true
        }
    }

    private fun handleForce(player: Player): Boolean {
        if (!player.hasPermission(action.forcePermission)) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        plugin.votes.forceVote(player, action)
        return true
    }
}
