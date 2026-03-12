package me.dragan.foxcore.listener

import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent

class AfkListener(
    private val plugin: FoxCorePlugin,
) : Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        plugin.afk.handleJoin(event.player)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        plugin.afk.handleQuit(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onChat(event: AsyncChatEvent) {
        scheduleActivity(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onCommand(event: PlayerCommandPreprocessEvent) {
        val commandLine = event.message.removePrefix("/").trim()
        val commandName = commandLine.substringBefore(' ').lowercase()
        if (commandName == "afk") {
            return
        }
        plugin.afk.recordActivity(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInteract(event: PlayerInteractEvent) {
        plugin.afk.recordActivity(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        plugin.afk.recordActivity(player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onDrop(event: PlayerDropItemEvent) {
        plugin.afk.recordActivity(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPickup(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return
        plugin.afk.recordActivity(player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        plugin.afk.recordActivity(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        plugin.afk.recordActivity(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onTeleport(event: PlayerTeleportEvent) {
        plugin.afk.recordActivity(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        plugin.afk.recordActivity(player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onDamageOther(event: EntityDamageByEntityEvent) {
        val player = event.damager as? Player ?: return
        plugin.afk.recordActivity(player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onMove(event: PlayerMoveEvent) {
        val to = event.to ?: return
        val from = event.from
        if (from.blockX == to.blockX && from.blockY == to.blockY && from.blockZ == to.blockZ) {
            return
        }

        val player = event.player
        if (isPassiveMovement(player)) {
            return
        }

        plugin.afk.recordActivity(player)
    }

    private fun scheduleActivity(player: Player) {
        plugin.server.scheduler.runTask(plugin, Runnable {
            if (player.isOnline) {
                plugin.afk.recordActivity(player)
            }
        })
    }

    private fun isPassiveMovement(player: Player): Boolean {
        if (player.isInsideVehicle || player.isGliding) {
            return true
        }
        if (player.isInWater || player.isSwimming) {
            return true
        }

        val feet = player.location.block.type
        val below = player.location.clone().subtract(0.0, 1.0, 0.0).block.type
        return feet == Material.BUBBLE_COLUMN || below == Material.BUBBLE_COLUMN
    }
}
