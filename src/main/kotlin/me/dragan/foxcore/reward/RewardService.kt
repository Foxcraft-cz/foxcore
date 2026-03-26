package me.dragan.foxcore.reward

import me.clip.placeholderapi.PlaceholderAPI
import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.back.storage.FoxCoreStorage
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.bukkit.entity.Player

class RewardService(
    private val plugin: FoxCorePlugin,
    private val storage: FoxCoreStorage,
) {
    private val miniMessage = MiniMessage.miniMessage()
    private val plainText = PlainTextComponentSerializer.plainText()
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "FoxCore-RewardStorage").apply { isDaemon = true }
    }
    private val registry = RewardTrackRegistry(plugin)
    private val cache = ConcurrentHashMap<UUID, RewardPlayerStorageSnapshot>()

    @Volatile
    private var settings = RewardSettings()

    fun reload() {
        registry.reload()
        settings = RewardSettings(
            enabled = plugin.config.getBoolean("rewards.enabled", true),
            joinNotificationsEnabled = plugin.config.getBoolean("rewards.join-notifications.enabled", true),
            joinNotificationDelayTicks = plugin.config.getLong("rewards.join-notifications.delay-ticks", 40L).coerceAtLeast(0L),
        )
        cache.clear()
    }

    fun shutdown() {
        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS)
    }

    fun trackIdsFor(player: Player): List<String> =
        registry.allTracks()
            .filter { canView(player, it) }
            .map(RewardTrack::id)

    fun openHub(player: Player, callback: (RewardHubLoadResult) -> Unit) {
        if (!settings.enabled) {
            callback(RewardHubLoadResult.Disabled)
            return
        }

        loadFreshState(player.uniqueId) { snapshot ->
            val visibleTracks = registry.allTracks().filter { canView(player, it) }
            if (visibleTracks.isEmpty()) {
                callback(RewardHubLoadResult.Empty)
                return@loadFreshState
            }

            callback(
                RewardHubLoadResult.Success(
                    visibleTracks.map { buildTrackView(player, it, snapshot) },
                ),
            )
        }
    }

    fun openTrack(player: Player, trackId: String, callback: (RewardTrackLoadResult) -> Unit) {
        if (!settings.enabled) {
            callback(RewardTrackLoadResult.Disabled)
            return
        }

        val track = registry.findTrack(trackId)
        if (track == null) {
            callback(RewardTrackLoadResult.NotFound(trackId))
            return
        }
        if (!canView(player, track)) {
            callback(RewardTrackLoadResult.NoAccess)
            return
        }

        loadFreshState(player.uniqueId) { snapshot ->
            val currentTrack = registry.findTrack(track.id)
            if (currentTrack == null) {
                callback(RewardTrackLoadResult.NotFound(track.id))
                return@loadFreshState
            }

            callback(RewardTrackLoadResult.Success(buildTrackView(player, currentTrack, snapshot)))
        }
    }

    fun claimReward(
        player: Player,
        trackId: String,
        rewardId: String,
        callback: (RewardClaimResult) -> Unit,
    ) {
        if (!settings.enabled) {
            callback(RewardClaimResult.Disabled)
            return
        }

        val track = registry.findTrack(trackId)
        if (track == null) {
            callback(RewardClaimResult.TrackNotFound(trackId))
            return
        }
        if (!canView(player, track)) {
            callback(RewardClaimResult.NoAccess)
            return
        }

        loadFreshState(player.uniqueId) { snapshot ->
            val currentTrack = registry.findTrack(track.id)
            if (currentTrack == null) {
                callback(RewardClaimResult.TrackNotFound(track.id))
                return@loadFreshState
            }

            val trackView = buildTrackView(player, currentTrack, snapshot)
            val rewardView = trackView.rewardViews.firstOrNull { it.reward.id.equals(rewardId, ignoreCase = true) }
            if (rewardView == null) {
                callback(RewardClaimResult.RewardNotFound(rewardId))
                return@loadFreshState
            }
            if (!trackView.canClaim) {
                callback(RewardClaimResult.NoClaimPermission)
                return@loadFreshState
            }

            when (rewardView.status) {
                RewardClaimStatus.CLAIMED -> {
                    callback(RewardClaimResult.AlreadyClaimed)
                    return@loadFreshState
                }

                RewardClaimStatus.LOCKED -> {
                    callback(RewardClaimResult.NotEligible(rewardView.reward.requiredProgress, trackView.progressValue))
                    return@loadFreshState
                }

                RewardClaimStatus.CLAIMABLE -> Unit
            }

            val claim = RewardClaimRecord(
                trackId = currentTrack.id,
                rewardId = rewardView.reward.id,
                cycleKey = trackView.cycleKey,
                claimedAtMillis = System.currentTimeMillis(),
            )

            executor.execute {
                val inserted = storage.claimReward(player.uniqueId, claim)
                val updatedSnapshot = if (inserted) {
                    snapshot.copy(claims = snapshot.claims + claim)
                } else {
                    loadStorageSnapshot(player.uniqueId)
                }
                cache[player.uniqueId] = updatedSnapshot

                plugin.server.scheduler.runTask(plugin, Runnable {
                    if (!inserted) {
                        callback(RewardClaimResult.AlreadyClaimed)
                        return@Runnable
                    }

                    executeRewardCommands(player, currentTrack, rewardView.reward, trackView.progressValue)
                    callback(
                        RewardClaimResult.Success(
                            buildTrackView(player, currentTrack, updatedSnapshot),
                            rewardView.reward,
                        ),
                    )
                })
            }
        }
    }

    fun notifyJoin(player: Player) {
        if (!settings.enabled || !settings.joinNotificationsEnabled) {
            return
        }

        plugin.server.scheduler.runTaskLater(
            plugin,
            Runnable {
                if (!player.isOnline) {
                    return@Runnable
                }

                openHub(player) { result ->
                    val success = result as? RewardHubLoadResult.Success ?: return@openHub
                    success.tracks
                        .filter { it.claimableCount > 0 }
                        .forEach { trackView ->
                            player.sendMessage(
                                plugin.messages.text(
                                    "reward.join-available",
                                    "count" to trackView.claimableCount.toString(),
                                    "track" to plainTitle(trackView.track.title),
                                )
                                    .clickEvent(ClickEvent.runCommand("/rewards ${trackView.track.id}"))
                                    .hoverEvent(HoverEvent.showText(plugin.messages.text("reward.join-available-hover"))),
                            )
                        }
                }
            },
            settings.joinNotificationDelayTicks,
        )
    }

    private fun loadFreshState(playerId: UUID, callback: (RewardPlayerStorageSnapshot) -> Unit) {
        executor.execute {
            val baseSnapshot = cache[playerId] ?: loadStorageSnapshot(playerId)
            val refreshedSnapshot = refreshDailyState(playerId, baseSnapshot)
            cache[playerId] = refreshedSnapshot
            plugin.server.scheduler.runTask(plugin, Runnable { callback(refreshedSnapshot) })
        }
    }

    private fun loadStorageSnapshot(playerId: UUID): RewardPlayerStorageSnapshot =
        RewardPlayerStorageSnapshot(
            claims = storage.loadRewardClaims(playerId),
            dailyStates = storage.loadRewardDailyStates(playerId).associateBy(RewardDailyStateRecord::trackId),
        )

    private fun refreshDailyState(
        playerId: UUID,
        snapshot: RewardPlayerStorageSnapshot,
    ): RewardPlayerStorageSnapshot {
        var changed = false
        val updatedStates = snapshot.dailyStates.toMutableMap()

        registry.allTracks()
            .filter { it.progress is DailyStreakRewardProgressDefinition }
            .forEach { track ->
                val definition = track.progress as DailyStreakRewardProgressDefinition
                val zoneId = parseZoneId(track.id, definition.timezone)
                val today = LocalDate.now(zoneId)
                val current = updatedStates[track.id]
                val refreshed = when {
                    current == null -> RewardDailyStateRecord(
                        trackId = track.id,
                        streak = 1,
                        lastJoinDate = today.toString(),
                        cycle = 1,
                    )

                    current.lastJoinDate == today.toString() -> current

                    current.lastJoinDate == today.minusDays(1).toString() -> current.copy(
                        streak = current.streak + 1,
                        lastJoinDate = today.toString(),
                    )

                    else -> current.copy(
                        streak = 1,
                        lastJoinDate = today.toString(),
                        cycle = current.cycle + 1,
                    )
                }

                if (refreshed != current) {
                    storage.saveRewardDailyState(playerId, refreshed)
                    updatedStates[track.id] = refreshed
                    changed = true
                }
            }

        return if (changed) snapshot.copy(dailyStates = updatedStates.toMap()) else snapshot
    }

    private fun buildTrackView(
        player: Player,
        track: RewardTrack,
        snapshot: RewardPlayerStorageSnapshot,
    ): RewardTrackView {
        val progress = resolveProgress(player, track, snapshot)
        val canClaim = hasPermission(player, track.claimPermission)
        val claimedKeys = snapshot.claims
            .filter { it.trackId == track.id && it.cycleKey == progress.cycleKey }
            .map { it.rewardId }
            .toSet()
        val rewardViews = track.rewards.map { reward ->
            val status = when {
                claimedKeys.contains(reward.id) -> RewardClaimStatus.CLAIMED
                canClaim && progress.value >= reward.requiredProgress -> RewardClaimStatus.CLAIMABLE
                else -> RewardClaimStatus.LOCKED
            }
            RewardEntryView(reward, status)
        }

        return RewardTrackView(
            track = track,
            progressValue = progress.value,
            cycleKey = progress.cycleKey,
            canClaim = canClaim,
            claimableCount = rewardViews.count { it.status == RewardClaimStatus.CLAIMABLE },
            rewardViews = rewardViews,
        )
    }

    private fun resolveProgress(
        player: Player,
        track: RewardTrack,
        snapshot: RewardPlayerStorageSnapshot,
    ): RewardProgressSnapshot =
        when (val definition = track.progress) {
            is PlaceholderRewardProgressDefinition -> {
                val value = resolveNumericPlaceholder(player, definition.valuePlaceholder)
                val cycleKey = definition.cyclePlaceholder
                    ?.let { resolveStringPlaceholder(player, it) }
                    ?.takeIf(::isResolvedValue)
                    ?: definition.cycleKey
                RewardProgressSnapshot(value = value.coerceAtLeast(0), cycleKey = cycleKey)
            }

            is DailyStreakRewardProgressDefinition -> {
                val state = snapshot.dailyStates[track.id]
                RewardProgressSnapshot(
                    value = state?.streak ?: 0,
                    cycleKey = "daily:${state?.cycle ?: 0}",
                )
            }
        }

    private fun resolveNumericPlaceholder(player: Player, placeholder: String): Int {
        val resolved = resolveStringPlaceholder(player, placeholder)
        if (!isResolvedValue(resolved)) {
            return 0
        }

        val normalized = resolved.replace(",", "").replace(" ", "")
        return normalized.toIntOrNull()
            ?: NUMBER_REGEX.find(normalized)?.value?.toIntOrNull()
            ?: 0
    }

    private fun resolveStringPlaceholder(player: Player, input: String): String {
        if (plugin.server.pluginManager.getPlugin("PlaceholderAPI") == null) {
            return input.trim()
        }

        return PlaceholderAPI.setPlaceholders(player, input).trim()
    }

    private fun isResolvedValue(value: String): Boolean =
        value.isNotBlank() && !value.contains('%')

    private fun executeRewardCommands(
        player: Player,
        track: RewardTrack,
        reward: RewardEntry,
        progressValue: Int,
    ) {
        reward.commands.forEach { commandTemplate ->
            val command = applyCommandPlaceholders(player, track, reward, progressValue, commandTemplate)
                .removePrefix("/")
                .trim()
            if (command.isBlank()) {
                return@forEach
            }

            val executed = plugin.server.dispatchCommand(plugin.server.consoleSender, command)
            if (!executed) {
                plugin.logger.warning("Reward command failed for track '${track.id}' reward '${reward.id}': $command")
            }
        }
    }

    private fun applyCommandPlaceholders(
        player: Player,
        track: RewardTrack,
        reward: RewardEntry,
        progressValue: Int,
        template: String,
    ): String {
        var resolved = template
            .replace("%player%", player.name)
            .replace("%player_uuid%", player.uniqueId.toString())
            .replace("%track_id%", track.id)
            .replace("%reward_id%", reward.id)
            .replace("%progress%", progressValue.toString())

        if (plugin.server.pluginManager.getPlugin("PlaceholderAPI") != null) {
            resolved = PlaceholderAPI.setPlaceholders(player, resolved)
        }

        return resolved
    }

    private fun canView(player: Player, track: RewardTrack): Boolean =
        hasPermission(player, track.viewPermission)

    private fun hasPermission(player: Player, permission: String): Boolean =
        permission.isBlank() || player.hasPermission(permission)

    private fun parseZoneId(trackId: String, raw: String): ZoneId =
        runCatching { ZoneId.of(raw) }
            .onFailure { plugin.logger.warning("Invalid reward timezone '$raw' for track '$trackId'. Falling back to UTC.") }
            .getOrDefault(ZoneId.of("UTC"))

    private fun plainTitle(title: String): String =
        plainText.serialize(miniMessage.deserialize(title))

    companion object {
        private val NUMBER_REGEX = Regex("-?\\d+")
    }
}

