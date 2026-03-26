package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.reward.RewardUiActions
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class RewardsCommand(
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

        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        when (args.size) {
            0 -> RewardUiActions.openHub(plugin, player)
            1 -> RewardUiActions.openTrack(plugin, player, args[0])
            else -> player.sendMessage(plugin.messages.text("command.rewards.usage"))
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
        if (!player.hasPermission(PERMISSION) || args.size != 1) {
            return emptyList()
        }

        return plugin.rewards.trackIdsFor(player)
            .filter { it.startsWith(args[0], ignoreCase = true) }
            .sorted()
    }

    companion object {
        private const val PERMISSION = "foxcore.rewards"
    }
}
