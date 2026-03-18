package me.dragan.foxcore.listener

import io.papermc.paper.event.player.AsyncChatEvent
import me.dragan.foxcore.FoxCorePlugin
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

class ChatListener(
    private val plugin: FoxCorePlugin,
) : Listener {
    private val plainText = PlainTextComponentSerializer.plainText()

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onChat(event: AsyncChatEvent) {
        if (!plugin.chatModeration.isPublicChatEnabled()) {
            return
        }

        val player = event.player
        val result = plugin.chatModeration.moderatePublic(player, plainText.serialize(event.message()))
        event.isCancelled = true

        plugin.server.scheduler.runTask(plugin, Runnable {
            if (!player.isOnline) {
                return@Runnable
            }

            if (!result.allowed) {
                player.sendMessage(plugin.messages.text(result.denialKey ?: "chat.public.blocked", *result.placeholders))
                return@Runnable
            }

            val formatted = plugin.chatFormat.formatPublic(player, result.message)
            val senderVanished = plugin.vanishService.isVanished(player)
            plugin.server.onlinePlayers
                .asSequence()
                .filter { canReceive(it, player, senderVanished) }
                .forEach { it.sendMessage(formatted) }
            plugin.server.consoleSender.sendMessage(formatted)
        })
    }

    private fun canReceive(recipient: Player, sender: Player, senderVanished: Boolean): Boolean {
        if (recipient.uniqueId == sender.uniqueId) {
            return true
        }
        if (!senderVanished) {
            return true
        }

        return recipient.hasPermission("foxcore.chat.vanish.see") || recipient.canSee(sender)
    }
}
