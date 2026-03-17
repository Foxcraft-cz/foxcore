package me.dragan.foxcore.warp

import me.dragan.foxcore.back.StoredLocation
import me.dragan.foxcore.back.storage.BackStorage
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class WarpService(
    private val plugin: JavaPlugin,
    private val storage: BackStorage,
) {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "FoxCore-WarpStorage").apply { isDaemon = true }
    }
    private val cache = ConcurrentHashMap<String, WarpData>()
    private val cooldowns = ConcurrentHashMap<UUID, Long>()

    init {
        storage.loadAllWarps().forEach { (name, warp) -> cache[name] = warp }
    }

    fun reload() = Unit

    fun listWarps(): List<WarpData> =
        cache.values.sortedWith(compareBy<WarpData>({ it.scope != WarpScope.SERVER }, { it.name }))

    fun getWarp(name: String): WarpData? =
        cache[WarpNames.normalize(name)]

    fun getOwnedWarpNames(player: Player): List<String> =
        cache.values
            .asSequence()
            .filter { it.scope == WarpScope.PLAYER && it.ownerId == player.uniqueId }
            .map(WarpData::name)
            .sorted()
            .toList()

    fun createPlayerWarp(player: Player, name: String, maxWarps: Int): WarpCreateResult {
        val normalized = WarpNames.normalize(name)
        if (!WarpNames.isValid(normalized)) {
            return WarpCreateResult.InvalidName
        }
        if (cache.containsKey(normalized)) {
            return WarpCreateResult.AlreadyExists(normalized)
        }

        val ownedCount = cache.values.count { it.scope == WarpScope.PLAYER && it.ownerId == player.uniqueId }
        if (ownedCount >= maxWarps) {
            return WarpCreateResult.LimitReached(maxWarps)
        }

        val warp = WarpData(
            name = normalized,
            scope = WarpScope.PLAYER,
            location = StoredLocation.from(player.location),
            ownerId = player.uniqueId,
            ownerName = player.name,
        )
        persist(normalized, warp)
        return WarpCreateResult.Success(warp)
    }

    fun setServerWarp(player: Player, name: String): WarpSetServerResult {
        val normalized = WarpNames.normalize(name)
        if (!WarpNames.isValid(normalized)) {
            return WarpSetServerResult.InvalidName
        }

        val existing = cache[normalized]
        if (existing != null && existing.scope != WarpScope.SERVER) {
            return WarpSetServerResult.AlreadyExists(normalized)
        }

        val warp = WarpData(
            name = normalized,
            scope = WarpScope.SERVER,
            location = StoredLocation.from(player.location),
            ownerName = "Server",
            iconMaterialKey = existing?.iconMaterialKey,
            title = existing?.title,
            description = existing?.description,
        )
        persist(normalized, warp)
        return WarpSetServerResult.Success(warp, existed = existing != null)
    }

    fun renameWarp(oldName: String, newName: String): WarpRenameResult {
        val normalizedOld = WarpNames.normalize(oldName)
        val normalizedNew = WarpNames.normalize(newName)
        if (!WarpNames.isValid(normalizedNew)) {
            return WarpRenameResult.InvalidName
        }
        if (normalizedOld == normalizedNew) {
            return WarpRenameResult.SameName(normalizedOld)
        }

        val existing = cache[normalizedOld] ?: return WarpRenameResult.NotFound(normalizedOld)
        if (cache.containsKey(normalizedNew)) {
            return WarpRenameResult.AlreadyExists(normalizedNew)
        }

        cache.remove(normalizedOld)
        val renamed = existing.copy(name = normalizedNew)
        cache[normalizedNew] = renamed
        executor.execute { storage.renameWarp(normalizedOld, normalizedNew) }
        executor.execute { storage.saveWarp(normalizedNew, renamed) }
        return WarpRenameResult.Success(normalizedOld, normalizedNew)
    }

    fun moveWarpHere(player: Player, name: String): WarpUpdateResult {
        val normalized = WarpNames.normalize(name)
        val existing = cache[normalized] ?: return WarpUpdateResult.NotFound(normalized)
        val updated = existing.copy(location = StoredLocation.from(player.location), ownerName = existing.ownerName ?: player.name)
        persist(normalized, updated)
        return WarpUpdateResult.Success(updated)
    }

    fun setWarpIcon(name: String, material: Material): WarpUpdateResult {
        val normalized = WarpNames.normalize(name)
        val existing = cache[normalized] ?: return WarpUpdateResult.NotFound(normalized)
        val updated = existing.withIcon(material)
        persist(normalized, updated)
        return WarpUpdateResult.Success(updated)
    }

    fun setWarpTitle(name: String, title: String?): WarpUpdateResult {
        val normalized = WarpNames.normalize(name)
        val existing = cache[normalized] ?: return WarpUpdateResult.NotFound(normalized)
        val updated = existing.withTitle(title)
        persist(normalized, updated)
        return WarpUpdateResult.Success(updated)
    }

    fun setWarpDescription(name: String, description: String?): WarpUpdateResult {
        val normalized = WarpNames.normalize(name)
        val existing = cache[normalized] ?: return WarpUpdateResult.NotFound(normalized)
        val updated = existing.withDescription(description)
        persist(normalized, updated)
        return WarpUpdateResult.Success(updated)
    }

    fun deleteWarp(name: String): WarpDeleteResult {
        val normalized = WarpNames.normalize(name)
        val existing = cache.remove(normalized) ?: return WarpDeleteResult.NotFound(normalized)
        executor.execute { storage.deleteWarp(normalized) }
        return WarpDeleteResult.Success(existing)
    }

    fun remainingTeleportCooldownSeconds(player: Player): Long {
        val until = cooldowns[player.uniqueId] ?: return 0L
        val remainingMillis = until - System.currentTimeMillis()
        if (remainingMillis <= 0L) {
            cooldowns.remove(player.uniqueId)
            return 0L
        }

        return (remainingMillis + 999L) / 1000L
    }

    fun markTeleportUsed(player: Player) {
        val seconds = plugin.config.getLong("warp.teleport-cooldown-seconds", 0L).coerceAtLeast(0L)
        if (seconds <= 0L) {
            cooldowns.remove(player.uniqueId)
            return
        }

        cooldowns[player.uniqueId] = System.currentTimeMillis() + (seconds * 1000L)
    }

    fun isOwner(player: Player, warp: WarpData): Boolean =
        warp.scope == WarpScope.PLAYER && warp.ownerId == player.uniqueId

    fun shutdown() {
        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS)
    }

    private fun persist(name: String, data: WarpData) {
        cache[name] = data
        executor.execute { storage.saveWarp(name, data) }
    }
}

sealed interface WarpCreateResult {
    data object InvalidName : WarpCreateResult
    data class AlreadyExists(val name: String) : WarpCreateResult
    data class LimitReached(val max: Int) : WarpCreateResult
    data class Success(val warp: WarpData) : WarpCreateResult
}

sealed interface WarpSetServerResult {
    data object InvalidName : WarpSetServerResult
    data class AlreadyExists(val name: String) : WarpSetServerResult
    data class Success(val warp: WarpData, val existed: Boolean) : WarpSetServerResult
}

sealed interface WarpRenameResult {
    data object InvalidName : WarpRenameResult
    data class NotFound(val name: String) : WarpRenameResult
    data class AlreadyExists(val name: String) : WarpRenameResult
    data class SameName(val name: String) : WarpRenameResult
    data class Success(val oldName: String, val newName: String) : WarpRenameResult
}

sealed interface WarpUpdateResult {
    data class NotFound(val name: String) : WarpUpdateResult
    data class Success(val warp: WarpData) : WarpUpdateResult
}

sealed interface WarpDeleteResult {
    data class NotFound(val name: String) : WarpDeleteResult
    data class Success(val warp: WarpData) : WarpDeleteResult
}
