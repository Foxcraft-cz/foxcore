package me.dragan.foxcore.teleport

import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Player

class SafeTeleportService(
    private val plugin: FoxCorePlugin,
) {
    fun teleport(player: Player, requested: Location): SafeTeleportResult {
        val origin = player.location.clone()
        val world = requested.world ?: return SafeTeleportResult.NO_SAFE_GROUND
        val bypassAllSafety = shouldBypassAllSafety(player)
        val bypassGroundSafety = shouldBypassGroundSafety(player)
        val wasFlying = player.isFlying

        val destination = if (bypassGroundSafety) {
            requested.clone()
        } else {
            findSafeGroundLocation(world, requested) ?: return SafeTeleportResult.NO_SAFE_GROUND
        }

        if (!bypassAllSafety && !isSafeDestination(world, destination, bypassGroundSafety)) {
            return SafeTeleportResult.NO_SAFE_GROUND
        }

        val success = player.teleport(destination)
        if (!success) {
            return SafeTeleportResult.FAILED
        }

        if (player.allowFlight && wasFlying) {
            player.isFlying = true
        }

        plugin.teleportEffects.play(player, origin, destination)
        return SafeTeleportResult.SUCCESS
    }

    private fun findSafeGroundLocation(world: World, requested: Location): Location? {
        val blockX = requested.blockX
        val blockZ = requested.blockZ
        val startY = requested.blockY.coerceIn(world.minHeight, world.maxHeight - 1)

        for (offset in 0..24) {
            val below = startY - offset
            if (below >= world.minHeight && isSafeStandingSpot(world, blockX, below, blockZ)) {
                return buildStandingLocation(world, requested, blockX, below, blockZ)
            }

            if (offset == 0) {
                continue
            }

            val above = startY + offset
            if (above < world.maxHeight && isSafeStandingSpot(world, blockX, above, blockZ)) {
                return buildStandingLocation(world, requested, blockX, above, blockZ)
            }
        }

        return null
    }

    private fun isSafeStandingSpot(world: World, x: Int, groundY: Int, z: Int): Boolean {
        val ground = world.getBlockAt(x, groundY, z)
        val feet = world.getBlockAt(x, groundY + 1, z)
        val head = world.getBlockAt(x, groundY + 2, z)

        return ground.isSafeGround() && feet.isSafeBodySpace() && head.isSafeBodySpace()
    }

    private fun buildStandingLocation(world: World, requested: Location, x: Int, groundY: Int, z: Int): Location =
        Location(
            world,
            x + 0.5,
            groundY + 1.0,
            z + 0.5,
            requested.yaw,
            requested.pitch,
        )

    private fun isSafeDestination(world: World, destination: Location, canFly: Boolean): Boolean {
        val feet = world.getBlockAt(destination.blockX, destination.blockY, destination.blockZ)
        val head = world.getBlockAt(destination.blockX, destination.blockY + 1, destination.blockZ)
        if (!feet.isSafeBodySpace() || !head.isSafeBodySpace()) {
            return false
        }

        if (canFly) {
            return true
        }

        val ground = world.getBlockAt(destination.blockX, destination.blockY - 1, destination.blockZ)
        return ground.isSafeGround()
    }

    private fun shouldBypassGroundSafety(player: Player): Boolean =
        shouldBypassAllSafety(player) || (player.allowFlight && player.isFlying)

    private fun shouldBypassAllSafety(player: Player): Boolean =
        player.gameMode == GameMode.CREATIVE || player.gameMode == GameMode.SPECTATOR

    private fun Block.isSafeGround(): Boolean {
        if (isPassable || !type.isSolid || isLiquid) {
            return false
        }

        return type !in unsafeGroundTypes
    }

    private fun Block.isSafeBodySpace(): Boolean {
        if (!isPassable || isLiquid) {
            return false
        }

        return type !in unsafeBodyTypes
    }

    companion object {
        private val unsafeGroundTypes = setOf(
            Material.LAVA,
            Material.MAGMA_BLOCK,
            Material.CAMPFIRE,
            Material.SOUL_CAMPFIRE,
            Material.CACTUS,
            Material.FIRE,
            Material.SOUL_FIRE,
        )

        private val unsafeBodyTypes = setOf(
            Material.LAVA,
            Material.WATER,
            Material.FIRE,
            Material.SOUL_FIRE,
            Material.CACTUS,
            Material.CAMPFIRE,
            Material.SOUL_CAMPFIRE,
            Material.SWEET_BERRY_BUSH,
            Material.POWDER_SNOW,
        )
    }
}

enum class SafeTeleportResult {
    SUCCESS,
    NO_SAFE_GROUND,
    FAILED,
}
