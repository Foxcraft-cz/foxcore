package me.dragan.foxcore.command

import me.clip.placeholderapi.PlaceholderAPI
import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.feedback.PlayerFeedback
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class RankExpiryCommand(
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

        if (args.isNotEmpty()) {
            PlayerFeedback.error(player)
            player.sendMessage(plugin.messages.text("command.konecranku.usage"))
            return true
        }

        if (plugin.server.pluginManager.getPlugin("PlaceholderAPI") == null || !hasLuckPermsExpansion(player)) {
            PlayerFeedback.error(player)
            player.sendMessage(plugin.messages.text("command.konecranku.unavailable"))
            return true
        }

        val expiry = resolveRankExpiry(player)
        if (expiry == null) {
            PlayerFeedback.error(player)
            player.sendMessage(plugin.messages.text("command.konecranku.none"))
            return true
        }

        PlayerFeedback.softSuccess(player)
        player.sendMessage(
            plugin.messages.text(
                "command.konecranku.result",
                "rank" to expiry.rankName,
                "time" to expiry.timeRemaining,
            ),
        )
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> = emptyList()

    private fun hasLuckPermsExpansion(player: Player): Boolean =
        resolvePlaceholder(player, "%luckperms_primary_group_name%") != null

    private fun resolveRankExpiry(player: Player): RankExpiry? =
        RANK_CANDIDATES.firstNotNullOfOrNull { candidate ->
            resolvePlaceholder(player, candidate.placeholder)?.let { value ->
                RankExpiry(candidate.rankName, value)
            }
        }

    private fun resolvePlaceholder(player: Player, placeholder: String): String? =
        PlaceholderAPI.setPlaceholders(player, placeholder)
            .trim()
            .takeIf(::isResolvedValue)

    private fun isResolvedValue(value: String): Boolean =
        value.isNotEmpty() && !value.contains('%')

    private data class RankExpiry(
        val rankName: String,
        val timeRemaining: String,
    )

    private data class RankCandidate(
        val rankName: String,
        val placeholder: String,
    )

    companion object {
        private val RANK_CANDIDATES = listOf(
            RankCandidate("Sampion", "%luckperms_group_expiry_time_premium%"),
            RankCandidate("Mistr", "%luckperms_group_expiry_time_vip%"),
            RankCandidate("Patron", "%luckperms_group_expiry_time_pro%"),
        )
    }
}
