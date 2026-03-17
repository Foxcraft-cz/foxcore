package me.dragan.foxcore.warp

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.gui.GuiScreen
import me.dragan.foxcore.gui.GuiSession
import me.dragan.foxcore.teleport.SafeTeleportResult
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack

class WarpGuiScreen(
    private val plugin: FoxCorePlugin,
    private val warps: List<WarpData>,
) : GuiScreen {
    override val size: Int = 54

    private val miniMessage = MiniMessage.miniMessage()
    private val pageSize = 45

    override fun title(): Component = plugin.messages.text("command.warp.menu-title")

    override fun render(session: GuiSession) {
        val inventory = session.inventory
        inventory.clear()

        val page = currentPage(session)
        val pageCount = totalPages()
        val entries = warps.drop(page * pageSize).take(pageSize)

        entries.forEachIndexed { index, warp ->
            inventory.setItem(index, warpItem(warp))
        }

        for (slot in 45..53) {
            inventory.setItem(slot, fillerItem())
        }

        if (page > 0) {
            inventory.setItem(45, navItem(Material.ARROW, "<yellow>Previous Page</yellow>"))
        }
        if (page + 1 < pageCount) {
            inventory.setItem(53, navItem(Material.ARROW, "<yellow>Next Page</yellow>"))
        }

        inventory.setItem(49, navItem(Material.BARRIER, "<red>Close</red>"))
        inventory.setItem(
            48,
            navItem(
                Material.BOOK,
                "<gold>Warps</gold>",
                "<gray>Total: <white>${warps.size}</white></gray>",
                "<gray>Page: <white>${page + 1}</white>/<white>$pageCount</white></gray>",
            ),
        )
    }

    override fun handleClick(session: GuiSession, rawSlot: Int, click: ClickType) {
        val viewer = session.viewer() ?: return
        when (rawSlot) {
            45 -> if (currentPage(session) > 0) {
                session.state["page"] = currentPage(session) - 1
                render(session)
            }

            49 -> viewer.closeInventory()
            53 -> if (currentPage(session) + 1 < totalPages()) {
                session.state["page"] = currentPage(session) + 1
                render(session)
            }

            in 0 until pageSize -> {
                val warp = warps.getOrNull(currentPage(session) * pageSize + rawSlot) ?: return
                viewer.closeInventory()
                teleportToWarp(viewer, warp)
            }
        }
    }

    private fun teleportToWarp(player: org.bukkit.entity.Player, warp: WarpData) {
        val cooldown = plugin.warps.remainingTeleportCooldownSeconds(player)
        if (cooldown > 0 && !player.hasPermission("foxcore.warp.bypasscooldown")) {
            player.sendMessage(plugin.messages.text("command.warp.cooldown", "seconds" to cooldown.toString()))
            return
        }

        val world = plugin.server.getWorld(warp.location.worldName)
        if (world == null) {
            player.sendMessage(plugin.messages.text("command.warp.missing-world", "warp" to warp.name, "world" to warp.location.worldName))
            return
        }

        when (plugin.safeTeleports.teleport(player, warp.location.toBukkitLocation(world))) {
            SafeTeleportResult.SUCCESS -> {
                plugin.warps.markTeleportUsed(player)
                player.sendMessage(plugin.messages.text("command.warp.success", "warp" to warp.name))
            }

            SafeTeleportResult.NO_SAFE_GROUND -> {
                player.sendMessage(plugin.messages.text("error.no-safe-ground"))
            }

            SafeTeleportResult.FAILED -> {
                player.sendMessage(plugin.messages.text("error.teleport-failed"))
            }
        }
    }

    private fun currentPage(session: GuiSession): Int =
        (session.state["page"] as? Int ?: 0).coerceIn(0, totalPages() - 1)

    private fun totalPages(): Int =
        warps.size.coerceAtLeast(1).let { ((it - 1) / pageSize) + 1 }

    private fun warpItem(warp: WarpData): ItemStack =
        ItemStack(warp.iconMaterial()).apply {
            editMeta { meta ->
                meta.displayName(miniMessage.deserialize(warp.title?.ifBlank { warp.name } ?: warp.name))
                val lore = mutableListOf<Component>()
                lore += miniMessage.deserialize("<gray>Name: <white>${warp.name}</white></gray>")
                lore += miniMessage.deserialize(
                    if (warp.scope == WarpScope.SERVER) {
                        "<gray>Owner: <gold>Server</gold></gray>"
                    } else {
                        "<gray>Owner: <white>${warp.ownerName ?: "Unknown"}</white></gray>"
                    },
                )
                warp.description
                    ?.takeIf { it.isNotBlank() }
                    ?.let { lore += miniMessage.deserialize(it) }
                lore += miniMessage.deserialize("<yellow>Click to teleport</yellow>")
                meta.lore(lore)
            }
        }

    private fun fillerItem(): ItemStack =
        ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
            editMeta { meta -> meta.displayName(Component.text(" ")) }
        }

    private fun navItem(material: Material, name: String, vararg lore: String): ItemStack =
        ItemStack(material).apply {
            editMeta { meta ->
                meta.displayName(miniMessage.deserialize(name))
                meta.lore(lore.map(miniMessage::deserialize))
            }
        }
}
