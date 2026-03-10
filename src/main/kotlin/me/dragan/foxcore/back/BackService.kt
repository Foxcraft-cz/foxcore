package me.dragan.foxcore.back

import me.dragan.foxcore.back.storage.BackStorage
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class BackService(
    private val plugin: JavaPlugin,
    private val storage: BackStorage,
) {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "FoxCore-BackStorage").apply { isDaemon = true }
    }
    private val cache = ConcurrentHashMap<UUID, BackData>()
    private val loaded = ConcurrentHashMap.newKeySet<UUID>()

    fun loadPlayer(playerId: UUID) {
        loaded.remove(playerId)
        executor.execute {
            cache[playerId] = storage.load(playerId) ?: BackData(playerId.toString())
            loaded.add(playerId)
        }
    }

    fun findOfflineLastLocationByName(name: String, callback: (OfflineLocationLookup) -> Unit) {
        executor.execute {
            val data = storage.findByLastKnownName(name)
            val result = when {
                data == null -> OfflineLocationLookup.NotFound
                data.lastLocation == null -> OfflineLocationLookup.NoStoredLocation(data.playerName ?: name)
                else -> {
                    val world = plugin.server.getWorld(data.lastLocation.worldName)
                    if (world == null) {
                        OfflineLocationLookup.MissingWorld(data.playerName ?: name, data.lastLocation.worldName)
                    } else {
                        OfflineLocationLookup.Success(
                            playerName = data.playerName ?: name,
                            location = data.lastLocation.toBukkitLocation(world),
                        )
                    }
                }
            }

            plugin.server.scheduler.runTask(plugin, Runnable { callback(result) })
        }
    }

    fun recordTeleportOrigin(player: Player, from: Location) {
        val playerId = player.uniqueId
        val updated = current(playerId).copy(
            playerName = player.name,
            lastLocation = StoredLocation.from(from),
            lastLocationAtMillis = System.currentTimeMillis(),
        )
        persist(playerId, updated)
    }

    fun recordDeath(player: Player, location: Location) {
        val playerId = player.uniqueId
        val updated = current(playerId).copy(
            playerName = player.name,
            lastDeathLocation = StoredLocation.from(location),
            lastDeathAtMillis = System.currentTimeMillis(),
        )
        persist(playerId, updated)
    }

    fun recordDisconnectLocation(player: Player) {
        val playerId = player.uniqueId
        val updated = current(playerId).copy(
            playerName = player.name,
            lastLocation = StoredLocation.from(player.location),
            lastLocationAtMillis = System.currentTimeMillis(),
        )
        persist(playerId, updated)
    }

    fun recordManualBackOrigin(player: Player) {
        recordTeleportOrigin(player, player.location)
    }

    fun findBackDestination(player: Player): BackLookupResult {
        val playerId = player.uniqueId
        if (!loaded.contains(playerId)) {
            return BackLookupResult.Loading
        }

        val data = cache[playerId] ?: return BackLookupResult.None
        val stored = chooseBackLocation(data) ?: return BackLookupResult.None
        val world = plugin.server.getWorld(stored.worldName) ?: return BackLookupResult.MissingWorld(stored.worldName)
        return BackLookupResult.Success(stored.toBukkitLocation(world))
    }

    fun shutdownAndFlush(players: Collection<Player>) {
        players.forEach { player ->
            val playerId = player.uniqueId
            cache[playerId] = current(playerId).copy(
                lastLocation = StoredLocation.from(player.location),
                lastLocationAtMillis = System.currentTimeMillis(),
            )
        }

        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS)
        cache.forEach { (playerId, data) -> storage.save(playerId, data) }
        storage.close()
    }

    private fun persist(playerId: UUID, data: BackData) {
        cache[playerId] = data
        loaded.add(playerId)
        executor.execute { storage.save(playerId, data) }
    }

    private fun current(playerId: UUID): BackData =
        cache[playerId] ?: BackData(playerId.toString())

    private fun chooseBackLocation(data: BackData): StoredLocation? {
        val prioritizeDeath = plugin.config.getBoolean("back.prioritize-death", true)
        val deathAt = data.lastDeathAtMillis ?: Long.MIN_VALUE
        val lastAt = data.lastLocationAtMillis ?: Long.MIN_VALUE

        return if (prioritizeDeath && deathAt >= lastAt) {
            data.lastDeathLocation ?: data.lastLocation
        } else {
            data.lastLocation ?: data.lastDeathLocation
        }
    }
}

sealed interface OfflineLocationLookup {
    data object NotFound : OfflineLocationLookup
    data class NoStoredLocation(val playerName: String) : OfflineLocationLookup
    data class MissingWorld(val playerName: String, val worldName: String) : OfflineLocationLookup
    data class Success(val playerName: String, val location: Location) : OfflineLocationLookup
}
