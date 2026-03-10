package me.dragan.foxcore.back

import me.dragan.foxcore.back.storage.BackStorage
import me.dragan.foxcore.home.HomeBrowseResult
import me.dragan.foxcore.home.HomeData
import me.dragan.foxcore.home.HomeDeleteResult
import me.dragan.foxcore.home.HomeIconChangeResult
import me.dragan.foxcore.home.HomeListResult
import me.dragan.foxcore.home.HomeLookupResult
import me.dragan.foxcore.home.HomeNames
import me.dragan.foxcore.home.HomeRenameResult
import me.dragan.foxcore.home.HomeSetResult
import org.bukkit.Location
import org.bukkit.Material
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

    fun setHome(player: Player, homeName: String, maxHomes: Int): HomeSetResult {
        val playerId = player.uniqueId
        if (!loaded.contains(playerId)) {
            return HomeSetResult.Loading
        }

        val normalizedName = HomeNames.normalize(homeName)
        val current = current(playerId)
        val existingHomes = current.homes
        if (!existingHomes.containsKey(normalizedName) && existingHomes.size >= maxHomes) {
            return HomeSetResult.LimitReached(maxHomes)
        }

        val updated = current.copy(
            playerName = player.name,
            homes = existingHomes + (normalizedName to HomeData(StoredLocation.from(player.location))),
        )
        persist(playerId, updated)
        return HomeSetResult.Success(normalizedName)
    }

    fun findHome(player: Player, homeName: String): HomeLookupResult {
        val playerId = player.uniqueId
        if (!loaded.contains(playerId)) {
            return HomeLookupResult.Loading
        }

        val normalizedName = HomeNames.normalize(homeName)
        val stored = cache[playerId]?.homes?.get(normalizedName)
            ?: return HomeLookupResult.NotFound(normalizedName)
        val world = plugin.server.getWorld(stored.location.worldName)
            ?: return HomeLookupResult.MissingWorld(normalizedName, stored.location.worldName)
        return HomeLookupResult.Success(normalizedName, stored.location.toBukkitLocation(world))
    }

    fun listHomes(player: Player): HomeListResult {
        val playerId = player.uniqueId
        if (!loaded.contains(playerId)) {
            return HomeListResult.Loading
        }

        val data = cache[playerId] ?: return HomeListResult.Empty(player.name)
        val homeNames = data.homes.keys.sorted()
        return if (homeNames.isEmpty()) {
            HomeListResult.Empty(data.playerName ?: player.name)
        } else {
            HomeListResult.Success(data.playerName ?: player.name, homeNames)
        }
    }

    fun browseHomes(player: Player): HomeBrowseResult {
        val playerId = player.uniqueId
        if (!loaded.contains(playerId)) {
            return HomeBrowseResult.Loading
        }

        val data = cache[playerId] ?: return HomeBrowseResult.Empty(player.name)
        return if (data.homes.isEmpty()) {
            HomeBrowseResult.Empty(data.playerName ?: player.name)
        } else {
            HomeBrowseResult.Success(data.playerName ?: player.name, data.homes.toSortedMap())
        }
    }

    fun listHomesByLastKnownName(name: String, callback: (HomeListResult) -> Unit) {
        executor.execute {
            val data = storage.findByLastKnownName(name)
            val result = when {
                data == null -> HomeListResult.NotFound(name)
                data.homes.isEmpty() -> HomeListResult.Empty(data.playerName ?: name)
                else -> HomeListResult.Success(
                    playerName = data.playerName ?: name,
                    homes = data.homes.keys.sorted(),
                )
            }

            plugin.server.scheduler.runTask(plugin, Runnable { callback(result) })
        }
    }

    fun browseHomesByLastKnownName(name: String, callback: (HomeBrowseResult) -> Unit) {
        executor.execute {
            val data = storage.findByLastKnownName(name)
            val result = when {
                data == null -> HomeBrowseResult.NotFound(name)
                data.homes.isEmpty() -> HomeBrowseResult.Empty(data.playerName ?: name)
                else -> HomeBrowseResult.Success(
                    playerName = data.playerName ?: name,
                    homes = data.homes.toSortedMap(),
                )
            }

            plugin.server.scheduler.runTask(plugin, Runnable { callback(result) })
        }
    }

    fun getCachedHomeNames(player: Player): List<String> {
        if (!loaded.contains(player.uniqueId)) {
            return emptyList()
        }

        return cache[player.uniqueId]
            ?.homes
            ?.keys
            ?.sorted()
            .orEmpty()
    }

    fun setHomeIcon(player: Player, homeName: String, material: Material): HomeIconChangeResult {
        val playerId = player.uniqueId
        if (!loaded.contains(playerId)) {
            return HomeIconChangeResult.Loading
        }

        val normalizedName = HomeNames.normalize(homeName)
        val current = current(playerId)
        val existingHome = current.homes[normalizedName] ?: return HomeIconChangeResult.NotFound(normalizedName)
        val updated = current.copy(
            playerName = player.name,
            homes = current.homes + (normalizedName to existingHome.withIcon(material)),
        )
        persist(playerId, updated)
        return HomeIconChangeResult.Success(normalizedName, material.key.toString())
    }

    fun deleteHome(player: Player, homeName: String): HomeDeleteResult {
        val playerId = player.uniqueId
        if (!loaded.contains(playerId)) {
            return HomeDeleteResult.Loading
        }

        val normalizedName = HomeNames.normalize(homeName)
        val current = current(playerId)
        if (!current.homes.containsKey(normalizedName)) {
            return HomeDeleteResult.HomeNotFound(player.name, normalizedName)
        }

        val updated = current.copy(
            playerName = player.name,
            homes = current.homes
                .toMutableMap()
                .apply { remove(normalizedName) }
                .toSortedMap(),
        )
        persist(playerId, updated)
        return HomeDeleteResult.Success(player.name, normalizedName)
    }

    fun deleteHomeByLastKnownName(playerName: String, homeName: String, callback: (HomeDeleteResult) -> Unit) {
        executor.execute {
            val data = storage.findByLastKnownName(playerName)
            val result = when {
                data == null -> HomeDeleteResult.PlayerNotFound(playerName)
                !data.homes.containsKey(homeName) -> HomeDeleteResult.HomeNotFound(data.playerName ?: playerName, homeName)
                else -> {
                    val playerId = UUID.fromString(data.playerId)
                    val updated = data.copy(
                        homes = data.homes
                            .toMutableMap()
                            .apply { remove(homeName) }
                            .toSortedMap(),
                    )
                    cache[playerId] = updated
                    loaded.add(playerId)
                    storage.save(playerId, updated)
                    HomeDeleteResult.Success(data.playerName ?: playerName, homeName)
                }
            }

            plugin.server.scheduler.runTask(plugin, Runnable { callback(result) })
        }
    }

    fun renameHome(player: Player, oldHomeName: String, newHomeName: String): HomeRenameResult {
        val playerId = player.uniqueId
        if (!loaded.contains(playerId)) {
            return HomeRenameResult.Loading
        }

        val normalizedOldName = HomeNames.normalize(oldHomeName)
        val normalizedNewName = HomeNames.normalize(newHomeName)
        if (normalizedOldName == normalizedNewName) {
            return HomeRenameResult.SameName(normalizedOldName)
        }

        val current = current(playerId)
        val existingHome = current.homes[normalizedOldName] ?: return HomeRenameResult.NotFound(normalizedOldName)
        if (current.homes.containsKey(normalizedNewName)) {
            return HomeRenameResult.AlreadyExists(normalizedNewName)
        }

        val renamedHomes = current.homes
            .toMutableMap()
            .apply {
                remove(normalizedOldName)
                put(normalizedNewName, existingHome)
            }
            .toSortedMap()

        val updated = current.copy(
            playerName = player.name,
            homes = renamedHomes,
        )
        persist(playerId, updated)
        return HomeRenameResult.Success(normalizedOldName, normalizedNewName)
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
                playerName = player.name,
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
