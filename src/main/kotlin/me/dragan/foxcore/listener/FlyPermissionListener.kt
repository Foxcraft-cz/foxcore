package me.dragan.foxcore.listener

import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerRespawnEvent

class FlyPermissionListener(
    private val plugin: FoxCorePlugin,
) : Listener {
    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        enforceWorldFlightPermission(event.player, silent = true)
    }

    @EventHandler
    fun onChangedWorld(event: PlayerChangedWorldEvent) {
        enforceWorldFlightPermission(event.player, silent = false)
    }

    @EventHandler
    fun onRespawn(event: PlayerRespawnEvent) {
        enforceWorldFlightPermission(event.player, silent = false)
    }

    private fun enforceWorldFlightPermission(player: Player, silent: Boolean) {
        if (player.gameMode == GameMode.CREATIVE || player.gameMode == GameMode.SPECTATOR) {
            return
        }

        if (!player.allowFlight) {
            return
        }

        if (player.hasPermission(worldPermission(player))) {
            return
        }

        player.allowFlight = false
        player.isFlying = false

        if (!silent) {
            player.sendMessage(
                plugin.messages.text(
                    "command.fly.disabled-world",
                    "world" to player.world.name,
                ),
            )
        }
    }

    private fun worldPermission(player: Player): String =
        "foxcore.fly.world.${player.world.name.lowercase()}"
}