sealed interface RewardHubLoadResult {
    data object Disabled : RewardHubLoadResult
    data object Empty : RewardHubLoadResult
    data class Success(val tracks: List<RewardTrackView>) : RewardHubLoadResult
}

sealed interface RewardTrackLoadResult {
    data object Disabled : RewardTrackLoadResult
    data object NoAccess : RewardTrackLoadResult
    data class NotFound(val trackId: String) : RewardTrackLoadResult
    data class Success(val track: RewardTrackView) : RewardTrackLoadResult
}

sealed interface RewardClaimResult {
    data object Disabled : RewardClaimResult
    data object NoAccess : RewardClaimResult
    data object NoClaimPermission : RewardClaimResult
    data object AlreadyClaimed : RewardClaimResult
    data class TrackNotFound(val trackId: String) : RewardClaimResult
    data class RewardNotFound(val rewardId: String) : RewardClaimResult
    data class NotEligible(val required: Int, val current: Int) : RewardClaimResult
    data class Success(val track: RewardTrackView, val reward: RewardEntry) : RewardClaimResult
}

private data class RewardProgressSnapshot(
    val value: Int,
    val cycleKey: String,
)

private data class RewardSettings(
    val enabled: Boolean = true,
    val joinNotificationsEnabled: Boolean = true,
    val joinNotificationDelayTicks: Long = 40L,
)
