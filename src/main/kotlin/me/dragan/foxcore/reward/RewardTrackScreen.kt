package me.dragan.foxcore.reward

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.feedback.PlayerFeedback
import me.dragan.foxcore.gui.GuiScreen
import me.dragan.foxcore.gui.GuiSession
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack

class RewardTrackScreen(
    private val plugin: FoxCorePlugin,
    private var trackView: RewardTrackView,
) : GuiScreen {
    private val miniMessage = MiniMessage.miniMessage()
    private val pageSize = CART_SLOTS.size

    override val size: Int = 27

    override fun title(): Component = miniMessage.deserialize(trackView.track.title)

    override fun render(session: GuiSession) {
        val inventory = session.inventory
        inventory.clear()
        for (slot in 0 until size) {
            inventory.setItem(slot, fillerItem())
        }

        val page = currentPage(session)
        val pageCount = totalPages()
        val rewards = trackView.rewardViews.drop(page * pageSize).take(pageSize)

        rewards.forEachIndexed { index, rewardView ->
            inventory.setItem(ICON_SLOTS[index], rewardPreviewItem(rewardView))
        }

        CONNECTOR_SLOTS.forEachIndexed { index, slot ->
            val left = rewards.getOrNull(index) ?: return@forEachIndexed
            val right = rewards.getOrNull(index + 1) ?: return@forEachIndexed
            inventory.setItem(slot, connectorItem(left, right))
        }

        rewards.forEachIndexed { index, rewardView ->
            inventory.setItem(CART_SLOTS[index], rewardCartItem(rewardView))
        }

        inventory.setItem(
            22,
            navItem(
                Material.BOOK,
                miniMessage.deserialize(trackView.track.title),
                plugin.messages.text("reward.gui.progress", "value" to trackView.progressValue.toString()),
                plugin.messages.text("reward.gui.claimable", "count" to trackView.claimableCount.toString()),
                trackView.nextRequiredProgress?.let {
                    plugin.messages.text("reward.gui.next-reward", "value" to it.toString())
                } ?: plugin.messages.text("reward.gui.all-claimed"),
            ),
        )
        inventory.setItem(18, navItem(Material.ARROW, plugin.messages.text("reward.gui.back"), plugin.messages.text("reward.gui.back-lore")))
        if (page > 0) {
            inventory.setItem(20, navItem(Material.SPECTRAL_ARROW, plugin.messages.text("reward.gui.previous-page")))
        }
        if (page + 1 < pageCount) {
            inventory.setItem(24, navItem(Material.SPECTRAL_ARROW, plugin.messages.text("reward.gui.next-page")))
        }
        inventory.setItem(26, navItem(Material.BARRIER, plugin.messages.text("reward.gui.close"), plugin.messages.text("reward.gui.close-lore")))
    }

    override fun handleClick(session: GuiSession, rawSlot: Int, click: ClickType) {
        val viewer = session.viewer() ?: return
        when (rawSlot) {
            18 -> {
                PlayerFeedback.navigation(viewer)
                RewardUiActions.openHub(plugin, viewer)
            }

            20 -> if (currentPage(session) > 0) {
                PlayerFeedback.navigation(viewer)
                session.state["page"] = currentPage(session) - 1
                render(session)
            }

            24 -> if (currentPage(session) + 1 < totalPages()) {
                PlayerFeedback.navigation(viewer)
                session.state["page"] = currentPage(session) + 1
                render(session)
            }

            26 -> {
                PlayerFeedback.guiClose(viewer)
                viewer.closeInventory()
            }

            else -> {
                val rewardIndex = when {
                    ICON_SLOTS.contains(rawSlot) -> ICON_SLOTS.indexOf(rawSlot)
                    CART_SLOTS.contains(rawSlot) -> CART_SLOTS.indexOf(rawSlot)
                    else -> -1
                }
                if (rewardIndex == -1) {
                    return
                }

                val rewardView = trackView.rewardViews.getOrNull(currentPage(session) * pageSize + rewardIndex) ?: return
                if (!trackView.canClaim) {
                    PlayerFeedback.error(viewer)
                    viewer.sendMessage(plugin.messages.text("error.no-permission"))
                    return
                }
                if (rewardView.status != RewardClaimStatus.CLAIMABLE) {
                    PlayerFeedback.error(viewer)
                    when (rewardView.status) {
                        RewardClaimStatus.CLAIMED -> viewer.sendMessage(plugin.messages.text("reward.already-claimed"))
                        RewardClaimStatus.LOCKED -> viewer.sendMessage(
                            plugin.messages.text(
                                "reward.not-ready",
                                "required" to rewardView.reward.requiredProgress.toString(),
                                "current" to trackView.progressValue.toString(),
                            ),
                        )
                        RewardClaimStatus.CLAIMABLE -> Unit
                    }
                    return
                }

                val claimKey = "claim:${trackView.track.id}:${rewardView.reward.id}"
                if (session.state[claimKey] == true) {
                    return
                }

                session.state[claimKey] = true
                plugin.rewards.claimReward(viewer, trackView.track.id, rewardView.reward.id) { result ->
                    session.state.remove(claimKey)
                    when (result) {
                        RewardClaimResult.Disabled -> {
                            PlayerFeedback.error(viewer)
                            viewer.sendMessage(plugin.messages.text("command.rewards.disabled"))
                        }

                        RewardClaimResult.NoAccess,
                        RewardClaimResult.NoClaimPermission -> {
                            PlayerFeedback.error(viewer)
                            viewer.sendMessage(plugin.messages.text("error.no-permission"))
                        }

                        RewardClaimResult.AlreadyClaimed -> {
                            PlayerFeedback.error(viewer)
                            viewer.sendMessage(plugin.messages.text("reward.already-claimed"))
                        }

                        is RewardClaimResult.TrackNotFound -> {
                            PlayerFeedback.error(viewer)
                            viewer.sendMessage(plugin.messages.text("command.rewards.track-not-found", "track" to result.trackId))
                        }

                        is RewardClaimResult.RewardNotFound -> {
                            PlayerFeedback.error(viewer)
                            viewer.sendMessage(plugin.messages.text("reward.not-found", "reward" to result.rewardId))
                        }

                        is RewardClaimResult.NotEligible -> {
                            PlayerFeedback.error(viewer)
                            viewer.sendMessage(
                                plugin.messages.text(
                                    "reward.not-ready",
                                    "required" to result.required.toString(),
                                    "current" to result.current.toString(),
                                ),
                            )
                        }

                        is RewardClaimResult.Success -> {
                            PlayerFeedback.success(viewer)
                            trackView = result.track
                            render(session)
                            viewer.sendMessage(
                                plugin.messages.text(
                                    "reward.claimed",
                                    "reward" to plainTitle(result.reward.title ?: result.reward.id),
                                    "track" to plainTitle(trackView.track.title),
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun rewardPreviewItem(rewardView: RewardEntryView): ItemStack =
        plugin.rewardItems.createRewardBaseItem(rewardView.reward).apply {
            editMeta { meta ->
                val title = rewardView.reward.title ?: defaultRewardTitle(rewardView.reward)
                meta.displayName(miniMessage.deserialize(title))
                val lore = mutableListOf<Component>()
                lore += plugin.messages.text("reward.gui.required-progress", "value" to rewardView.reward.requiredProgress.toString())
                lore += plugin.messages.text(statusPath(rewardView.status))
                if (rewardView.reward.description.isNotEmpty()) {
                    lore += Component.empty()
                    lore += rewardView.reward.description.map(miniMessage::deserialize)
                }
                lore += Component.empty()
                lore += plugin.messages.text(actionPath(rewardView.status))
                meta.lore(lore)
            }
        }

    private fun rewardCartItem(rewardView: RewardEntryView): ItemStack =
        ItemStack(
            when (rewardView.status) {
                RewardClaimStatus.CLAIMED -> Material.MINECART
                RewardClaimStatus.CLAIMABLE -> Material.CHEST_MINECART
                RewardClaimStatus.LOCKED -> Material.FURNACE_MINECART
            },
        ).apply {
            editMeta { meta ->
                meta.displayName(miniMessage.deserialize(rewardView.reward.title ?: defaultRewardTitle(rewardView.reward)))
                meta.lore(
                    listOf(
                        plugin.messages.text(statusPath(rewardView.status)),
                        plugin.messages.text("reward.gui.required-progress", "value" to rewardView.reward.requiredProgress.toString()),
                        plugin.messages.text(actionPath(rewardView.status)),
                    ),
                )
            }
        }

    private fun connectorItem(left: RewardEntryView, right: RewardEntryView): ItemStack =
        ItemStack(
            when {
                left.status == RewardClaimStatus.CLAIMED && right.status == RewardClaimStatus.CLAIMED -> Material.POWERED_RAIL
                left.status == RewardClaimStatus.CLAIMABLE || right.status == RewardClaimStatus.CLAIMABLE -> Material.RAIL
                else -> Material.DETECTOR_RAIL
            },
        ).apply {
            editMeta { meta -> meta.displayName(Component.text(" ")) }
        }

    private fun navItem(material: Material, name: Component, vararg lore: Component): ItemStack =
        ItemStack(material).apply {
            editMeta { meta ->
                meta.displayName(name)
                meta.lore(lore.toList())
            }
        }

    private fun navItem(material: Material, name: String, vararg lore: String): ItemStack =
        ItemStack(material).apply {
            editMeta { meta ->
                meta.displayName(miniMessage.deserialize(name))
                meta.lore(lore.map(miniMessage::deserialize))
            }
        }

    private fun fillerItem(): ItemStack =
        ItemStack(Material.BLACK_STAINED_GLASS_PANE).apply {
            editMeta { meta -> meta.displayName(Component.text(" ")) }
        }

    private fun defaultRewardTitle(reward: RewardEntry): String =
        "<gold>${reward.requiredProgress}</gold>"

    private fun statusPath(status: RewardClaimStatus): String =
        when (status) {
            RewardClaimStatus.CLAIMED -> "reward.gui.status.claimed"
            RewardClaimStatus.CLAIMABLE -> "reward.gui.status.claimable"
            RewardClaimStatus.LOCKED -> "reward.gui.status.locked"
        }

    private fun actionPath(status: RewardClaimStatus): String =
        when (status) {
            RewardClaimStatus.CLAIMED -> "reward.gui.action.claimed"
            RewardClaimStatus.CLAIMABLE -> "reward.gui.action.claimable"
            RewardClaimStatus.LOCKED -> "reward.gui.action.locked"
        }

    private fun plainTitle(title: String): String =
        title.replace(Regex("<[^>]+>"), "").trim().ifBlank { trackView.track.id }

    private fun currentPage(session: GuiSession): Int =
        (session.state["page"] as? Int ?: trackView.defaultPage(pageSize)).coerceIn(0, totalPages() - 1)

    private fun totalPages(): Int =
        trackView.rewardViews.size.coerceAtLeast(1).let { ((it - 1) / pageSize) + 1 }

    companion object {
        private val ICON_SLOTS = listOf(1, 3, 5, 7)
        private val CART_SLOTS = listOf(10, 12, 14, 16)
        private val CONNECTOR_SLOTS = listOf(11, 13, 15)
    }
}
