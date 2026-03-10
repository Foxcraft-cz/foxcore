package me.dragan.foxcore.back

import org.bukkit.Location
import org.bukkit.World

data class StoredLocation(
    val worldName: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float,
) {
    fun toBukkitLocation(world: World): Location =
        Location(world, x, y, z, yaw, pitch)

    companion object {
        fun from(location: Location): StoredLocation =
            StoredLocation(
                worldName = requireNotNull(location.world).name,
                x = location.x,
                y = location.y,
                z = location.z,
                yaw = location.yaw,
                pitch = location.pitch,
            )
    }
}

