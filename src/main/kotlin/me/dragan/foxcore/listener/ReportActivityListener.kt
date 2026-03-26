package me.dragan.foxcore.listener

import io.papermc.paper.event.player.AsyncChatEvent
import me.dragan.foxcore.FoxCorePlugin
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerQuitEvent

class ReportActivityListener(
    private val plugin: FoxCorePlugin,
) : Listener {
    private val plainText = PlainTextComponentSerializer.plainText()

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onChat(event: AsyncChatEvent) {
        if (plugin.chatModeration.isPublicChatEnabled()) {
            return
        }

        plugin.reportActivity.recordChat(event.player.uniqueId, plainText.serialize(event.message()))
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onCommand(event: PlayerCommandPreprocessEvent) {
        plugin.reportActivity.recordCommand(event.player.uniqueId, event.message)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        plugin.reportActivity.clear(event.player.uniqueId)
    }
}
