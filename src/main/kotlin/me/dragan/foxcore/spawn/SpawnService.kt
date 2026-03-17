package me.dragan.foxcore.spawn

import org.bukkit.Location
import org.bukkit.plugin.java.JavaPlugin

class SpawnService(
    private val plugin: JavaPlugin,
) {
    private var cachedSpawn: Location? = null

    init {
        reload()
    }

    fun isEnabled(): Boolean =
        plugin.config.getBoolean("spawn.enabled", true)

    fun reload() {
        cachedSpawn = resolveSpawnFromConfig()
    }

    fun setSpawn(location: Location) {
        val world = requireNotNull(location.world).name
        plugin.config.set("spawn.location.world", world)
        plugin.config.set("spawn.location.x", location.x)
        plugin.config.set("spawn.location.y", location.y)
        plugin.config.set("spawn.location.z", location.z)
        plugin.config.set("spawn.location.yaw", location.yaw.toDouble())
        plugin.config.set("spawn.location.pitch", location.pitch.toDouble())
        plugin.saveConfig()
        cachedSpawn = location.clone()
    }

    fun getSpawn(): Location? {
        if (!isEnabled()) {
            return null
        }

        return cachedSpawn?.clone() ?: resolveSpawnFromConfig()?.also { cachedSpawn = it }?.clone()
    }

    private fun resolveSpawnFromConfig(): Location? {
        val worldName = plugin.config.getString("spawn.location.world")
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?: return null
        val world = plugin.server.getWorld(worldName)
            ?: plugin.server.worlds.firstOrNull { it.name.equals(worldName, ignoreCase = true) }
            ?: return null

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
