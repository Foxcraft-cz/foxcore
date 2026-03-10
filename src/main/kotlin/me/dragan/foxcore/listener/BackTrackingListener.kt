package me.dragan.foxcore.listener

import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent

class BackTrackingListener(
    private val plugin: FoxCorePlugin,
) : Listener {
    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        plugin.backService.loadPlayer(event.player.uniqueId)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onTeleport(event: PlayerTeleportEvent) {
        if (event.from == event.to) {
            return
        }

        plugin.backService.recordTeleportOrigin(event.player, event.from)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onDeath(event: PlayerDeathEvent) {
        plugin.backService.recordDeath(event.player, event.player.location)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onQuit(event: PlayerQuitEvent) {
        plugin.backService.recordDisconnectLocation(event.player)
    }
}
