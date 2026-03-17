package me.dragan.foxcore.gui

import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import java.util.UUID

class GuiHolder(
    val viewerId: UUID,
) : InventoryHolder {
    lateinit var backingInventory: Inventory

    override fun getInventory(): Inventory = backingInventory
}
