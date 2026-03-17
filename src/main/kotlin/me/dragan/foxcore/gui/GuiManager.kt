package me.dragan.foxcore.gui

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class GuiManager {
    private val sessions = ConcurrentHashMap<UUID, GuiSession>()

    fun open(player: Player, screen: GuiScreen, configure: (GuiSession.() -> Unit)? = null) {
        val holder = GuiHolder(player.uniqueId)
        val inventory = Bukkit.createInventory(holder, screen.size, screen.title())
        holder.backingInventory = inventory

        val session = GuiSession(player.uniqueId, screen, inventory)
        configure?.invoke(session)

        inventory.clear()
        screen.render(session)
        sessions[player.uniqueId] = session
        player.openInventory(inventory)
    }

    fun getSession(playerId: UUID): GuiSession? = sessions[playerId]

    fun close(playerId: UUID) {
        sessions.remove(playerId)
    }
}
