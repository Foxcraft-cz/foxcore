package me.dragan.foxcore.back.storage

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin

object StorageFactory {
    fun create(plugin: JavaPlugin, config: FileConfiguration): BackStorage =
        when (config.getString("storage.type", "sqlite")?.lowercase()) {
            "sqlite" -> SqliteBackStorage(plugin, config)
            "mysql" -> MysqlBackStorage(config)
            else -> error("Unsupported storage.type. Supported values: sqlite, mysql")
        }
}

