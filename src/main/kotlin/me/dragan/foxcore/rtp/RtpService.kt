package me.dragan.foxcore.rtp

import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.HeightMap
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom

class RtpService(
    private val plugin: FoxCorePlugin,
) {
    private val activeRequests = ConcurrentHashMap.newKeySet<UUID>()
    private val cooldowns = ConcurrentHashMap<UUID, Long>()

    @Volatile
    private var settings = RtpSettings()

    @Volatile
    private var worldSettings = emptyMap<String, RtpWorldSettings>()

    init {
        reload()
    }

    fun reload() {
        settings = RtpSettings(
            enabled = plugin.config.getBoolean("rtp.enabled", true),
            cooldownSeconds = plugin.config.getLong("rtp.cooldown-seconds", 300L).coerceAtLeast(0L),
            maxChunkAttempts = plugin.config.getInt("rtp.max-chunk-attempts", 8).coerceIn(1, 32),
            samplesPerChunk = plugin.config.getInt("rtp.samples-per-chunk", 4).coerceIn(1, 16),
        )

        val configuredWorlds = mutableMapOf<String, RtpWorldSettings>()
        val worldsSection = plugin.config.getConfigurationSection("rtp.worlds")
        worldsSection?.getKeys(false)?.forEach { worldName ->
            val worldSection = worldsSection.getConfigurationSection(worldName) ?: return@forEach
            val minRadius = worldSection.getInt("min-radius", 500).coerceAtLeast(0)
            val maxRadius = worldSection.getInt("max-radius", 5000).coerceAtLeast(minRadius + 1)
            configuredWorlds[worldName.lowercase()] = RtpWorldSettings(
                worldName = worldName,
                enabled = worldSection.getBoolean("enabled", true),
                minRadius = minRadius,
                maxRadius = maxRadius,
                centerX = worldSection.getDouble("center-x", 0.0),
                centerZ = worldSection.getDouble("center-z", 0.0),
                icon = parseIcon(worldName, worldSection.getString("icon")),
            )
        }

        worldSettings = configuredWorlds
    }

    fun availableWorlds(): List<RtpWorldSettings> =
        worldSettings.values
            .asSequence()
            .filter { it.enabled }
            .filter { plugin.server.getWorld(it.worldName) != null }
            .sortedBy { it.worldName.lowercase() }
            .toList()

    fun beginTeleport(player: Player, worldName: String, onComplete: (RtpResult) -> Unit): RtpStartResult {
        if (!settings.enabled) {
            return RtpStartResult.Disabled
        }

        val worldConfig = worldSettings[worldName.lowercase()]
            ?.takeIf { it.enabled }
            ?: return RtpStartResult.WorldDisabled

        val world = plugin.server.getWorld(worldConfig.worldName)
            ?: plugin.server.worlds.firstOrNull { it.name.equals(worldConfig.worldName, ignoreCase = true) }
            ?: return RtpStartResult.WorldUnavailable

        if (!activeRequests.add(player.uniqueId)) {
            return RtpStartResult.AlreadySearching
        }

        val cooldownRemainingSeconds = remainingCooldownSeconds(player)
        if (cooldownRemainingSeconds > 0 && !player.hasPermission("foxcore.rtp.bypasscooldown")) {
            activeRequests.remove(player.uniqueId)
            return RtpStartResult.Cooldown(cooldownRemainingSeconds)
        }

        searchChunks(player, world, worldConfig, settings.maxChunkAttempts, onComplete)
        return RtpStartResult.Started
    }

    private fun searchChunks(
        player: Player,
        world: World,
        worldConfig: RtpWorldSettings,
        remainingChunkAttempts: Int,
        onComplete: (RtpResult) -> Unit,
    ) {
        if (remainingChunkAttempts <= 0) {
            finish(player.uniqueId)
            onComplete(RtpResult.NoLocationFound)
            return
        }

        if (!player.isOnline) {
            finish(player.uniqueId)
            onComplete(RtpResult.Cancelled)
            return
        }

        val candidateChunk = randomChunkCandidate(worldConfig)
        val loadFuture = world.getChunkAtAsync(candidateChunk.chunkX, candidateChunk.chunkZ, true, false)
        loadFuture.whenComplete { chunk, throwable ->
            if (throwable != null || chunk == null) {
                plugin.server.scheduler.runTask(plugin, Runnable {
                    searchChunks(player, world, worldConfig, remainingChunkAttempts - 1, onComplete)
                })
                return@whenComplete
            }

            plugin.server.scheduler.runTask(plugin, Runnable {
                val result = resolveCandidateLocation(player, world, chunk)
                when {
                    result == null -> searchChunks(player, world, worldConfig, remainingChunkAttempts - 1, onComplete)
                    !player.isOnline -> {
                        finish(player.uniqueId)
                        onComplete(RtpResult.Cancelled)
                    }

                    else -> {
                        val origin = player.location.clone()
                        val success = player.teleport(result)
                        finish(player.uniqueId)
                        if (!success) {
                            onComplete(RtpResult.Failed)
                            return@Runnable
                        }

                        if (player.allowFlight) {
                            player.isFlying = true
                        }

                        plugin.teleportEffects.play(player, origin, result)
                        cooldowns[player.uniqueId] = System.currentTimeMillis() + (settings.cooldownSeconds * 1000L)
                        onComplete(RtpResult.Success(result))
                    }
                }
            })
        }
    }

    private fun resolveCandidateLocation(player: Player, world: World, chunk: Chunk): Location? {
        repeat(settings.samplesPerChunk) {
            val blockX = (chunk.x shl 4) + ThreadLocalRandom.current().nextInt(16)
            val blockZ = (chunk.z shl 4) + ThreadLocalRandom.current().nextInt(16)
            val location = when (world.environment) {
                World.Environment.NETHER -> findNetherLocation(world, blockX, blockZ)
                else -> findSurfaceLocation(world, blockX, blockZ)
            }

            if (location != null) {
                if (!world.worldBorder.isInside(location)) {
                    plugin.logger.warning(
                        "Skipping RTP location outside the world border in '${world.name}' at " +
                            "${location.blockX}, ${location.blockY}, ${location.blockZ}.",
                    )
                    return@repeat
                }

                return location.apply {
                    yaw = player.location.yaw
                    pitch = player.location.pitch
                }
            }
        }

        return null
    }

    private fun findSurfaceLocation(world: World, blockX: Int, blockZ: Int): Location? {
        val groundY = world.getHighestBlockYAt(blockX, blockZ, HeightMap.MOTION_BLOCKING_NO_LEAVES)
        if (groundY < world.minHeight || groundY >= world.maxHeight - 2) {
            return null
        }

        if (!isSafeStandingSpot(world, blockX, groundY, blockZ)) {
            return null
        }

        return buildStandingLocation(world, blockX, groundY, blockZ)
    }

    private fun findNetherLocation(world: World, blockX: Int, blockZ: Int): Location? {
        val startY = minOf(world.maxHeight - 3, 126)
        for (groundY in startY downTo world.minHeight) {
            if (isSafeStandingSpot(world, blockX, groundY, blockZ)) {
                return buildStandingLocation(world, blockX, groundY, blockZ)
            }
        }

        return null
    }

    private fun isSafeStandingSpot(world: World, x: Int, groundY: Int, z: Int): Boolean {
        if (groundY + 2 >= world.maxHeight) {
            return false
        }

        val ground = world.getBlockAt(x, groundY, z)
        val feet = world.getBlockAt(x, groundY + 1, z)
        val head = world.getBlockAt(x, groundY + 2, z)

        return ground.isSafeGround() && feet.isSafeBodySpace() && head.isSafeBodySpace()
    }

    private fun buildStandingLocation(world: World, x: Int, groundY: Int, z: Int): Location =
        Location(world, x + 0.5, groundY + 1.0, z + 0.5)

    private fun randomChunkCandidate(worldConfig: RtpWorldSettings): ChunkCandidate {
        val random = ThreadLocalRandom.current()
        val minSquared = worldConfig.minRadius.toDouble() * worldConfig.minRadius.toDouble()
        val maxSquared = worldConfig.maxRadius.toDouble() * worldConfig.maxRadius.toDouble()
        val radius = kotlin.math.sqrt(random.nextDouble(minSquared, maxSquared))
        val angle = random.nextDouble(0.0, Math.PI * 2.0)
        val blockX = worldConfig.centerX + (kotlin.math.cos(angle) * radius)
        val blockZ = worldConfig.centerZ + (kotlin.math.sin(angle) * radius)
        return ChunkCandidate(
            chunkX = kotlin.math.floor(blockX / 16.0).toInt(),
            chunkZ = kotlin.math.floor(blockZ / 16.0).toInt(),
        )
    }

    private fun remainingCooldownSeconds(player: Player): Long {
        val until = cooldowns[player.uniqueId] ?: return 0L
        val remainingMillis = until - System.currentTimeMillis()
        if (remainingMillis <= 0L) {
            cooldowns.remove(player.uniqueId)
            return 0L
        }

        return (remainingMillis + 999L) / 1000L
    }

    private fun finish(playerId: UUID) {
        activeRequests.remove(playerId)
    }

    private fun parseIcon(worldName: String, configured: String?): Material {
        val fallback = when (worldName.lowercase()) {
            "world" -> Material.GRASS_BLOCK
            "world_nether" -> Material.NETHERRACK
            "world_the_end" -> Material.END_STONE
            else -> Material.ENDER_PEARL
        }

        val materialName = configured?.trim().orEmpty()
        if (materialName.isEmpty()) {
            return fallback
        }

        return Material.matchMaterial(materialName.uppercase()) ?: fallback
    }

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
            Material.WATER,
            Material.MAGMA_BLOCK,
            Material.CAMPFIRE,
            Material.SOUL_CAMPFIRE,
            Material.CACTUS,
            Material.FIRE,
            Material.SOUL_FIRE,
            Material.POWDER_SNOW,
            Material.OAK_LEAVES,
            Material.SPRUCE_LEAVES,
            Material.BIRCH_LEAVES,
            Material.JUNGLE_LEAVES,
            Material.ACACIA_LEAVES,
            Material.DARK_OAK_LEAVES,
            Material.MANGROVE_LEAVES,
            Material.CHERRY_LEAVES,
            Material.AZALEA_LEAVES,
            Material.FLOWERING_AZALEA_LEAVES,
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

data class RtpWorldSettings(
    val worldName: String,
    val enabled: Boolean,
    val minRadius: Int,
    val maxRadius: Int,
    val centerX: Double,
    val centerZ: Double,
    val icon: Material,
)

data class RtpSettings(
    val enabled: Boolean = true,
    val cooldownSeconds: Long = 300L,
    val maxChunkAttempts: Int = 8,
    val samplesPerChunk: Int = 4,
)

data class ChunkCandidate(
    val chunkX: Int,
    val chunkZ: Int,
)

sealed interface RtpStartResult {
    data object Started : RtpStartResult
    data object Disabled : RtpStartResult
    data object WorldDisabled : RtpStartResult
    data object WorldUnavailable : RtpStartResult
    data object AlreadySearching : RtpStartResult
    data class Cooldown(
        val remainingSeconds: Long,
    ) : RtpStartResult
}

sealed interface RtpResult {
    data class Success(
        val location: Location,
    ) : RtpResult

    data object NoLocationFound : RtpResult
    data object Failed : RtpResult
    data object Cancelled : RtpResult
}
