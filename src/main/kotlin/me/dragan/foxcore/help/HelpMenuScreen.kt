package me.dragan.foxcore.help

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.gui.GuiScreen
import me.dragan.foxcore.gui.GuiSession
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack

class HelpMenuScreen(
    private val plugin: FoxCorePlugin,
) : GuiScreen {
    override val size: Int = 27

    override fun title(): Component = plugin.messages.text("command.help.menu-title")

    override fun render(session: GuiSession) {
        val viewer = plugin.server.getPlayer(session.viewerId) ?: return
        val inventory = session.inventory
        inventory.clear()

        val visibleEntries = HelpCatalog.visibleEntries(plugin, viewer)
        val categories = HelpCategory.entries.filter { category -> visibleEntries.any { it.category == category } }
        val slots = listOf(10, 11, 13, 15, 16)

        for (slot in 0 until size) {
            inventory.setItem(slot, filler())
        }

        if (categories.isEmpty()) {
            inventory.setItem(
                13,
                ItemStack(Material.BOOK).apply {
                    editMeta { meta ->
                        meta.displayName(plugin.messages.text("command.help.empty-title"))
                        meta.lore(
                            listOf(
                                plugin.messages.text("command.help.empty-line1"),
                                plugin.messages.text("command.help.empty-line2"),
                            ),
                        )
                    }
                },
            )
        }

        categories.forEachIndexed { index, category ->
            val count = visibleEntries.count { it.category == category }
            inventory.setItem(slots[index], categoryItem(category, count))
        }

        inventory.setItem(22, navItem(Material.BARRIER, plugin.messages.text("command.help.close")))
    }

    override fun handleClick(session: GuiSession, rawSlot: Int, click: ClickType) {
        val viewer = plugin.server.getPlayer(session.viewerId) ?: return
        val visibleEntries = HelpCatalog.visibleEntries(plugin, viewer)
        val categories = HelpCategory.entries.filter { category -> visibleEntries.any { it.category == category } }
        val slots = listOf(10, 11, 13, 15, 16)

        if (rawSlot == 22) {
            viewer.closeInventory()
            return
        }

        val index = slots.indexOf(rawSlot)
        val category = categories.getOrNull(index) ?: return
        plugin.guiManager.open(viewer, HelpCategoryScreen(plugin, category))
    }

    private fun categoryItem(category: HelpCategory, count: Int): ItemStack =
        ItemStack(category.icon).apply {
            editMeta { meta ->
                meta.displayName(plugin.messages.text("command.help.category.${category.key}.name"))
                meta.lore(
                    listOf(
                        plugin.messages.text("command.help.category.${category.key}.description"),
                        plugin.messages.text("command.help.category.count", "count" to count.toString()),
                        plugin.messages.text("command.help.open-category"),
                    ),
                )
            }
        }

    private fun filler(): ItemStack =
        ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
            editMeta { it.displayName(Component.text(" ")) }
        }

    private fun navItem(material: Material, name: Component): ItemStack =
        ItemStack(material).apply {
            editMeta { it.displayName(name) }
        }
}
