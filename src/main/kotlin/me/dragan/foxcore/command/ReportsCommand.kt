package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.report.ReportCenterScreen
import me.dragan.foxcore.report.ReportDetailScreen
import me.dragan.foxcore.feedback.PlayerFeedback
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class ReportsCommand(
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

        if (!plugin.reports.hasAnyViewAccess(player)) {
            PlayerFeedback.error(player)
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        if (args.isEmpty()) {
            PlayerFeedback.guiOpen(player)
            plugin.guiManager.open(player, ReportCenterScreen(plugin))
            return true
        }

        if (args.size != 1) {
            PlayerFeedback.error(player)
            player.sendMessage(plugin.messages.text("command.reports.usage"))
            return true
        }

        val reportId = args[0].toLongOrNull()
        if (reportId == null || reportId <= 0L) {
            PlayerFeedback.error(player)
            player.sendMessage(plugin.messages.text("command.reports.usage"))
            return true
        }

        PlayerFeedback.guiOpen(player)
        player.sendMessage(plugin.messages.text("report.loading"))
        plugin.reports.loadReportDetail(reportId) { detail ->
            if (!player.isOnline) {
                return@loadReportDetail
            }

            if (detail == null) {
                PlayerFeedback.error(player)
                player.sendMessage(plugin.messages.text("report.not-found"))
                return@loadReportDetail
            }

            if (!plugin.reports.hasViewAccess(player, detail.type)) {
                PlayerFeedback.error(player)
                player.sendMessage(plugin.messages.text("error.no-permission"))
                return@loadReportDetail
            }

            plugin.guiManager.open(player, ReportDetailScreen(plugin, detail, ReportCenterScreen(plugin)))
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> = emptyList()
}
