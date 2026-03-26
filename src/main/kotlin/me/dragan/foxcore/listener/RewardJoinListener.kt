package me.dragan.foxcore.listener

import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class RewardJoinListener(
    private val plugin: FoxCorePlugin,
) : Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    fun onJoin(event: PlayerJoinEvent) {
        plugin.rewards.notifyJoin(event.player)
    }
}
