package me.dragan.foxcore.chat

import me.dragan.foxcore.FoxCorePlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class SpyService(
    private val plugin: FoxCorePlugin,
) {
    private val socialSpyEnabled = ConcurrentHashMap.newKeySet<UUID>()
    private val commandSpyEnabled = ConcurrentHashMap.newKeySet<UUID>()
    private val ignoredCommandNames = setOf("message", "msg", "reply", "r", "socialspy", "commandspy")

    fun toggleSocialSpy(player: Player): Boolean =
        toggle(player.uniqueId, socialSpyEnabled)

    fun toggleCommandSpy(player: Player): Boolean =
        toggle(player.uniqueId, commandSpyEnabled)

    fun clear(playerId: UUID) {
        socialSpyEnabled.remove(playerId)
        commandSpyEnabled.remove(playerId)
    }

    fun notifySocialSpies(sender: Player, target: Player, message: String) {
        val component = plugin.messages.text("spy.social.prefix")
            .append(plugin.chatFormat.playerNameComponent(sender, "/message ${sender.name} "))
            .appendSpace()
            .append(Component.text("->", NamedTextColor.DARK_GRAY))
            .appendSpace()
            .append(plugin.chatFormat.playerNameComponent(target, "/message ${target.name} "))
            .appendSpace()
            .append(plugin.chatFormat.plainMessageComponent(sender, message, "/reply "))

        Bukkit.getOnlinePlayers()
            .asSequence()
            .filter { it.uniqueId != sender.uniqueId && it.uniqueId != target.uniqueId }
            .filter { socialSpyEnabled.contains(it.uniqueId) }
            .filter { it.hasPermission("foxcore.socialspy") }
            .forEach { it.sendMessage(component) }
    }

    fun notifyCommandSpies(sender: Player, commandLine: String) {
        val commandName = commandLine.removePrefix("/")
            .trim()
            .substringBefore(' ')
            .substringAfterLast(':')
            .lowercase()
        if (commandName in ignoredCommandNames) {
            return
        }

        val component = plugin.messages.text("spy.command.prefix")
            .append(plugin.chatFormat.playerNameComponent(sender, "/message ${sender.name} "))
            .appendSpace()
            .append(
                Component.text(commandLine, NamedTextColor.GRAY)
                    .clickEvent(ClickEvent.suggestCommand(commandLine))
                    .hoverEvent(HoverEvent.showText(plugin.messages.text("spy.command.hover"))),
            )

        Bukkit.getOnlinePlayers()
            .asSequence()
            .filter { it.uniqueId != sender.uniqueId }
            .filter { commandSpyEnabled.contains(it.uniqueId) }
            .filter { it.hasPermission("foxcore.commandspy") }
            .forEach { it.sendMessage(component) }
    }

    private fun toggle(playerId: UUID, store: MutableSet<UUID>): Boolean =
        if (store.add(playerId)) {
            true
        } else {
            store.remove(playerId)
            false
        }
}
