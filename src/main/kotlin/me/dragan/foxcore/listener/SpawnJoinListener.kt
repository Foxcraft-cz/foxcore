package me.dragan.foxcore.listener

import me.dragan.foxcore.FoxCraftPlugin
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class SpawnJoinListener(
    private val plugin: FoxCraftPlugin,
) : Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        val shouldTeleport = when {
            !plugin.spawnService.isEnabled() -> false
            !player.hasPlayedBefore() && plugin.spawnService.shouldTeleportOnFirstJoin() -> true
            plugin.spawnService.shouldTeleportOnJoin() -> true
            else -> false
        }

        if (!shouldTeleport) {
            return
        }

        val spawn = plugin.spawnService.getSpawn() ?: return
        plugin.server.scheduler.runTask(plugin, Runnable {
            plugin.safeTeleports.teleport(player, spawn)
        })
    }
}

