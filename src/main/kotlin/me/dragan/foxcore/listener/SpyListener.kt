package me.dragan.foxcore.listener

import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerQuitEvent

class SpyListener(
    private val plugin: FoxCorePlugin,
) : Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onCommand(event: PlayerCommandPreprocessEvent) {
        plugin.spies.notifyCommandSpies(event.player, event.message)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        plugin.spies.clear(event.player.uniqueId)
    }
}
