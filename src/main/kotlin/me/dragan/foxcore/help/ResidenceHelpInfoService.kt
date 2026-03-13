package me.dragan.foxcore.help

import me.clip.placeholderapi.PlaceholderAPI
import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.entity.Player

class ResidenceHelpInfoService(
    private val plugin: FoxCorePlugin,
) {
    fun isAvailable(): Boolean {
        if (!plugin.config.getBoolean("help.residence.enabled", true)) {
            return false
        }

        val pluginName = plugin.config.getString("help.residence.plugin-name", "Residence")
            ?.trim()
            .orEmpty()
            .ifBlank { "Residence" }
        return plugin.server.pluginManager.plugins.any { it.name.equals(pluginName, ignoreCase = true) }
    }

    fun maxResidences(player: Player): String? =
        resolve(player, "help.residence.max-count-placeholders")

    fun maxSize(player: Player): String? =
        resolve(player, "help.residence.max-size-placeholders")

    private fun resolve(player: Player, path: String): String? {
        if (plugin.server.pluginManager.getPlugin("PlaceholderAPI") == null) {
            return null
        }

        val placeholders = plugin.config.getStringList(path)
            .map(String::trim)
            .filter(String::isNotEmpty)

        return placeholders
            .asSequence()
            .map { placeholder -> PlaceholderAPI.setPlaceholders(player, placeholder) }
            .map(String::trim)
            .firstOrNull(::isResolvedValue)
    }

    private fun isResolvedValue(value: String): Boolean =
        value.isNotEmpty() && !value.contains('%')
}
