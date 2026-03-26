package me.dragan.foxcore.report

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.gui.GuiScreen
import me.dragan.foxcore.gui.GuiSession
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType

class ReportCenterScreen(
    private val plugin: FoxCorePlugin,
) : GuiScreen {
    override val size: Int = 27

    override fun title(): Component = plugin.messages.text("report.gui.center-title")

    override fun render(session: GuiSession) {
        val inventory = session.inventory
        inventory.clear()
        for (slot in 0 until size) {
            inventory.setItem(slot, ReportGuiSupport.filler())
        }

        val viewer = plugin.server.getPlayer(session.viewerId) ?: return
        if (plugin.reports.hasViewAccess(viewer, ReportType.PLAYER)) {
            inventory.setItem(10, entryItem(Material.PLAYER_HEAD, "report.gui.open-player-title", "report.gui.open-player-lore"))
            inventory.setItem(12, entryItem(Material.CHEST, "report.gui.resolved-player-title", "report.gui.resolved-player-lore"))
        }
        if (plugin.reports.hasViewAccess(viewer, ReportType.STAFF)) {
            inventory.setItem(14, entryItem(Material.NETHER_STAR, "report.gui.open-staff-title", "report.gui.open-staff-lore"))
            inventory.setItem(16, entryItem(Material.ENDER_CHEST, "report.gui.resolved-staff-title", "report.gui.resolved-staff-lore"))
        }

        inventory.setItem(22, ReportGuiSupport.item(Material.BARRIER, plugin.messages.text("report.gui.close")))
    }

    override fun handleClick(session: GuiSession, rawSlot: Int, click: ClickType) {
        val viewer = session.viewer() ?: return
        when (rawSlot) {
            10 -> openList(viewer, ReportType.PLAYER, resolved = false)
            12 -> openList(viewer, ReportType.PLAYER, resolved = true)
            14 -> openList(viewer, ReportType.STAFF, resolved = false)
            16 -> openList(viewer, ReportType.STAFF, resolved = true)
            22 -> {
                ReportUiFeedback.close(viewer)
                viewer.closeInventory()
            }
        }
    }

    private fun openList(viewer: org.bukkit.entity.Player, type: ReportType, resolved: Boolean) {
        if (!plugin.reports.hasViewAccess(viewer, type)) {
            ReportUiFeedback.error(viewer)
            viewer.sendMessage(plugin.messages.text("error.no-permission"))
            return
        }

        ReportUiFeedback.open(viewer)
        viewer.sendMessage(plugin.messages.text("report.loading"))
        plugin.reports.loadTargetSummaries(type, resolved) { summaries ->
            if (!viewer.isOnline) {
                return@loadTargetSummaries
            }
            plugin.guiManager.open(viewer, ReportTargetListScreen(plugin, type, resolved, summaries))
        }
    }

    private fun entryItem(material: Material, titlePath: String, lorePath: String) =
        ReportGuiSupport.item(
            material = material,
            name = plugin.messages.text(titlePath),
            lore = listOf(plugin.messages.text(lorePath)),
        )
}
