package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.feedback.PlayerFeedback
import me.dragan.foxcore.vote.VoteCastResult
import me.dragan.foxcore.vote.VoteChoice
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class VoteChoiceCommand(
    private val plugin: FoxCorePlugin,
    private val choice: VoteChoice,
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

        if (args.isNotEmpty()) {
            player.sendMessage(plugin.messages.text("command.${command.name.lowercase()}.usage"))
            return true
        }

        return when (val result = plugin.votes.castVote(player, choice)) {
            VoteCastResult.NoActiveVote -> {
                PlayerFeedback.error(player)
                player.sendMessage(plugin.messages.text("command.vote.no-active"))
                true
            }
            is VoteCastResult.Recorded -> {
                PlayerFeedback.voteCast(player)
                player.sendMessage(plugin.messages.text("command.vote.recorded.${result.choice.name.lowercase()}"))
                true
            }
            is VoteCastResult.Changed -> {
                PlayerFeedback.voteCast(player)
                player.sendMessage(plugin.messages.text("command.vote.changed.${result.choice.name.lowercase()}"))
                true
            }
            is VoteCastResult.AlreadyVoted -> {
                PlayerFeedback.error(player)
                player.sendMessage(plugin.messages.text("command.vote.already.${result.choice.name.lowercase()}"))
                true
            }
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> = emptyList()
}
