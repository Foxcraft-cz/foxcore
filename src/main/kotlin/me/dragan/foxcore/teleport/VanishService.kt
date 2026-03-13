package me.dragan.foxcore.teleport

import me.clip.placeholderapi.PlaceholderAPI
import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.entity.Player

class VanishService(
    private val plugin: FoxCorePlugin,
) {
    fun isVanished(player: Player): Boolean {
        if (!plugin.config.getBoolean("teleport.vanish-check.enabled", true)) {
            return false
        }
        if (plugin.server.pluginManager.getPlugin("PlaceholderAPI") == null) {
            return false
        }

        val placeholders = plugin.config.getStringList("teleport.vanish-check.placeholders")
            .map(String::trim)
            .filter(String::isNotEmpty)

        if (placeholders.isEmpty()) {
            return false
        }

        return placeholders.any { placeholder ->
            isTruthy(PlaceholderAPI.setPlaceholders(player, placeholder))
        }
    }

    private fun isTruthy(value: String?): Boolean =
        when (value?.trim()?.lowercase()) {
            "true", "yes", "on", "1" -> true
            else -> false
        }
}
