package me.dragan.foxcore.report

import me.dragan.foxcore.FoxCorePlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

internal object ReportGuiSupport {
    private val miniMessage = MiniMessage.miniMessage()
    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z", Locale.ROOT)

    fun filler(): ItemStack =
        ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
            editMeta { meta -> meta.displayName(Component.text(" ")) }
        }

    fun item(material: Material, name: Component, lore: List<Component> = emptyList()): ItemStack =
        ItemStack(material).apply {
            editMeta { meta ->
                meta.displayName(name)
                meta.lore(lore)
            }
        }

    fun miniItem(material: Material, name: String, vararg lore: String): ItemStack =
        ItemStack(material).apply {
            editMeta { meta ->
                meta.displayName(miniMessage.deserialize(name))
                meta.lore(lore.map(miniMessage::deserialize))
            }
        }

    fun playerHead(plugin: FoxCorePlugin, playerId: String, name: String, lore: List<Component>): ItemStack =
        ItemStack(Material.PLAYER_HEAD).apply {
            val meta = itemMeta as? SkullMeta ?: return@apply
            meta.displayName(Component.text(name))
            meta.lore(lore)
            runCatching { UUID.fromString(playerId) }
                .map(plugin.server::getOfflinePlayer)
                .getOrNull()
                ?.let(meta::setOwningPlayer)
            itemMeta = meta
        }

    fun statusName(plugin: FoxCorePlugin, status: ReportStatus): Component =
        plugin.messages.text("report.status.${status.name.lowercase()}")

    fun typeName(plugin: FoxCorePlugin, type: ReportType): Component =
        plugin.messages.text("report.type.${type.name.lowercase()}")

    fun formatTimestamp(epochMillis: Long): String =
        dateFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
}
