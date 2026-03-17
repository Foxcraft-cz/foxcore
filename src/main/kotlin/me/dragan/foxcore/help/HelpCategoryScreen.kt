package me.dragan.foxcore.help

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.gui.GuiScreen
import me.dragan.foxcore.gui.GuiSession
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack

class HelpCategoryScreen(
    private val plugin: FoxCorePlugin,
    private val category: HelpCategory,
) : GuiScreen {
    override val size: Int = 54
    private val pageSize = 45

    override fun title(): Component =
        plugin.messages.text("command.help.category-screen-title", "category" to plainCategoryName())

    override fun render(session: GuiSession) {
        val viewer = plugin.server.getPlayer(session.viewerId) ?: return
        val inventory = session.inventory
        inventory.clear()

        val entries = HelpCatalog.visibleEntries(plugin, viewer).filter { it.category == category }
        val page = currentPage(session, entries.size)
        val pageCount = totalPages(entries.size)

        entries.drop(page * pageSize).take(pageSize).forEachIndexed { index, entry ->
            inventory.setItem(index, entryItem(viewer, entry))
        }

        for (slot in 45..53) {
            inventory.setItem(slot, filler())
        }

        if (page > 0) {
            inventory.setItem(45, navItem(Material.ARROW, plugin.messages.text("command.help.previous-page")))
        }
        inventory.setItem(49, navItem(Material.BARRIER, plugin.messages.text("command.help.back-to-categories")))
        if (page + 1 < pageCount) {
            inventory.setItem(53, navItem(Material.ARROW, plugin.messages.text("command.help.next-page")))
        }
    }

    override fun handleClick(session: GuiSession, rawSlot: Int, click: ClickType) {
        val viewer = plugin.server.getPlayer(session.viewerId) ?: return
        val entries = HelpCatalog.visibleEntries(plugin, viewer).filter { it.category == category }
        when (rawSlot) {
            45 -> if (currentPage(session, entries.size) > 0) {
                session.state["page"] = currentPage(session, entries.size) - 1
                render(session)
            }
            49 -> plugin.guiManager.open(viewer, HelpMenuScreen(plugin))
            53 -> if (currentPage(session, entries.size) + 1 < totalPages(entries.size)) {
                session.state["page"] = currentPage(session, entries.size) + 1
                render(session)
            }
            in 0 until pageSize -> {
                val entry = entries.getOrNull(currentPage(session, entries.size) * pageSize + rawSlot) ?: return
                viewer.closeInventory()
                viewer.sendMessage(plugin.messages.text("command.help.command-header", "command" to commandLabel(entry)))
                viewer.sendMessage(descriptionComponent(entry))
                viewer.sendMessage(plugin.messages.text("command.help.command-usage", "usage" to usageLabel(entry)))
            }
        }
    }

    private fun entryItem(viewer: org.bukkit.entity.Player, entry: HelpEntry): ItemStack =
        ItemStack(entry.icon).apply {
            editMeta { meta ->
                meta.displayName(nameComponent(entry))
                val lore = mutableListOf<Component>()
                lore += descriptionComponent(entry)
                lore += plugin.messages.text("command.help.command-usage", "usage" to usageLabel(entry))
                entry.dynamicLore(plugin, viewer).forEach { token ->
                    lore += dynamicLine(token)
                }
                lore += plugin.messages.text("command.help.command-click")
                meta.lore(lore)
            }
        }

    private fun dynamicLine(token: String): Component {
        val key = token.substringBefore(':')
        val value = token.substringAfter(':', "")
        return when (key) {
            "help.dynamic.homes.limit" -> plugin.messages.text("command.help.dynamic.homes.limit", "count" to value)
            "help.dynamic.homes.limit.unlimited" -> plugin.messages.text("command.help.dynamic.homes.limit-unlimited")
            "help.dynamic.homes.current" -> plugin.messages.text("command.help.dynamic.homes.current", "count" to value)
            "help.dynamic.homes.loading" -> plugin.messages.text("command.help.dynamic.homes.loading")
            "help.dynamic.back.types" -> plugin.messages.text("command.help.dynamic.back.types", "types" to value)
            "help.dynamic.rtp.worlds" -> plugin.messages.text("command.help.dynamic.rtp.worlds", "count" to value)
            "help.dynamic.warp.count" -> plugin.messages.text("command.help.dynamic.warp.count", "count" to value)
            "help.dynamic.residence.max-count" -> plugin.messages.text("command.help.dynamic.residence.max-count", "count" to value)
            "help.dynamic.residence.max-count-unavailable" -> plugin.messages.text("command.help.dynamic.residence.max-count-unavailable")
            "help.dynamic.residence.max-size" -> plugin.messages.text("command.help.dynamic.residence.max-size", "size" to value)
            "help.dynamic.residence.max-size-unavailable" -> plugin.messages.text("command.help.dynamic.residence.max-size-unavailable")
            "help.dynamic.vote.pending" -> plugin.messages.text("command.help.dynamic.vote.pending", "count" to value)
            "help.dynamic.vote.pending-unavailable" -> plugin.messages.text("command.help.dynamic.vote.pending-unavailable")
            "help.dynamic.kits.available" -> plugin.messages.text("command.help.dynamic.kits.available", "count" to value)
            "help.dynamic.kits.available-unavailable" -> plugin.messages.text("command.help.dynamic.kits.available-unavailable")
            "help.dynamic.spawn.available" -> plugin.messages.text("command.help.dynamic.spawn.available")
            "help.dynamic.spawn.unset" -> plugin.messages.text("command.help.dynamic.spawn.unset")
            "help.dynamic.tpa.expiration" -> plugin.messages.text("command.help.dynamic.tpa.expiration", "seconds" to value)
            else -> Component.text(value)
        }
    }

    private fun usageLabel(entry: HelpEntry): String =
        entry.customUsage?.invoke(plugin) ?: when (entry.key) {
            "spawn" -> "/spawn [player]"
            "back" -> "/back [teleport|death]"
            "tp" -> "/tp <player>"
            "tphere" -> "/tphere <player>"
            "tpa" -> "/tpa <player>"
            "tpahere" -> "/tpahere <player>"
            "tpaccept" -> "/tpaccept [player]"
            "tpadeny" -> "/tpadeny [player]"
            "rtp" -> "/rtp"
            "home" -> "/home [name]"
            "homes" -> "/homes"
            "sethome" -> "/sethome [name]"
            "renamehome" -> "/renamehome <old> <new>"
            "sethomeicon" -> "/sethomeicon <name> [material]"
            "delhome" -> "/delhome <home>"
            "residence" -> "/res"
            "warp" -> "/warp [name]"
            "vote" -> plugin.pluginHelpInfo.commandLabel("help.integrations.vote.command", "/vote")
            "kits" -> plugin.pluginHelpInfo.commandLabel("help.integrations.kits.command", "/kits")
            "skins" -> plugin.pluginHelpInfo.commandLabel("help.integrations.skinsrestorer.command", "/skin")
            "bannermaker" -> plugin.pluginHelpInfo.commandLabel("help.integrations.bannermaker.command", "/banner")
            "armorstandeditor" -> plugin.pluginHelpInfo.commandLabel("help.integrations.armorstandeditor.command", "/ase")
            "rosetimber" -> plugin.pluginHelpInfo.commandLabel("help.integrations.rosetimber.command", "/timber")
            "afk" -> "/afk"
            "feed" -> "/feed [player]"
            "fix" -> "/fix"
            "fixall" -> "/fixall"
            "heal" -> "/heal [player]"
            "onlinetime" -> "/onlinetime [player]"
            "hat" -> "/hat"
            "head" -> "/head [player] [amount]"
            "dispose" -> "/dispose"
            "craft" -> "/craft"
            "enderchest" -> "/enderchest"
            "anvil" -> "/anvil"
            "cartographytable" -> "/cartographytable"
            "loom" -> "/loom"
            "grindstone" -> "/grindstone"
            "smithingtable" -> "/smithingtable"
            "stonecutter" -> "/stonecutter"
            else -> "/${entry.key}"
        }

    private fun nameComponent(entry: HelpEntry): Component =
        entry.customName?.invoke(plugin)
            ?: plugin.messages.text("command.help.command.${entry.key}.name")

    private fun descriptionComponent(entry: HelpEntry): Component =
        entry.customDescription?.invoke(plugin)
            ?: plugin.messages.text("command.help.command.${entry.key}.description")

    private fun commandLabel(entry: HelpEntry): String =
        usageLabel(entry).substringBefore(' ')

    private fun currentPage(session: GuiSession, totalEntries: Int): Int =
        (session.state["page"] as? Int ?: 0).coerceIn(0, totalPages(totalEntries) - 1)

    private fun totalPages(totalEntries: Int): Int =
        totalEntries.coerceAtLeast(1).let { ((it - 1) / pageSize) + 1 }

    private fun plainCategoryName(): String =
        category.key.replaceFirstChar(Char::uppercase)

    private fun filler(): ItemStack =
        ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
            editMeta { it.displayName(Component.text(" ")) }
        }

    private fun navItem(material: Material, name: Component): ItemStack =
        ItemStack(material).apply {
            editMeta { it.displayName(name) }
        }
}
