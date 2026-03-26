package me.dragan.foxcore.home

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.feedback.PlayerFeedback
import me.dragan.foxcore.teleport.SafeTeleportResult
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack

class HomesGuiScreen(
    private val plugin: FoxCorePlugin,
    private val ownerName: String,
    private val homes: Map<String, HomeData>,
    private val selfView: Boolean,
) : me.dragan.foxcore.gui.GuiScreen {
    override val size: Int = 54

    private val miniMessage = MiniMessage.miniMessage()
    private val homeNames = homes.keys.sorted()
    private val pageSize = 45

    override fun title(): Component =
        Component.text(if (selfView) "Homes" else "$ownerName Homes")

    override fun render(session: me.dragan.foxcore.gui.GuiSession) {
        val inventory = session.inventory
        inventory.clear()

        val page = currentPage(session)
        val pageCount = totalPages()
        val start = page * pageSize
        val entries = homeNames.drop(start).take(pageSize)

        entries.forEachIndexed { index, homeName ->
            val homeData = homes.getValue(homeName)
            inventory.setItem(index, homeItem(homeName, homeData))
        }

        for (slot in 45..53) {
            inventory.setItem(slot, fillerItem())
        }

        if (page > 0) {
            inventory.setItem(45, navItem(org.bukkit.Material.ARROW, "<yellow>Previous Page</yellow>", "<gray>Go to page <white>${page}</white></gray>"))
        }
        if (page + 1 < pageCount) {
            inventory.setItem(53, navItem(org.bukkit.Material.ARROW, "<yellow>Next Page</yellow>", "<gray>Go to page <white>${page + 2}</white></gray>"))
        }

        inventory.setItem(
            49,
            navItem(
                org.bukkit.Material.BARRIER,
                "<red>Close</red>",
                "<gray>Close this menu</gray>",
            ),
        )
        inventory.setItem(
            48,
            navItem(
                org.bukkit.Material.BOOK,
                "<gold>${ownerName}</gold>",
                "<gray>Homes: <white>${homeNames.size}</white></gray>",
                "<gray>Page: <white>${page + 1}</white>/<white>$pageCount</white></gray>",
            ),
        )
    }

    override fun handleClick(session: me.dragan.foxcore.gui.GuiSession, rawSlot: Int, click: ClickType) {
        val viewer = session.viewer() ?: return
        when (rawSlot) {
            45 -> {
                if (currentPage(session) > 0) {
                    PlayerFeedback.navigation(viewer)
                    session.state["page"] = currentPage(session) - 1
                    render(session)
                }
            }

            49 -> {
                PlayerFeedback.guiClose(viewer)
                viewer.closeInventory()
            }

            53 -> {
                if (currentPage(session) + 1 < totalPages()) {
                    PlayerFeedback.navigation(viewer)
                    session.state["page"] = currentPage(session) + 1
                    render(session)
                }
            }

            in 0 until pageSize -> {
                val index = currentPage(session) * pageSize + rawSlot
                val homeName = homeNames.getOrNull(index) ?: return
                val stored = homes[homeName] ?: return
                val world = plugin.server.getWorld(stored.location.worldName)
                if (world == null) {
                    PlayerFeedback.error(viewer)
                    viewer.sendMessage(
                        plugin.messages.text(
                            "command.home.missing-world",
                            "home" to homeName,
                            "world" to stored.location.worldName,
                        ),
                    )
                    PlayerFeedback.guiClose(viewer)
                    viewer.closeInventory()
                    return
                }

                PlayerFeedback.guiClose(viewer)
                viewer.closeInventory()
                when (plugin.safeTeleports.teleport(viewer, stored.location.toBukkitLocation(world))) {
                    SafeTeleportResult.SUCCESS -> {
                        PlayerFeedback.teleport(viewer)
                        val key = if (selfView) "command.home.success" else "command.homes.teleport-other-success"
                        viewer.sendMessage(plugin.messages.text(key, "home" to homeName, "player" to ownerName))
                    }

                    SafeTeleportResult.NO_SAFE_GROUND -> {
                        PlayerFeedback.error(viewer)
                        viewer.sendMessage(plugin.messages.text("error.no-safe-ground"))
                    }

                    SafeTeleportResult.FAILED -> {
                        PlayerFeedback.error(viewer)
                        viewer.sendMessage(plugin.messages.text("error.teleport-failed"))
                    }
                }
            }
        }
    }

    private fun currentPage(session: me.dragan.foxcore.gui.GuiSession): Int =
        (session.state["page"] as? Int ?: 0).coerceIn(0, totalPages() - 1)

    private fun totalPages(): Int =
        homeNames.size.coerceAtLeast(1).let { ((it - 1) / pageSize) + 1 }

    private fun homeItem(homeName: String, homeData: HomeData): ItemStack =
        ItemStack(homeData.iconMaterial()).apply {
            editMeta { meta ->
                meta.displayName(miniMessage.deserialize("<gold>$homeName</gold>"))
                meta.lore(
                    listOf(
                        miniMessage.deserialize("<gray>World: <white>${homeData.location.worldName}</white></gray>"),
                        miniMessage.deserialize("<gray>X: <white>${homeData.location.x.toInt()}</white> Y: <white>${homeData.location.y.toInt()}</white> Z: <white>${homeData.location.z.toInt()}</white></gray>"),
                        miniMessage.deserialize(if (selfView) "<yellow>Click to teleport</yellow>" else "<yellow>Click to teleport to this home</yellow>"),
                    ),
                )
            }
        }

    private fun fillerItem(): ItemStack =
        ItemStack(org.bukkit.Material.GRAY_STAINED_GLASS_PANE).apply {
            editMeta { meta -> meta.displayName(Component.text(" ")) }
        }

    private fun navItem(material: org.bukkit.Material, name: String, vararg lore: String): ItemStack =
        ItemStack(material).apply {
            editMeta { meta ->
                meta.displayName(miniMessage.deserialize(name))
                meta.lore(lore.map(miniMessage::deserialize))
            }
        }
}
