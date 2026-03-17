package me.dragan.foxcore.listener

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.gui.GuiHolder
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerQuitEvent

class GuiListener(
    private val plugin: FoxCorePlugin,
) : Listener {
    @EventHandler(ignoreCancelled = true)
    fun onInventoryClick(event: InventoryClickEvent) {
        val holder = event.view.topInventory.holder as? GuiHolder ?: return
        val player = event.whoClicked as? org.bukkit.entity.Player ?: return
        if (holder.viewerId != player.uniqueId) {
            event.isCancelled = true
            return
        }

        val session = plugin.guiManager.getSession(player.uniqueId) ?: run {
            event.isCancelled = true
            return
        }

        event.isCancelled = true
        if (event.rawSlot !in 0 until event.view.topInventory.size) {
            return
        }

        session.screen.handleClick(session, event.rawSlot, event.click)
    }

    @EventHandler(ignoreCancelled = true)
    fun onInventoryDrag(event: InventoryDragEvent) {
        val holder = event.view.topInventory.holder as? GuiHolder ?: return
        if (holder.viewerId != event.whoClicked.uniqueId) {
            event.isCancelled = true
            return
        }

        event.isCancelled = true
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val holder = event.inventory.holder as? GuiHolder ?: return
        val session = plugin.guiManager.getSession(holder.viewerId) ?: return
        if (session.inventory != event.inventory) {
            return
        }

        session.screen.onClose(session)
        plugin.guiManager.close(holder.viewerId)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        plugin.guiManager.close(event.player.uniqueId)
    }
}
