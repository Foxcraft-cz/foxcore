package me.dragan.foxcore.report

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.gui.GuiScreen
import me.dragan.foxcore.gui.GuiSession
import me.dragan.foxcore.teleport.SafeTeleportResult
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType

class ReportDetailScreen(
    private val plugin: FoxCorePlugin,
    private val detail: ReportDetail,
    private val previousScreen: GuiScreen,
) : GuiScreen {
    override val size: Int = 54

    override fun title(): Component = plugin.messages.text("report.gui.detail-title", "id" to detail.id.toString())

    override fun render(session: GuiSession) {
        val inventory = session.inventory
        inventory.clear()
        for (slot in 0 until size) {
            inventory.setItem(slot, ReportGuiSupport.filler())
        }

        inventory.setItem(10, playerCard(detail.reportedId, detail.reportedName, detail.reportedLocation, detail.reportedGameMode, "report.gui.reported-title"))
        inventory.setItem(12, playerCard(detail.reporterId, detail.reporterName, detail.reporterLocation, detail.reporterGameMode, "report.gui.reporter-title"))
        inventory.setItem(
            14,
            ReportGuiSupport.item(
                Material.BOOK,
                plugin.messages.text("report.gui.info-title"),
                buildList {
                    add(plugin.messages.text("report.gui.type-line", "type" to ReportText.type(plugin, detail.type)))
                    add(plugin.messages.text("report.gui.status-line", "status" to ReportText.status(plugin, detail.status)))
                    add(plugin.messages.text("report.gui.latest-line", "date" to ReportGuiSupport.formatTimestamp(detail.createdAtMillis)))
                    add(plugin.messages.text("report.gui.reason-line", "reason" to detail.reason))
                    detail.resolverName?.let { add(plugin.messages.text("report.gui.resolver-line", "player" to it)) }
                    detail.resolvedAtMillis?.let {
                        add(plugin.messages.text("report.gui.resolved-date-line", "date" to ReportGuiSupport.formatTimestamp(it)))
                    }
                },
            ),
        )
        inventory.setItem(19, ReportGuiSupport.item(Material.ENDER_PEARL, plugin.messages.text("report.gui.teleport-reported")))
        inventory.setItem(21, ReportGuiSupport.item(Material.ENDER_EYE, plugin.messages.text("report.gui.teleport-reporter")))
        inventory.setItem(28, activityItem(ReportActivityType.CHAT))
        inventory.setItem(30, activityItem(ReportActivityType.COMMAND))
        inventory.setItem(48, ReportGuiSupport.item(Material.ARROW, plugin.messages.text("report.gui.back")))
        inventory.setItem(49, ReportGuiSupport.item(Material.BARRIER, plugin.messages.text("report.gui.close")))

        if (detail.status == ReportStatus.OPEN) {
            inventory.setItem(23, ReportGuiSupport.item(Material.LIME_WOOL, plugin.messages.text("report.gui.confirm")))
            inventory.setItem(24, ReportGuiSupport.item(Material.RED_WOOL, plugin.messages.text("report.gui.reject")))
        }
    }

    override fun handleClick(session: GuiSession, rawSlot: Int, click: ClickType) {
        val viewer = session.viewer() ?: return
        when (rawSlot) {
            19 -> teleport(viewer, detail.reportedLocation)
            21 -> teleport(viewer, detail.reporterLocation)
            23 -> resolve(viewer, ReportStatus.CONFIRMED)
            24 -> resolve(viewer, ReportStatus.REJECTED)
            48 -> {
                ReportUiFeedback.navigation(viewer)
                plugin.guiManager.open(viewer, previousScreen)
            }
            49 -> {
                ReportUiFeedback.close(viewer)
                viewer.closeInventory()
            }
        }
    }

    private fun resolve(viewer: org.bukkit.entity.Player, newStatus: ReportStatus) {
        if (!plugin.reports.hasResolveAccess(viewer, detail.type) || viewer.uniqueId.toString() == detail.reportedId) {
            ReportUiFeedback.error(viewer)
            viewer.sendMessage(plugin.messages.text("error.no-permission"))
            return
        }
        if (detail.status != ReportStatus.OPEN) {
            ReportUiFeedback.error(viewer)
            viewer.sendMessage(plugin.messages.text("report.already-resolved"))
            return
        }

        ReportUiFeedback.navigation(viewer)
        plugin.reports.resolveReport(viewer, detail, newStatus) { result ->
            when (result) {
                ReportResolveResult.NotFound -> {
                    ReportUiFeedback.error(viewer)
                    viewer.sendMessage(plugin.messages.text("report.not-found"))
                }
                ReportResolveResult.AlreadyResolved -> {
                    ReportUiFeedback.error(viewer)
                    viewer.sendMessage(plugin.messages.text("report.already-resolved"))
                }
                ReportResolveResult.Success -> {
                    ReportUiFeedback.success(viewer)
                    viewer.sendMessage(plugin.messages.text("report.resolved", "status" to ReportText.status(plugin, newStatus)))
                    plugin.reports.loadReportDetail(detail.id) { refreshed ->
                        if (!viewer.isOnline || refreshed == null) {
                            return@loadReportDetail
                        }
                        plugin.guiManager.open(viewer, ReportDetailScreen(plugin, refreshed, previousScreen))
                    }
                }
            }
        }
    }

    private fun teleport(viewer: org.bukkit.entity.Player, snapshot: ReportLocationSnapshot) {
        if (!plugin.reports.hasTeleportAccess(viewer, detail.type)) {
            ReportUiFeedback.error(viewer)
            viewer.sendMessage(plugin.messages.text("error.no-permission"))
            return
        }

        val world = plugin.server.getWorld(snapshot.worldName)
        if (world == null) {
            ReportUiFeedback.error(viewer)
            viewer.sendMessage(plugin.messages.text("report.missing-world", "world" to snapshot.worldName))
            return
        }

        when (plugin.safeTeleports.teleport(viewer, org.bukkit.Location(world, snapshot.x, snapshot.y, snapshot.z, snapshot.yaw, snapshot.pitch))) {
            SafeTeleportResult.SUCCESS -> {
                ReportUiFeedback.teleport(viewer)
                viewer.sendMessage(plugin.messages.text("report.teleport-success"))
            }
            SafeTeleportResult.NO_SAFE_GROUND -> {
                ReportUiFeedback.error(viewer)
                viewer.sendMessage(plugin.messages.text("error.no-safe-ground"))
            }
            SafeTeleportResult.FAILED -> {
                ReportUiFeedback.error(viewer)
                viewer.sendMessage(plugin.messages.text("error.teleport-failed"))
            }
        }
    }

    private fun playerCard(
        playerId: String,
        name: String,
        location: ReportLocationSnapshot,
        gameMode: String,
        titlePath: String,
    ) = ReportGuiSupport.playerHead(
        plugin = plugin,
        playerId = playerId,
        name = name,
        lore = listOf(
            plugin.messages.text(titlePath),
            plugin.messages.text("report.gui.location-line", "location" to "${location.worldName} ${location.x.toInt()}, ${location.y.toInt()}, ${location.z.toInt()}"),
            plugin.messages.text("report.gui.gamemode-line", "gamemode" to gameMode.lowercase()),
        ),
    )

    private fun activityItem(type: ReportActivityType) =
        ReportGuiSupport.item(
            material = if (type == ReportActivityType.CHAT) Material.WRITABLE_BOOK else Material.COMMAND_BLOCK,
            name = plugin.messages.text("report.gui.activity.${type.name.lowercase()}-title"),
            lore = detail.activities
                .filter { it.type == type }
                .takeLast(8)
                .map { plugin.messages.text("report.gui.activity-line", "entry" to it.content, "time" to ReportGuiSupport.formatTimestamp(it.createdAtMillis)) }
                .ifEmpty { listOf(plugin.messages.text("report.gui.activity-empty")) },
        )
}
