package me.dragan.foxcore.config

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
        val normalized = YamlConfiguration()

        for (key in defaults.getKeys(true)) {
            if (defaults.isConfigurationSection(key)) {
                continue
            }

            val value = if (current.contains(key)) current.get(key) else defaults.get(key)
            normalized.set(key, value)
        }

        if (normalized.saveToString() != current.saveToString()) {
            normalized.save(target)
        }

        return target
    }
}

