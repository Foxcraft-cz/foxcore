package me.dragan.foxcore.placeholder

import me.clip.placeholderapi.PlaceholderAPI
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import me.dragan.foxcore.FoxCorePlugin
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

class FoxCorePlaceholderExpansion(
    private val plugin: FoxCorePlugin,
) : PlaceholderExpansion() {
    private val plainText = PlainTextComponentSerializer.plainText()

    override fun getIdentifier(): String = "foxcore"

    override fun getAuthor(): String = "Dragan"

    override fun getVersion(): String = plugin.pluginMeta.version

    override fun persist(): Boolean = true

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        val lowerParams = params.lowercase()
        return when {
            lowerParams == "afk" -> player?.uniqueId?.let { id -> plugin.afk.afkSinceMillis(id)?.let { "true" } ?: "false" } ?: ""
            lowerParams == "afk_status" -> player?.uniqueId?.let { id -> plugin.afk.afkSinceMillis(id)?.let { "afk" } ?: "active" } ?: ""
            lowerParams == "afk_duration_seconds" -> player?.uniqueId?.let(plugin.afk::afkDurationSeconds)?.toString() ?: ""
            lowerParams == "afk_duration_human" -> player?.uniqueId?.let(plugin.afk::afkDurationHuman) ?: ""
            lowerParams == "rank_expiry_rank" -> (player as? Player)?.let(::rankExpiry)?.rankName ?: ""
            lowerParams == "rank_expiry_time" -> (player as? Player)?.let(::rankExpiry)?.timeRemaining ?: ""
            lowerParams == "rank_expiry_chat" -> (player as? Player)?.let(::rankExpiryChat) ?: ""
            lowerParams.startsWith("logout_") -> quitBroadcast(params.substringAfter('_'))
            lowerParams.startsWith("quit_broadcast_") -> quitBroadcast(params.substringAfter("quit_broadcast_"))
            lowerParams.startsWith("login_") -> joinBroadcast(params.substringAfter('_'))
            lowerParams.startsWith("join_broadcast_") -> joinBroadcast(params.substringAfter("join_broadcast_"))
            else -> null
        }
    }

    private fun quitBroadcast(playerName: String): String {
        val normalizedName = playerName.trim()
        if (normalizedName.isEmpty() || !plugin.config.getBoolean("join-messages.quit-broadcast-enabled", true)) {
            return ""
        }

        return plainText.serialize(
            plugin.messages.text(
                "event.quit.broadcast",
                *commonPlaceholders(normalizedName),
            ),
        )
    }

    private fun joinBroadcast(playerName: String): String {
        val normalizedName = playerName.trim()
        if (normalizedName.isEmpty()) {
            return ""
        }

        val target = resolvePlayer(normalizedName)
        val isFirstJoin = target?.hasPlayedBefore() == false
        val path = when {
            isFirstJoin && plugin.config.getBoolean("join-messages.first-join-broadcast-enabled", true) -> "event.join.first-broadcast"
            plugin.config.getBoolean("join-messages.join-broadcast-enabled", true) -> "event.join.broadcast"
            else -> return ""
        }

        return plainText.serialize(
            plugin.messages.text(
                path,
                *commonPlaceholders(normalizedName),
            ),
        )
    }

    private fun resolvePlayer(playerName: String): OfflinePlayer? =
        plugin.server.getPlayerExact(playerName)
            ?: plugin.server.onlinePlayers.firstOrNull { it.name.equals(playerName, ignoreCase = true) }
            ?: plugin.server.getOfflinePlayerIfCached(playerName)

    private fun rankExpiry(player: Player): RankExpiry? =
        RANK_CANDIDATES.firstNotNullOfOrNull { candidate ->
            resolveLuckPermsPlaceholder(player, candidate.placeholder)?.let { value ->
                RankExpiry(candidate.rankName, value)
            }
        }

    private fun rankExpiryChat(player: Player): String =
        rankExpiry(player)?.let { "${it.rankName} za ${it.timeRemaining}" } ?: "bez dočasného ranku"

    private fun resolveLuckPermsPlaceholder(player: Player, placeholder: String): String? =
        PlaceholderAPI.setPlaceholders(player, placeholder)
            .trim()
            .takeIf(::isResolvedValue)

    private fun isResolvedValue(value: String): Boolean =
        value.isNotEmpty() && !value.contains('%')

    private fun commonPlaceholders(playerName: String): Array<Pair<String, String>> {
        val onlineCount = plugin.server.onlinePlayers.size.toString()
        val maxPlayers = plugin.server.maxPlayers.toString()
        val serverName = plugin.server.name
        return arrayOf(
            "player" to playerName,
            "online" to onlineCount,
            "max" to maxPlayers,
            "server" to serverName,
        )
    }

    private data class RankExpiry(
        val rankName: String,
        val timeRemaining: String,
    )

    private data class RankCandidate(
        val rankName: String,
        val placeholder: String,
    )

    companion object {
        private val RANK_CANDIDATES = listOf(
            RankCandidate("Sampion", "%luckperms_group_expiry_time_premium%"),
            RankCandidate("Mistr", "%luckperms_group_expiry_time_vip%"),
            RankCandidate("Patron", "%luckperms_group_expiry_time_pro%"),
        )
    }
}
