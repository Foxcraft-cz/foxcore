package me.dragan.foxcore.placeholder

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.OfflinePlayer

class FoxCorePlaceholderExpansion(
    private val plugin: FoxCorePlugin,
) : PlaceholderExpansion() {

    override fun getIdentifier(): String = "foxcore"

    override fun getAuthor(): String = "Dragan"

    override fun getVersion(): String = plugin.pluginMeta.version

    override fun persist(): Boolean = true

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        val targetId = player?.uniqueId ?: return ""
        return when (params.lowercase()) {
            "afk" -> plugin.afk.afkSinceMillis(targetId)?.let { "true" } ?: "false"
            "afk_status" -> plugin.afk.afkSinceMillis(targetId)?.let { "afk" } ?: "active"
            "afk_duration_seconds" -> plugin.afk.afkDurationSeconds(targetId).toString()
            "afk_duration_human" -> plugin.afk.afkDurationHuman(targetId)
            else -> null
        }
    }
}
