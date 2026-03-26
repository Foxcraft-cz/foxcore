package me.dragan.foxcore.config

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class YamlResourceSynchronizer(
    private val plugin: JavaPlugin,
) {
    fun sync(resourcePath: String): File {
        val target = File(plugin.dataFolder, resourcePath)
        target.parentFile?.mkdirs()

        if (!target.exists()) {
            plugin.saveResource(resourcePath, false)
            return target
        }

        val defaults = plugin.getResource(resourcePath)
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { YamlConfiguration.loadConfiguration(it) }
            ?: error("Bundled resource '$resourcePath' is missing")

        val current = YamlConfiguration.loadConfiguration(target)
        val normalized = YamlConfiguration().also { merged ->
            mergeSections(defaults, current, merged)
        }

        if (normalized.saveToString() != current.saveToString()) {
            normalized.save(target)
        }

        return target
    }

    private fun mergeSections(
        defaults: ConfigurationSection,
        current: ConfigurationSection,
        target: ConfigurationSection,
    ) {
        val keys = linkedSetOf<String>()
        keys.addAll(defaults.getKeys(false))
        keys.addAll(current.getKeys(false))

        for (key in keys) {
            val currentHasValue = current.contains(key)
            val defaultsHasValue = defaults.contains(key)
            val currentIsSection = current.isConfigurationSection(key)
            val defaultsIsSection = defaults.isConfigurationSection(key)

            when {
                currentIsSection || defaultsIsSection -> {
                    val nestedTarget = target.createSection(key)
                    mergeSections(
                        defaults.getConfigurationSection(key) ?: YamlConfiguration(),
                        current.getConfigurationSection(key) ?: YamlConfiguration(),
                        nestedTarget,
                    )
                }

                currentHasValue -> target.set(key, current.get(key))
                defaultsHasValue -> target.set(key, defaults.get(key))
            }
        }
    }
}
