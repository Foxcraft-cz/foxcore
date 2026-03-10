package me.dragan.foxcore.listener

import me.dragan.foxcore.FoxCraftPlugin
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent

class TpaRequestCleanupListener(
    private val plugin: FoxCraftPlugin,
) : Listener {
    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        plugin.tpaRequests.removeAllFor(event.player.uniqueId)
    }

    @EventHandler
    fun onKick(event: PlayerKickEvent) {
        plugin.tpaRequests.removeAllFor(event.player.uniqueId)
    }
}

