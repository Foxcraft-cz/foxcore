package me.dragan.foxcore.listener

import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.inventory.EquipmentSlot

class PortalListener(
    private val plugin: FoxCorePlugin,
) : Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onInteract(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) {
            return
        }
        if (!plugin.portals.isPortalWand(event.item)) {
            return
        }

        val clicked = event.clickedBlock?.location ?: return
        if (!event.player.hasPermission("foxcore.portal.admin")) {
            event.player.sendMessage(plugin.messages.text("error.no-permission"))
            return
        }

        when (event.action) {
            Action.LEFT_CLICK_BLOCK -> {
                plugin.portals.setSelectionPos1(event.player, clicked)
                event.player.sendMessage(
                    plugin.messages.text(
                        "command.portal.pos1-set",
                        "world" to requireNotNull(clicked.world).name,
                        "x" to clicked.blockX.toString(),
                        "y" to clicked.blockY.toString(),
                        "z" to clicked.blockZ.toString(),
                    ),
                )
            }

            Action.RIGHT_CLICK_BLOCK -> {
                plugin.portals.setSelectionPos2(event.player, clicked)
                event.player.sendMessage(
                    plugin.messages.text(
                        "command.portal.pos2-set",
                        "world" to requireNotNull(clicked.world).name,
                        "x" to clicked.blockX.toString(),
                        "y" to clicked.blockY.toString(),
                        "z" to clicked.blockZ.toString(),
                    ),
                )
            }

            else -> return
        }

        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onMove(event: PlayerMoveEvent) {
        val to = event.to ?: return
        val from = event.from
        if (from.blockX == to.blockX && from.blockY == to.blockY && from.blockZ == to.blockZ) {
            return
        }

        plugin.portals.handleMove(event.player, to)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onTeleport(event: PlayerTeleportEvent) {
        plugin.portals.syncPlayerLocation(event.player, event.to)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        plugin.portals.clearTracking(event.player.uniqueId)
    }
}
