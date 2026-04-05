package me.dragan.foxcore.report

import me.dragan.foxcore.FoxCorePlugin

internal object ReportText {
    fun type(plugin: FoxCorePlugin, reportType: ReportType): String =
        when (locale(plugin)) {
            "cs" -> when (reportType) {
                ReportType.PLAYER -> "hráčský"
                ReportType.STAFF -> "staff"
            }

            else -> reportType.name.lowercase()
        }

    fun queue(plugin: FoxCorePlugin, reportType: ReportType): String =
        when (locale(plugin)) {
            "cs" -> when (reportType) {
                ReportType.PLAYER -> "hráči"
                ReportType.STAFF -> "staff"
            }

            else -> when (reportType) {
                ReportType.PLAYER -> "players"
                ReportType.STAFF -> "staff"
            }
        }

    fun status(plugin: FoxCorePlugin, reportStatus: ReportStatus): String =
        when (locale(plugin)) {
            "cs" -> when (reportStatus) {
                ReportStatus.OPEN -> "otevřený"
                ReportStatus.CONFIRMED -> "potvrzený"
                ReportStatus.REJECTED -> "zamítnutý"
            }

            else -> reportStatus.name.lowercase()
        }

    private fun locale(plugin: FoxCorePlugin): String =
        plugin.config.getString("translations.locale", "en").orEmpty().ifBlank { "en" }.lowercase()
}
