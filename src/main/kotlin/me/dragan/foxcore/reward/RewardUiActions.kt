package me.dragan.foxcore.reward

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.feedback.PlayerFeedback
import org.bukkit.entity.Player

object RewardUiActions {
    fun openHub(plugin: FoxCorePlugin, player: Player) {
        player.sendMessage(plugin.messages.text("command.rewards.loading"))
        plugin.rewards.openHub(player) { result ->
            when (result) {
                RewardHubLoadResult.Disabled -> {
                    PlayerFeedback.error(player)
                    player.sendMessage(plugin.messages.text("command.rewards.disabled"))
                }

                RewardHubLoadResult.Empty -> {
                    PlayerFeedback.error(player)
                    player.sendMessage(plugin.messages.text("command.rewards.none"))
                }

                is RewardHubLoadResult.Success -> {
                    PlayerFeedback.guiOpen(player)
                    plugin.guiManager.open(player, RewardHubScreen(plugin, result.tracks))
                }
            }
        }
    }

    fun openTrack(plugin: FoxCorePlugin, player: Player, trackId: String) {
        player.sendMessage(plugin.messages.text("command.rewards.loading"))
        plugin.rewards.openTrack(player, trackId) { result ->
            when (result) {
                RewardTrackLoadResult.Disabled -> {
                    PlayerFeedback.error(player)
                    player.sendMessage(plugin.messages.text("command.rewards.disabled"))
                }

                RewardTrackLoadResult.NoAccess -> {
                    PlayerFeedback.error(player)
                    player.sendMessage(plugin.messages.text("error.no-permission"))
                }

                is RewardTrackLoadResult.NotFound -> {
                    PlayerFeedback.error(player)
                    player.sendMessage(plugin.messages.text("command.rewards.track-not-found", "track" to result.trackId))
                }

                is RewardTrackLoadResult.Success -> {
                    PlayerFeedback.guiOpen(player)
                    val defaultPage = result.track.defaultPage(4)
                    plugin.guiManager.open(player, RewardTrackScreen(plugin, result.track)) {
                        state["page"] = defaultPage
                    }
                }
            }
        }
    }
}
