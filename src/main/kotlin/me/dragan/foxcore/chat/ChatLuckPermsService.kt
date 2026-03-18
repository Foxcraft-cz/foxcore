package me.dragan.foxcore.chat

import me.dragan.foxcore.FoxCorePlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.luckperms.api.LuckPermsProvider
import org.bukkit.entity.Player

class ChatLuckPermsService(
    private val plugin: FoxCorePlugin,
) {
    private val legacySerializer = LegacyComponentSerializer.legacyAmpersand()

    fun resolveProfile(player: Player): ChatProfile {
        if (!plugin.config.getBoolean("chat.luckperms.enabled", true)) {
            return ChatProfile.empty()
        }

        val luckPerms = runCatching { LuckPermsProvider.get() }.getOrNull() ?: return ChatProfile.empty()
        val user = luckPerms.userManager.getUser(player.uniqueId) ?: return ChatProfile.empty()
        val queryOptions = luckPerms.contextManager.getQueryOptions(player)
        val metaData = user.cachedData.getMetaData(queryOptions)

        val prefixText = metaData.prefix?.trim().orEmpty()
        val prefixComponent = if (prefixText.isBlank()) {
            Component.empty()
        } else {
            legacySerializer.deserialize(prefixText)
        }

        val rankColorKey = plugin.config.getString("chat.luckperms.rank-color-key", "rank_color").orEmpty()
        val nameColorKey = plugin.config.getString("chat.luckperms.name-color-key", "name_color").orEmpty()
        val chatColorKey = plugin.config.getString("chat.luckperms.chat-color-key", "chat_color").orEmpty()

        return ChatProfile(
            prefixText = prefixText,
            prefixComponent = prefixComponent,
            rankFormat = parseFormat(metaData.getMetaValue(rankColorKey)),
            nameColor = parseColor(metaData.getMetaValue(nameColorKey)),
            chatColor = parseColor(metaData.getMetaValue(chatColorKey)),
        )
    }

    fun parseFormat(raw: String?): ChatTextFormat? {
        val value = raw?.trim().orEmpty()
        if (value.isBlank()) {
            return null
        }

        var color: TextColor? = null
        val decorations = linkedMapOf<TextDecoration, TextDecoration.State>()
        var index = 0
        while (index < value.length) {
            when {
                value.startsWith("&#", index) && index + 8 <= value.length -> {
                    color = TextColor.fromHexString("#${value.substring(index + 2, index + 8)}") ?: color
                    index += 8
                }
                value.startsWith("#", index) && index + 7 <= value.length -> {
                    color = TextColor.fromHexString(value.substring(index, index + 7)) ?: color
                    index += 7
                }
                value[index] == '&' && index + 1 < value.length -> {
                    when (value[index + 1].lowercaseChar()) {
                        'l' -> decorations[TextDecoration.BOLD] = TextDecoration.State.TRUE
                        'o' -> decorations[TextDecoration.ITALIC] = TextDecoration.State.TRUE
                        'n' -> decorations[TextDecoration.UNDERLINED] = TextDecoration.State.TRUE
                        'm' -> decorations[TextDecoration.STRIKETHROUGH] = TextDecoration.State.TRUE
                        'k' -> decorations[TextDecoration.OBFUSCATED] = TextDecoration.State.TRUE
                        'r' -> decorations.clear()
                    }
                    index += 2
                }
                else -> {
                    val remaining = value.substring(index)
                    val namedColor = parseColor(remaining)
                    if (namedColor != null) {
                        color = namedColor
                        break
                    }
                    index += 1
                }
            }
        }

        if (color == null && decorations.isEmpty()) {
            return null
        }

        return ChatTextFormat(color, decorations)
    }

    fun parseColor(raw: String?): TextColor? {
        val value = raw?.trim().orEmpty()
        if (value.isBlank()) {
            return null
        }

        if (value.startsWith("&#") && value.length == 8) {
            return TextColor.fromHexString("#${value.substring(2)}")
        }
        if (value.startsWith("#") && value.length == 7) {
            return TextColor.fromHexString(value)
        }

        return when (value.lowercase()) {
            "black" -> NamedTextColor.BLACK
            "dark_blue" -> NamedTextColor.DARK_BLUE
            "dark_green" -> NamedTextColor.DARK_GREEN
            "dark_aqua" -> NamedTextColor.DARK_AQUA
            "dark_red" -> NamedTextColor.DARK_RED
            "dark_purple" -> NamedTextColor.DARK_PURPLE
            "gold" -> NamedTextColor.GOLD
            "gray", "grey" -> NamedTextColor.GRAY
            "dark_gray", "dark_grey" -> NamedTextColor.DARK_GRAY
            "blue" -> NamedTextColor.BLUE
            "green" -> NamedTextColor.GREEN
            "aqua" -> NamedTextColor.AQUA
            "red" -> NamedTextColor.RED
            "light_purple" -> NamedTextColor.LIGHT_PURPLE
            "yellow" -> NamedTextColor.YELLOW
            "white" -> NamedTextColor.WHITE
            else -> null
        }
    }
}

data class ChatProfile(
    val prefixText: String,
    val prefixComponent: Component,
    val rankFormat: ChatTextFormat?,
    val nameColor: TextColor?,
    val chatColor: TextColor?,
) {
    companion object {
        fun empty(): ChatProfile = ChatProfile("", Component.empty(), null, null, null)
    }
}

data class ChatTextFormat(
    val color: TextColor?,
    val decorations: Map<TextDecoration, TextDecoration.State>,
)
