package me.dragan.foxcore.gui

import org.bukkit.event.inventory.ClickType

interface GuiScreen {
    val size: Int

    fun title(): net.kyori.adventure.text.Component

    fun render(session: GuiSession)

    fun handleClick(session: GuiSession, rawSlot: Int, click: ClickType)

    fun onClose(session: GuiSession) = Unit
}
