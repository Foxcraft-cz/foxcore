package me.dragan.foxcore.help

import me.clip.placeholderapi.PlaceholderAPI
import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.entity.Player

class PluginHelpInfoService(
    private val plugin: FoxCorePlugin,
) {
    fun isPluginLoaded(configPath: String): Boolean {
        if (!plugin.config.getBoolean("$configPath.enabled", true)) {
            return false
        }

        val pluginName = plugin.config.getString("$configPath.plugin-name")
            ?.trim()
            .orEmpty()
            .ifBlank { return false }
        return plugin.server.pluginManager.plugins.any { it.name.equals(pluginName, ignoreCase = true) }
    }

    fun resolveFirst(player: Player, path: String): String? {
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

    fun commandLabel(path: String, fallback: String): String =
        plugin.config.getString(path)
            ?.trim()
            .orEmpty()
            .ifBlank { fallback }

    private fun isResolvedValue(value: String): Boolean =
        value.isNotEmpty() && !value.contains('%')
}
