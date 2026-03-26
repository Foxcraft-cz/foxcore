package me.dragan.foxcore.report

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.gui.GuiScreen
import me.dragan.foxcore.gui.GuiSession
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType

class ReportTargetListScreen(
    private val plugin: FoxCorePlugin,
    private val type: ReportType,
    private val resolved: Boolean,
    private val summaries: List<ReportTargetSummary>,
) : GuiScreen {
    override val size: Int = 54

    private val pageSize = 45

    override fun title(): Component =
        plugin.messages.text(
            if (resolved) "report.gui.target-list-title-resolved" else "report.gui.target-list-title-open",
            "type" to ReportText.type(plugin, type),
        )

    override fun render(session: GuiSession) {
        val inventory = session.inventory
        inventory.clear()

        val page = currentPage(session)
        val pageCount = totalPages()
        val entries = summaries.drop(page * pageSize).take(pageSize)

        entries.forEachIndexed { index, summary ->
            inventory.setItem(index, summaryItem(summary))
        }
        for (slot in 45 until size) {
            inventory.setItem(slot, ReportGuiSupport.filler())
        }

        if (page > 0) {
            inventory.setItem(45, ReportGuiSupport.item(Material.ARROW, plugin.messages.text("report.gui.previous-page")))
        }
        inventory.setItem(
            49,
            ReportGuiSupport.item(
                Material.BOOK,
                plugin.messages.text("report.gui.target-summary"),
                listOf(
                    plugin.messages.text("report.gui.type-line", "type" to ReportText.type(plugin, type)),
                    plugin.messages.text("report.gui.count-line", "count" to summaries.size.toString()),
                ),
            ),
        )
        inventory.setItem(48, ReportGuiSupport.item(Material.ARROW, plugin.messages.text("report.gui.back")))
        inventory.setItem(50, ReportGuiSupport.item(Material.BARRIER, plugin.messages.text("report.gui.close")))
        if (page + 1 < pageCount) {
            inventory.setItem(53, ReportGuiSupport.item(Material.ARROW, plugin.messages.text("report.gui.next-page")))
        }

        if (summaries.isEmpty()) {
            inventory.setItem(
                22,
                ReportGuiSupport.item(
                    Material.PAPER,
                    plugin.messages.text("report.gui.empty-title"),
                    listOf(plugin.messages.text("report.gui.empty-lore")),
                ),
            )
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
                plugin.guiManager.open(viewer, ReportCenterScreen(plugin))
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
                val target = summaries.getOrNull(currentPage(session) * pageSize + rawSlot) ?: return
                ReportUiFeedback.open(viewer)
                viewer.sendMessage(plugin.messages.text("report.loading"))
                plugin.reports.loadReportsForTarget(type, resolved, target.targetId) { reports ->
                    if (!viewer.isOnline) {
                        return@loadReportsForTarget
                    }
                    plugin.guiManager.open(viewer, ReportThreadListScreen(plugin, type, resolved, summaries, target, reports))
                }
            }
        }
    }

    private fun currentPage(session: GuiSession): Int =
        (session.state["page"] as? Int ?: 0).coerceIn(0, totalPages() - 1)

    private fun totalPages(): Int =
        summaries.size.coerceAtLeast(1).let { ((it - 1) / pageSize) + 1 }

    private fun summaryItem(summary: ReportTargetSummary) =
        ReportGuiSupport.playerHead(
            plugin = plugin,
            playerId = summary.targetId,
            name = summary.targetName,
            lore = listOf(
                plugin.messages.text("report.gui.count-line", "count" to summary.count.toString()),
                plugin.messages.text("report.gui.latest-line", "date" to ReportGuiSupport.formatTimestamp(summary.latestCreatedAtMillis)),
                plugin.messages.text("report.gui.reason-line", "reason" to summary.latestReason),
                plugin.messages.text("report.gui.open-thread"),
            ),
        )
}
