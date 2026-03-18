package me.dragan.foxcore.chat

import me.dragan.foxcore.FoxCorePlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PrivateMessageService(
    private val plugin: FoxCorePlugin,
) {
    private val miniMessage = MiniMessage.miniMessage()
    private val lastContacts = ConcurrentHashMap<UUID, UUID>()

    @Volatile
    private var outgoingFormat = "<dark_gray>[</dark_gray><gray>to</gray> <name><dark_gray>]</dark_gray> <message> <button>"

    @Volatile
    private var incomingFormat = "<dark_gray>[</dark_gray><gray>from</gray> <name><dark_gray>]</dark_gray> <message> <button>"

    fun reload() {
        outgoingFormat = plugin.config.getString("chat.private.outgoing-format", outgoingFormat).orEmpty()
        incomingFormat = plugin.config.getString("chat.private.incoming-format", incomingFormat).orEmpty()
    }

    fun findVisibleTarget(sender: Player, input: String): Player? =
        Bukkit.getOnlinePlayers().firstOrNull { candidate ->
            candidate.uniqueId != sender.uniqueId &&
                candidate.name.equals(input, ignoreCase = true) &&
                canTarget(sender, candidate)
        }

    fun visiblePlayerNames(sender: Player, prefix: String): List<String> =
        Bukkit.getOnlinePlayers()
            .asSequence()
            .filter { it.uniqueId != sender.uniqueId }
            .filter { canTarget(sender, it) }
            .map(Player::getName)
            .filter { it.startsWith(prefix, ignoreCase = true) }
            .sorted()
            .toList()

    fun replyTarget(sender: Player): Player? {
        val targetId = lastContacts[sender.uniqueId] ?: return null
        val target = Bukkit.getPlayer(targetId) ?: return null
        return target.takeIf { canTarget(sender, it) }
    }

    fun hasReplyTarget(sender: Player): Boolean = lastContacts.containsKey(sender.uniqueId)

    fun send(sender: Player, target: Player, rawMessage: String): PrivateMessageSendResult {
        val moderated = plugin.chatModeration.moderatePrivate(sender, rawMessage)
        if (!moderated.allowed) {
            return PrivateMessageSendResult(false, moderated.denialKey, moderated.placeholders)
        }

        val targetSuggestion = "/message ${target.name} "
        val replySuggestion = "/reply "
        val outgoing = miniMessage.deserialize(
            outgoingFormat,
            Placeholder.component("name", plugin.chatFormat.playerNameComponent(target, targetSuggestion)),
            Placeholder.component("message", plugin.chatFormat.plainMessageComponent(sender, moderated.message, targetSuggestion)),
            Placeholder.component("button", outgoingButton(targetSuggestion)),
        )
        val incoming = miniMessage.deserialize(
            incomingFormat,
            Placeholder.component("name", plugin.chatFormat.playerNameComponent(sender, replySuggestion)),
            Placeholder.component("message", plugin.chatFormat.plainMessageComponent(sender, moderated.message, replySuggestion)),
            Placeholder.component("button", replyButton(replySuggestion)),
        )

        sender.sendMessage(outgoing)
        target.sendMessage(incoming)
        lastContacts[sender.uniqueId] = target.uniqueId
        lastContacts[target.uniqueId] = sender.uniqueId
        plugin.spies.notifySocialSpies(sender, target, moderated.message)
        return PrivateMessageSendResult(true, null, emptyArray())
    }

    private fun outgoingButton(suggestion: String): Component =
        plugin.messages.text("chat.private.button.again")
            .clickEvent(ClickEvent.suggestCommand(suggestion))
            .hoverEvent(HoverEvent.showText(plugin.messages.text("chat.private.button.again-hover")))

    private fun replyButton(suggestion: String): Component =
        plugin.messages.text("chat.private.button.reply")
            .clickEvent(ClickEvent.suggestCommand(suggestion))
            .hoverEvent(HoverEvent.showText(plugin.messages.text("chat.private.button.reply-hover")))

    private fun canTarget(sender: Player, target: Player): Boolean {
        if (!plugin.vanishService.isVanished(target)) {
            return true
        }

        return sender.hasPermission("foxcore.chat.vanish.see") || sender.canSee(target)
    }
}

data class PrivateMessageSendResult(
    val success: Boolean,
    val denialKey: String?,
    val placeholders: Array<out Pair<String, String>>,
)
