package me.dragan.foxcore.afk

import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class AfkService(
    private val plugin: FoxCorePlugin,
) {
    private val states = ConcurrentHashMap<UUID, AfkState>()
    private var checkTask: BukkitTask? = null

    fun start() {
        stop()
        if (!plugin.config.getBoolean("afk.enabled", true)) {
            return
        }

        val intervalTicks = plugin.config.getLong("afk.check-interval-seconds", 5L).coerceAtLeast(1L) * 20L
        val now = System.currentTimeMillis()
        plugin.server.onlinePlayers.forEach { player ->
            states.compute(player.uniqueId) { _, existing ->
                existing?.copy(lastActivityAtMillis = now) ?: AfkState(lastActivityAtMillis = now)
            }
        }

        checkTask = plugin.server.scheduler.runTaskTimer(
            plugin,
            Runnable { tick() },
            intervalTicks,
            intervalTicks,
        )
    }

    fun stop() {
        checkTask?.cancel()
        checkTask = null
        states.clear()
    }

    fun handleJoin(player: Player) {
        states[player.uniqueId] = AfkState(lastActivityAtMillis = System.currentTimeMillis())
    }

    fun handleQuit(player: Player) {
        states.remove(player.uniqueId)
    }

    fun recordActivity(player: Player) {
        if (!plugin.config.getBoolean("afk.enabled", true)) {
            return
        }

        val now = System.currentTimeMillis()
        states.compute(player.uniqueId) { _, current ->
            val state = current ?: AfkState(lastActivityAtMillis = now)
            if (state.isAfk) {
                plugin.broadcastAfkState(player, becameAfk = false)
            }
            state.copy(
                lastActivityAtMillis = now,
                isAfk = false,
                afkSinceAtMillis = null,
                manual = false,
            )
        }
    }

    fun isAfk(player: Player): Boolean =
        states[player.uniqueId]?.isAfk == true

    fun toggleManualAfk(player: Player): Boolean {
        if (!plugin.config.getBoolean("afk.enabled", true)) {
            return false
        }

        val now = System.currentTimeMillis()
        val updated = states.compute(player.uniqueId) { _, current ->
            val state = current ?: AfkState(lastActivityAtMillis = now)
            if (state.manual) {
                plugin.broadcastAfkState(player, becameAfk = false)
                state.copy(
                    lastActivityAtMillis = now,
                    isAfk = false,
                    afkSinceAtMillis = null,
                    manual = false,
                )
            } else if (state.isAfk) {
                state.copy(
                    lastActivityAtMillis = now,
                    isAfk = true,
                    afkSinceAtMillis = state.afkSinceAtMillis ?: now,
                    manual = true,
                )
            } else {
                plugin.broadcastAfkState(player, becameAfk = true)
                state.copy(
                    lastActivityAtMillis = now,
                    isAfk = true,
                    afkSinceAtMillis = now,
                    manual = true,
                )
            }
        } ?: return false

        return updated.isAfk
    }

    fun afkSinceMillis(playerId: UUID): Long? =
        states[playerId]?.afkSinceAtMillis

    fun afkDurationSeconds(playerId: UUID): Long {
        val afkSince = states[playerId]?.afkSinceAtMillis ?: return 0L
        return ((System.currentTimeMillis() - afkSince).coerceAtLeast(0L)) / 1000L
    }

    fun afkDurationHuman(playerId: UUID): String {
        val totalSeconds = afkDurationSeconds(playerId)
        if (totalSeconds <= 0L) {
            return "0s"
        }

        val days = totalSeconds / 86_400
        val hours = (totalSeconds % 86_400) / 3_600
        val minutes = (totalSeconds % 3_600) / 60
        val seconds = totalSeconds % 60
        val parts = mutableListOf<String>()
        if (days > 0) {
            parts += "${days}d"
        }
        if (hours > 0) {
            parts += "${hours}h"
        }
        if (minutes > 0) {
            parts += "${minutes}m"
        }
        if (seconds > 0 || parts.isEmpty()) {
            parts += "${seconds}s"
        }
        return parts.joinToString(" ")
    }

    private fun tick() {
        val now = System.currentTimeMillis()
        val idleMillis = plugin.config.getLong("afk.idle-seconds", 300L).coerceAtLeast(1L) * 1000L
        val kickEnabled = plugin.config.getBoolean("afk.kick.enabled", true)
        val kickMillis = plugin.config.getLong("afk.kick.after-seconds", 1800L).coerceAtLeast(1L) * 1000L

        plugin.server.onlinePlayers.forEach { player ->
            val state = states.computeIfAbsent(player.uniqueId) { AfkState(lastActivityAtMillis = now) }
            val hasFullBypass = hasPermission(player, BYPASS_PERMISSION)
            val hasKickBypass = hasPermission(player, BYPASS_KICK_PERMISSION)
            if (hasFullBypass) {
                if (state.isAfk) {
                    plugin.broadcastAfkState(player, becameAfk = false)
                    states[player.uniqueId] = state.copy(
                        lastActivityAtMillis = now,
                        isAfk = false,
                        afkSinceAtMillis = null,
                        manual = false,
                    )
                } else {
                    states[player.uniqueId] = state.copy(lastActivityAtMillis = now)
                }
                return@forEach
            }
            if (state.manual) {
                if (
                    kickEnabled &&
                    !hasKickBypass &&
                    state.isAfk &&
                    now - (state.afkSinceAtMillis ?: now) >= kickMillis
                ) {
                    player.kick(plugin.messages.text("afk.kick-reason"))
                }
                return@forEach
            }
            if (!state.isAfk && now - state.lastActivityAtMillis >= idleMillis) {
                states[player.uniqueId] = state.copy(
                    isAfk = true,
                    afkSinceAtMillis = now,
                )
                plugin.broadcastAfkState(player, becameAfk = true)
                return@forEach
            }

            if (
                kickEnabled &&
                !hasKickBypass &&
                state.isAfk &&
                now - (state.afkSinceAtMillis ?: now) >= kickMillis
            ) {
                player.kick(plugin.messages.text("afk.kick-reason"))
            }
        }
    }

    companion object {
        const val BYPASS_PERMISSION = "foxcore.afk.bypass"
        const val BYPASS_KICK_PERMISSION = "foxcore.afk.bypass-kick"
    }

    private fun hasPermission(player: Player, permission: String): Boolean =
        player.effectivePermissions.any { info ->
            info.permission.equals(permission, ignoreCase = true) && info.value
        }
}

data class AfkState(
    val lastActivityAtMillis: Long,
    val isAfk: Boolean = false,
    val afkSinceAtMillis: Long? = null,
    val manual: Boolean = false,
)
