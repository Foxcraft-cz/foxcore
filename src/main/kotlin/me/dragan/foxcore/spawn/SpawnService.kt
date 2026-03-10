package me.dragan.foxcore.spawn

import org.bukkit.Location
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin

class SpawnService(
    private val plugin: JavaPlugin,
) {
    fun isEnabled(): Boolean =
        plugin.config.getBoolean("spawn.enabled", true)

    fun setSpawn(location: Location) {
        val world = requireNotNull(location.world).name
        plugin.config.set("spawn.location.world", world)
        plugin.config.set("spawn.location.x", location.x)
        plugin.config.set("spawn.location.y", location.y)
        plugin.config.set("spawn.location.z", location.z)
        plugin.config.set("spawn.location.yaw", location.yaw.toDouble())
        plugin.config.set("spawn.location.pitch", location.pitch.toDouble())
        plugin.saveConfig()
    }

    fun getSpawn(): Location? {
        if (!isEnabled()) {
            return null
        }

        val worldName = plugin.config.getString("spawn.location.world") ?: return null
        val world = plugin.server.getWorld(worldName) ?: return null

        return Location(
            world,
            plugin.config.getDouble("spawn.location.x"),
            plugin.config.getDouble("spawn.location.y"),
            plugin.config.getDouble("spawn.location.z"),
            plugin.config.getDouble("spawn.location.yaw").toFloat(),
            plugin.config.getDouble("spawn.location.pitch").toFloat(),
        )
    }

    fun shouldTeleportOnJoin(): Boolean =
        isEnabled() && plugin.config.getBoolean("spawn.on-join", false)

    fun shouldTeleportOnFirstJoin(): Boolean =
        isEnabled() && plugin.config.getBoolean("spawn.on-first-join", false)

    fun shouldTeleportOnRespawn(): Boolean =
        isEnabled() && plugin.config.getBoolean("spawn.on-respawn", false)
}

