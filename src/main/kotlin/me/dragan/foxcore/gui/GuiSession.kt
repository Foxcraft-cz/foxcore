package me.dragan.foxcore.gui

import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import java.util.UUID

class GuiSession(
    val viewerId: UUID,
    val screen: GuiScreen,
    val inventory: Inventory,
) {
    val state = mutableMapOf<String, Any?>()

    fun viewer(): Player? = inventory.viewers.firstOrNull() as? Player
}
