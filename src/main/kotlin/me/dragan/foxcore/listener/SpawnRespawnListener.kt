package me.dragan.foxcore.listener

import me.dragan.foxcore.FoxCraftPlugin
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerRespawnEvent

class SpawnRespawnListener(
    private val plugin: FoxCraftPlugin,
) : Listener {
    @EventHandler
    fun onRespawn(event: PlayerRespawnEvent) {
        if (!plugin.spawnService.shouldTeleportOnRespawn()) {
            return
        }

        val spawn = plugin.spawnService.getSpawn() ?: return
        event.respawnLocation = spawn
    }
}

