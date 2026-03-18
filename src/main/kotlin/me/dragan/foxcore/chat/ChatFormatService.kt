package me.dragan.foxcore.chat

import me.dragan.foxcore.FoxCorePlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Player

class ChatFormatService(
    private val plugin: FoxCorePlugin,
) {
    private val miniMessage = MiniMessage.miniMessage()

    @Volatile
    private var publicFormat = "<prefix><name><dark_gray>:</dark_gray> <message>"

    @Volatile
    private var prefixFormat = "<dark_gray>[</dark_gray><gray><prefix></gray><dark_gray>]</dark_gray> "

    @Volatile
    private var hoverLines: List<String> = emptyList()

    @Volatile
    private var defaultNameColor: TextColor = NamedTextColor.WHITE

    @Volatile
    private var defaultChatColor: TextColor = NamedTextColor.GRAY

    fun reload() {
        publicFormat = plugin.config.getString("chat.public.format", publicFormat).orEmpty()
        prefixFormat = plugin.config.getString("chat.public.prefix-format", prefixFormat).orEmpty()
        hoverLines = plugin.config.getStringList("chat.public.hover-lines")
        defaultNameColor = plugin.chatLuckPerms.parseColor(
            plugin.config.getString("chat.public.default-name-color", "#D2CEC6"),
        ) ?: NamedTextColor.WHITE
        defaultChatColor = plugin.chatLuckPerms.parseColor(
            plugin.config.getString("chat.public.default-chat-color", "#F4EEE8"),
        ) ?: NamedTextColor.GRAY
    }

    fun formatPublic(player: Player, message: String): Component {
        val profile = plugin.chatLuckPerms.resolveProfile(player)
        val prefix = formatPrefix(profile)
        val suggestion = "/message ${player.name} "
        val name = playerNameComponent(player, profile, suggestion)
        val chatMessage = Component.text(message)
            .color(profile.chatColor ?: defaultChatColor)

        return miniMessage.deserialize(
            publicFormat,
            Placeholder.component("prefix", prefix),
            Placeholder.component("name", name),
            Placeholder.component("message", chatMessage),
        )
    }

    fun playerNameComponent(player: Player, suggestion: String): Component =
        playerNameComponent(player, plugin.chatLuckPerms.resolveProfile(player), suggestion)

    fun plainMessageComponent(player: Player, message: String, suggestion: String? = null): Component {
        val component = Component.text(message)
            .color(plugin.chatLuckPerms.resolveProfile(player).chatColor ?: defaultChatColor)

        if (suggestion == null) {
            return component
        }

        return component
            .clickEvent(ClickEvent.suggestCommand(suggestion))
            .hoverEvent(HoverEvent.showText(Component.text("Reply to ${player.name}")))
    }

    private fun formatPrefix(profile: ChatProfile): Component {
        if (profile.prefixText.isBlank()) {
            return Component.empty()
        }

        var prefixComponent = profile.prefixComponent
        profile.rankFormat?.let { format ->
            if (format.color != null) {
                prefixComponent = Component.text().append(prefixComponent).color(format.color).build()
            }
            for ((decoration, state) in format.decorations) {
                prefixComponent = prefixComponent.decoration(decoration, state)
            }
        }

        return miniMessage.deserialize(
            prefixFormat,
            Placeholder.component("prefix", prefixComponent),
        )
    }

    private fun buildHover(player: Player, profile: ChatProfile): Component {
        if (hoverLines.isEmpty()) {
            return Component.text(player.name)
        }

        val rank = profile.prefixText.ifBlank { "-" }
        val lines = hoverLines.map { line ->
            miniMessage.deserialize(
                line
                    .replace("%player%", player.name)
                    .replace("%rank%", rank)
                    .replace("%world%", player.world.name),
            )
        }

        return lines.drop(1).fold(lines.first()) { acc, line -> acc.append(Component.newline()).append(line) }
    }

    private fun playerNameComponent(player: Player, profile: ChatProfile, suggestion: String): Component =
        Component.text(player.name)
            .color(profile.nameColor ?: defaultNameColor)
            .clickEvent(ClickEvent.suggestCommand(suggestion))
            .hoverEvent(HoverEvent.showText(buildHover(player, profile)))
}
