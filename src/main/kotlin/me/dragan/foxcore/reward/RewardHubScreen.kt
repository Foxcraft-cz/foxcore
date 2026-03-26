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

class RewardHubScreen(
    private val plugin: FoxCorePlugin,
    private val tracks: List<RewardTrackView>,
) : GuiScreen {
    private val miniMessage = MiniMessage.miniMessage()
    private val pageSize = 28

    override val size: Int = 54

    override fun title(): Component = plugin.messages.text("command.rewards.menu-title")

    override fun render(session: GuiSession) {
        val inventory = session.inventory
        inventory.clear()

        val page = currentPage(session)
        val pageCount = totalPages()
        val entries = tracks.drop(page * pageSize).take(pageSize)
        for (slot in 0 until size) {
            inventory.setItem(slot, fillerItem())
        }

        val layoutSlots = HUB_SLOTS.take(entries.size)
        entries.forEachIndexed { index, track ->
            inventory.setItem(layoutSlots[index], trackItem(track))
        }

        if (page > 0) {
            inventory.setItem(45, navItem(Material.ARROW, plugin.messages.text("reward.gui.previous-page")))
        }
        if (page + 1 < pageCount) {
            inventory.setItem(53, navItem(Material.ARROW, plugin.messages.text("reward.gui.next-page")))
        }

        inventory.setItem(
            49,
            navItem(
                Material.BARRIER,
                plugin.messages.text("reward.gui.close"),
                plugin.messages.text("reward.gui.close-lore"),
            ),
        )
        inventory.setItem(
            48,
            navItem(
                Material.NETHER_STAR,
                plugin.messages.text("reward.gui.paths-title"),
                plugin.messages.text("reward.gui.paths-count", "count" to tracks.size.toString()),
                plugin.messages.text("reward.gui.page", "current" to (page + 1).toString(), "total" to pageCount.toString()),
            ),
        )
    }

    override fun handleClick(session: GuiSession, rawSlot: Int, click: ClickType) {
        val viewer = session.viewer() ?: return
        when (rawSlot) {
            45 -> if (currentPage(session) > 0) {
                PlayerFeedback.navigation(viewer)
                session.state["page"] = currentPage(session) - 1
                render(session)
            }

            49 -> {
                PlayerFeedback.guiClose(viewer)
                viewer.closeInventory()
            }

            53 -> if (currentPage(session) + 1 < totalPages()) {
                PlayerFeedback.navigation(viewer)
                session.state["page"] = currentPage(session) + 1
                render(session)
            }

            else -> {
                val index = HUB_SLOTS.indexOf(rawSlot)
                if (index == -1) {
                    return
                }

                val track = tracks.getOrNull(currentPage(session) * pageSize + index) ?: return
                PlayerFeedback.navigation(viewer)
                plugin.guiManager.open(viewer, RewardTrackScreen(plugin, track)) {
                    state["page"] = track.defaultPage(4)
                }
            }
        }
    }

    private fun trackItem(trackView: RewardTrackView): ItemStack =
        plugin.rewardItems.createTrackBaseItem(trackView.track).apply {
            editMeta { meta ->
                meta.displayName(miniMessage.deserialize(trackView.track.title))
                val lore = mutableListOf<Component>()
                lore += plugin.messages.text("reward.gui.progress", "value" to trackView.progressValue.toString())
                lore += plugin.messages.text("reward.gui.claimable", "count" to trackView.claimableCount.toString())
                trackView.nextRequiredProgress?.let { next ->
                    lore += plugin.messages.text("reward.gui.next-reward", "value" to next.toString())
                }
                if (!trackView.canClaim) {
                    lore += plugin.messages.text("reward.gui.browse-only")
                }
                if (trackView.track.summary.isNotEmpty()) {
                    lore += Component.empty()
                    lore += trackView.track.summary.map(miniMessage::deserialize)
                }
                lore += Component.empty()
                lore += plugin.messages.text("reward.gui.open-track")
                meta.lore(lore)
            }
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
        ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
            editMeta { meta -> meta.displayName(Component.text(" ")) }
        }

    private fun currentPage(session: GuiSession): Int =
        (session.state["page"] as? Int ?: 0).coerceIn(0, totalPages() - 1)

    private fun totalPages(): Int =
        tracks.size.coerceAtLeast(1).let { ((it - 1) / pageSize) + 1 }

    companion object {
        private val HUB_SLOTS = listOf(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43,
        )
    }
}
