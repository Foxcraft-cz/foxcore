package me.dragan.foxcore.report

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.gui.GuiScreen
import me.dragan.foxcore.gui.GuiSession
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType

class ReportThreadListScreen(
    private val plugin: FoxCorePlugin,
    private val type: ReportType,
    private val resolved: Boolean,
    private val previousSummaries: List<ReportTargetSummary>,
    private val target: ReportTargetSummary,
    private val reports: List<ReportListEntry>,
) : GuiScreen {
    override val size: Int = 54

    private val pageSize = 45

    override fun title(): Component = plugin.messages.text("report.gui.thread-title", "player" to target.targetName)

    override fun render(session: GuiSession) {
        val inventory = session.inventory
        inventory.clear()

        val page = currentPage(session)
        val pageCount = totalPages()
        val entries = reports.drop(page * pageSize).take(pageSize)

        entries.forEachIndexed { index, report ->
            inventory.setItem(index, reportItem(report))
        }
        for (slot in 45 until size) {
            inventory.setItem(slot, ReportGuiSupport.filler())
        }

        if (page > 0) {
            inventory.setItem(45, ReportGuiSupport.item(Material.ARROW, plugin.messages.text("report.gui.previous-page")))
        }
        inventory.setItem(
            49,
            ReportGuiSupport.playerHead(
                plugin,
                target.targetId,
                target.targetName,
                listOf(
                    plugin.messages.text("report.gui.type-line", "type" to ReportText.type(plugin, type)),
                    plugin.messages.text("report.gui.count-line", "count" to reports.size.toString()),
                ),
            ),
        )
        inventory.setItem(48, ReportGuiSupport.item(Material.ARROW, plugin.messages.text("report.gui.back")))
        inventory.setItem(50, ReportGuiSupport.item(Material.BARRIER, plugin.messages.text("report.gui.close")))
        if (page + 1 < pageCount) {
            inventory.setItem(53, ReportGuiSupport.item(Material.ARROW, plugin.messages.text("report.gui.next-page")))
        }
    }

    override fun handleClick(session: GuiSession, rawSlot: Int, click: ClickType) {
        val viewer = session.viewer() ?: return
        when (rawSlot) {
            45 -> if (currentPage(session) > 0) {
                ReportUiFeedback.navigation(viewer)
                session.state["page"] = currentPage(session) - 1
                render(session)
            }

            48 -> {
                ReportUiFeedback.navigation(viewer)
                plugin.guiManager.open(viewer, ReportTargetListScreen(plugin, type, resolved, previousSummaries))
            }
            50 -> {
                ReportUiFeedback.close(viewer)
                viewer.closeInventory()
            }
            53 -> if (currentPage(session) + 1 < totalPages()) {
                ReportUiFeedback.navigation(viewer)
                session.state["page"] = currentPage(session) + 1
                render(session)
            }

            in 0 until pageSize -> {
                val report = reports.getOrNull(currentPage(session) * pageSize + rawSlot) ?: return
                ReportUiFeedback.open(viewer)
                viewer.sendMessage(plugin.messages.text("report.loading"))
                plugin.reports.loadReportDetail(report.id) { detail ->
                    if (!viewer.isOnline) {
                        return@loadReportDetail
                    }
                    if (detail == null) {
                        viewer.sendMessage(plugin.messages.text("report.not-found"))
                        return@loadReportDetail
                    }
                    plugin.guiManager.open(viewer, ReportDetailScreen(plugin, detail, this))
                }
            }
        }
    }

    private fun currentPage(session: GuiSession): Int =
        (session.state["page"] as? Int ?: 0).coerceIn(0, totalPages() - 1)

    private fun totalPages(): Int =
        reports.size.coerceAtLeast(1).let { ((it - 1) / pageSize) + 1 }

    private fun reportItem(report: ReportListEntry) =
        ReportGuiSupport.item(
            material = when (report.status) {
                ReportStatus.OPEN -> Material.PAPER
                ReportStatus.CONFIRMED -> Material.LIME_DYE
                ReportStatus.REJECTED -> Material.RED_DYE
            },
            name = plugin.messages.text("report.gui.report-entry-title", "id" to report.id.toString()),
            lore = buildList {
                add(plugin.messages.text("report.gui.reporter-line", "player" to report.reporterName))
                add(plugin.messages.text("report.gui.status-line", "status" to ReportText.status(plugin, report.status)))
                add(plugin.messages.text("report.gui.latest-line", "date" to ReportGuiSupport.formatTimestamp(report.createdAtMillis)))
                add(plugin.messages.text("report.gui.reason-line", "reason" to report.reason))
                report.resolverName?.let { add(plugin.messages.text("report.gui.resolver-line", "player" to it)) }
                add(plugin.messages.text("report.gui.open-detail"))
            },
        )
}
