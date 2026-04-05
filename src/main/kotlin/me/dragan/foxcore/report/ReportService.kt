package me.dragan.foxcore.report

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.back.storage.FoxCoreStorage
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ReportService(
    private val plugin: FoxCorePlugin,
    private val storage: FoxCoreStorage,
    private val activityTracker: ReportActivityTracker,
) {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "FoxCore-ReportStorage").apply { isDaemon = true }
    }
    private val lastReportAt = ConcurrentHashMap<UUID, Long>()
    private val discordNotifier = ReportDiscordNotifier(plugin)

    @Volatile
    private var settings = ReportSettings()

    fun reload() {
        settings = ReportSettings(
            enabled = plugin.config.getBoolean("reports.enabled", true),
            cooldownMillis = plugin.config.getLong("reports.cooldown-seconds", 60L).coerceAtLeast(0L) * 1000L,
            minReasonLength = plugin.config.getInt("reports.min-reason-length", 5).coerceAtLeast(1),
            maxReasonLength = plugin.config.getInt("reports.max-reason-length", 250).coerceAtLeast(1),
            duplicateWindowMillis = plugin.config.getLong("reports.duplicate-window-seconds", 300L).coerceAtLeast(0L) * 1000L,
            staffTargetPermission = plugin.config.getString("reports.staff-target-permission", "foxcore.report.staff-target")
                .orEmpty()
                .ifBlank { "foxcore.report.staff-target" },
            notificationsChatEnabled = plugin.config.getBoolean("reports.notifications.chat.enabled", true),
            notificationsSoundEnabled = plugin.config.getBoolean("reports.notifications.sound.enabled", true),
            notificationSound = parseSound(plugin.config.getString("reports.notifications.sound.key", "ENTITY_EXPERIENCE_ORB_PICKUP")),
            notificationSoundVolume = plugin.config.getDouble("reports.notifications.sound.volume", 1.0).toFloat().coerceAtLeast(0f),
            notificationSoundPitch = plugin.config.getDouble("reports.notifications.sound.pitch", 1.1).toFloat().coerceAtLeast(0f),
            joinNotificationsEnabled = plugin.config.getBoolean("reports.join-notifications.enabled", true),
            joinNotificationDelayTicks = plugin.config.getLong("reports.join-notifications.delay-ticks", 40L).coerceAtLeast(0L),
        )
        discordNotifier.reload()
    }

    fun hasAnyViewAccess(player: Player): Boolean =
        hasViewAccess(player, ReportType.PLAYER) || hasViewAccess(player, ReportType.STAFF)

    fun hasViewAccess(player: Player, type: ReportType): Boolean =
        player.hasPermission("foxcore.report.view.${type.name.lowercase()}")

    fun hasResolveAccess(player: Player, type: ReportType): Boolean =
        player.hasPermission("foxcore.report.resolve.${type.name.lowercase()}")

    fun hasTeleportAccess(player: Player, type: ReportType): Boolean =
        player.hasPermission("foxcore.report.teleport.${type.name.lowercase()}")

    fun canNotify(player: Player, type: ReportType): Boolean =
        player.hasPermission("foxcore.report.notify.${type.name.lowercase()}")

    fun detectType(target: Player): ReportType =
        if (settings.staffTargetPermission.isNotBlank() && target.hasPermission(settings.staffTargetPermission)) {
            ReportType.STAFF
        } else {
            ReportType.PLAYER
        }

    fun createReport(reporter: Player, reported: Player, rawReason: String, callback: (ReportCreateResult) -> Unit) {
        val currentSettings = settings
        if (!currentSettings.enabled) {
            callback(ReportCreateResult.Disabled)
            return
        }
        if (reporter.uniqueId == reported.uniqueId) {
            callback(ReportCreateResult.SelfReport)
            return
        }

        val reason = sanitizeReason(rawReason)
        if (reason.length < currentSettings.minReasonLength) {
            callback(ReportCreateResult.ReasonTooShort(currentSettings.minReasonLength))
            return
        }
        if (reason.length > currentSettings.maxReasonLength) {
            callback(ReportCreateResult.ReasonTooLong(currentSettings.maxReasonLength))
            return
        }

        val now = System.currentTimeMillis()
        val previous = lastReportAt[reporter.uniqueId]
        if (previous != null && currentSettings.cooldownMillis > 0L) {
            val remaining = currentSettings.cooldownMillis - (now - previous)
            if (remaining > 0L) {
                callback(ReportCreateResult.Cooldown(TimeUnit.MILLISECONDS.toSeconds(remaining).coerceAtLeast(1L)))
                return
            }
        }

        val type = detectType(reported)
        val record = NewReportRecord(
            type = type,
            status = ReportStatus.OPEN,
            reason = reason,
            normalizedReason = normalizeReason(reason),
            createdAtMillis = now,
            reportedId = reported.uniqueId.toString(),
            reportedName = reported.name,
            reportedLocation = reported.location.toSnapshot(),
            reportedOnline = reported.isOnline,
            reportedGameMode = reported.gameMode.name,
            reporterId = reporter.uniqueId.toString(),
            reporterName = reporter.name,
            reporterLocation = reporter.location.toSnapshot(),
            reporterOnline = reporter.isOnline,
            reporterGameMode = reporter.gameMode.name,
            activities = activityTracker.snapshot(reported.uniqueId),
        )

        executor.execute {
            val duplicate = currentSettings.duplicateWindowMillis > 0L &&
                storage.hasRecentOpenDuplicate(
                    reporterId = record.reporterId,
                    reportedId = record.reportedId,
                    normalizedReason = record.normalizedReason,
                    createdAfterMillis = now - currentSettings.duplicateWindowMillis,
                )
            val result = if (duplicate) {
                ReportCreateResult.Duplicate
            } else {
                val reportId = storage.createReport(record)
                lastReportAt[reporter.uniqueId] = now
                ReportCreateResult.Success(reportId, type)
            }

            plugin.server.scheduler.runTask(plugin, Runnable {
                if (result is ReportCreateResult.Success) {
                    notifyTeam(result.reportId, type, reporter, reported, reason)
                }
                callback(result)
            })
        }
    }

    fun notifyJoin(player: Player) {
        val currentSettings = settings
        if (!currentSettings.enabled || !currentSettings.joinNotificationsEnabled) {
            return
        }

        plugin.server.scheduler.runTaskLater(
            plugin,
            Runnable {
                if (!player.isOnline) {
                    return@Runnable
                }

                val accessibleTypes = ReportType.entries.filter { hasViewAccess(player, it) }
                if (accessibleTypes.isEmpty()) {
                    return@Runnable
                }

                executor.execute {
                    val openCounts = linkedMapOf<ReportType, Int>()
                    accessibleTypes.forEach { type ->
                        val count = storage.listReportTargetSummaries(type, resolved = false).sumOf(ReportTargetSummary::count)
                        if (count > 0) {
                            openCounts[type] = count
                        }
                    }

                    val totalCount = openCounts.values.sum()
                    if (totalCount <= 0) {
                        return@execute
                    }

                    plugin.server.scheduler.runTask(
                        plugin,
                        Runnable {
                            if (!player.isOnline) {
                                return@Runnable
                            }
                            sendJoinNotification(player, openCounts, totalCount, currentSettings)
                        },
                    )
                }
            },
            currentSettings.joinNotificationDelayTicks,
        )
    }

    fun loadTargetSummaries(type: ReportType, resolved: Boolean, callback: (List<ReportTargetSummary>) -> Unit) {
        executor.execute {
            val results = storage.listReportTargetSummaries(type, resolved)
            plugin.server.scheduler.runTask(plugin, Runnable { callback(results) })
        }
    }

    fun loadReportsForTarget(
        type: ReportType,
        resolved: Boolean,
        targetId: String,
        callback: (List<ReportListEntry>) -> Unit,
    ) {
        executor.execute {
            val results = storage.listReportsForTarget(type, resolved, targetId)
            plugin.server.scheduler.runTask(plugin, Runnable { callback(results) })
        }
    }

    fun loadReportDetail(id: Long, callback: (ReportDetail?) -> Unit) {
        executor.execute {
            val detail = storage.findReportById(id)
            plugin.server.scheduler.runTask(plugin, Runnable { callback(detail) })
        }
    }

    fun resolveReport(viewer: Player, detail: ReportDetail, status: ReportStatus, callback: (ReportResolveResult) -> Unit) {
        if (status == ReportStatus.OPEN) {
            callback(ReportResolveResult.AlreadyResolved)
            return
        }
        if (!hasResolveAccess(viewer, detail.type) || viewer.uniqueId.toString() == detail.reportedId) {
            callback(ReportResolveResult.AlreadyResolved)
            return
        }

        executor.execute {
            val updated = storage.resolveReport(
                id = detail.id,
                status = status,
                resolverId = viewer.uniqueId.toString(),
                resolverName = viewer.name,
                resolvedAtMillis = System.currentTimeMillis(),
            )
            val result = when {
                updated -> ReportResolveResult.Success
                storage.findReportById(detail.id) == null -> ReportResolveResult.NotFound
                else -> ReportResolveResult.AlreadyResolved
            }
            plugin.server.scheduler.runTask(plugin, Runnable { callback(result) })
        }
    }

    fun shutdown() {
        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS)
    }

    private fun notifyTeam(reportId: Long, type: ReportType, reporter: Player, reported: Player, reason: String) {
        val recipients = plugin.server.onlinePlayers.filter { it.uniqueId != reported.uniqueId && canNotify(it, type) }
        if (settings.notificationsChatEnabled) {
            val component = plugin.messages.text(
                "report.notify.created",
                "id" to reportId.toString(),
                "type" to ReportText.type(plugin, type),
                "reporter" to reporter.name,
                "reported" to reported.name,
                "reason" to reason,
            )
                .clickEvent(ClickEvent.runCommand("/reports $reportId"))
                .hoverEvent(HoverEvent.showText(plugin.messages.text("report.notify.hover")))
            recipients.forEach { it.sendMessage(component) }
        }

        if (settings.notificationsSoundEnabled) {
            val sound = settings.notificationSound
            if (sound != null) {
                recipients.forEach { it.playSound(it.location, sound, settings.notificationSoundVolume, settings.notificationSoundPitch) }
            }
        }

        discordNotifier.sendCreatedNotification(reportId, type, reporter.name, reported.name, reason)
    }

    private fun sendJoinNotification(
        player: Player,
        openCounts: Map<ReportType, Int>,
        totalCount: Int,
        currentSettings: ReportSettings,
    ) {
        val summary = openCounts.entries.joinToString(", ") { (type, count) ->
            "${ReportText.queue(plugin, type)} $count"
        }
        val clickEvent = ClickEvent.runCommand("/reports")
        val hoverEvent = HoverEvent.showText(plugin.messages.text("report.notify.join-hover"))

        player.sendMessage(
            plugin.messages.text(
                "report.notify.join",
                "count" to totalCount.toString(),
                "summary" to summary,
            )
                .clickEvent(clickEvent)
                .hoverEvent(hoverEvent)
                .append(plugin.messages.text("report.notify.join-button").clickEvent(clickEvent).hoverEvent(hoverEvent)),
        )

        if (!currentSettings.notificationsSoundEnabled) {
            return
        }

        val sound = currentSettings.notificationSound ?: return
        player.playSound(player.location, sound, currentSettings.notificationSoundVolume, currentSettings.notificationSoundPitch)
    }

    private fun sanitizeReason(raw: String): String =
        raw.trim()
            .replace(Regex("\\s+"), " ")
            .replace('<', '‹')
            .replace('>', '›')

    private fun normalizeReason(reason: String): String =
        reason.lowercase()

    private fun parseSound(raw: String?): Sound? {
        val normalized = raw.orEmpty().trim()
        if (normalized.isEmpty()) {
            return null
        }

        return runCatching { Sound.valueOf(normalized.uppercase()) }
            .onFailure { plugin.logger.warning("Invalid reports notification sound: $normalized") }
            .getOrNull()
    }

    private fun Location.toSnapshot(): ReportLocationSnapshot =
        ReportLocationSnapshot(
            worldName = world?.name ?: "unknown",
            x = x,
            y = y,
            z = z,
            yaw = yaw,
            pitch = pitch,
        )
}

private data class ReportSettings(
    val enabled: Boolean = true,
    val cooldownMillis: Long = 60_000L,
    val minReasonLength: Int = 5,
    val maxReasonLength: Int = 250,
    val duplicateWindowMillis: Long = 300_000L,
    val staffTargetPermission: String = "foxcore.report.staff-target",
    val notificationsChatEnabled: Boolean = true,
    val notificationsSoundEnabled: Boolean = true,
    val notificationSound: Sound? = Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
    val notificationSoundVolume: Float = 1f,
    val notificationSoundPitch: Float = 1.1f,
    val joinNotificationsEnabled: Boolean = true,
    val joinNotificationDelayTicks: Long = 40L,
)
