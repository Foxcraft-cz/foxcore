package me.dragan.foxcore.listener

import me.dragan.foxcore.dispose.DisposeInventoryHolder
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent

class DisposeInventoryListener : Listener {
    @EventHandler(ignoreCancelled = true)
    fun onInventoryClick(event: InventoryClickEvent) {
        val holder = event.view.topInventory.holder as? DisposeInventoryHolder ?: return
        val player = event.whoClicked as? Player ?: return
        if (holder.viewerId == player.uniqueId) {
            return
        }

        event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun onInventoryDrag(event: InventoryDragEvent) {
        val holder = event.view.topInventory.holder as? DisposeInventoryHolder ?: return
        if (holder.viewerId == event.whoClicked.uniqueId) {
            return
        }

        event.isCancelled = true
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val holder = event.inventory.holder as? DisposeInventoryHolder ?: return
        if (holder.viewerId != event.player.uniqueId) {
            return
        }

        event.inventory.clear()
    }
}
