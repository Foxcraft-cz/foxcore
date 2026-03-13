package me.dragan.foxcore.portal

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.rtp.RtpResult
import me.dragan.foxcore.rtp.RtpStartResult
import me.dragan.foxcore.teleport.SafeTeleportResult
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.StringUtil
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom

class PortalService(
    private val plugin: FoxCorePlugin,
) {
    private val file: File
        get() = File(plugin.dataFolder, "portals.yml")

    private val wandKey = org.bukkit.NamespacedKey(plugin, "portal_wand")
    private val portals = LinkedHashMap<String, PortalDefinition>()
    private var portalsByWorld = emptyMap<String, List<PortalDefinition>>()
    private val selections = ConcurrentHashMap<UUID, PortalSelection>()
    private val currentInside = ConcurrentHashMap<UUID, Set<String>>()
    private val cooldowns = ConcurrentHashMap<Pair<UUID, String>, Long>()
    private val teleportImmunity = ConcurrentHashMap<UUID, Long>()
    private var particleTask: BukkitTask? = null

    init {
        reload()
    }

    fun reload() {
        load()
        restartParticles()
    }

    fun shutdown() {
        particleTask?.cancel()
        particleTask = null
        currentInside.clear()
        cooldowns.clear()
        teleportImmunity.clear()
        selections.clear()
    }

    fun isEnabled(): Boolean =
        plugin.config.getBoolean("portals.enabled", true)

    fun giveWand(player: Player) {
        val wand = ItemStack(org.bukkit.Material.BLAZE_ROD).apply {
            editMeta { meta ->
                meta.displayName(plugin.messages.text("command.portal.wand-name"))
                meta.lore(plugin.messages.lines("command.portal.wand-lore"))
                meta.persistentDataContainer.set(wandKey, PersistentDataType.BYTE, 1)
            }
        }

        val leftovers = player.inventory.addItem(wand)
        if (leftovers.isNotEmpty()) {
            leftovers.values.forEach { player.world.dropItemNaturally(player.location, it) }
        }
    }

    fun isPortalWand(item: ItemStack?): Boolean {
        val meta: ItemMeta = item?.itemMeta ?: return false
        return meta.persistentDataContainer.has(wandKey, PersistentDataType.BYTE)
    }

    fun setSelectionPos1(player: Player, location: Location) {
        selections.compute(player.uniqueId) { _, current ->
            (current ?: PortalSelection()).copy(pos1 = normalizeSelectionLocation(location))
        }
    }

    fun setSelectionPos2(player: Player, location: Location) {
        selections.compute(player.uniqueId) { _, current ->
            (current ?: PortalSelection()).copy(pos2 = normalizeSelectionLocation(location))
        }
    }

    fun selection(player: Player): PortalSelection =
        selections[player.uniqueId] ?: PortalSelection()

    fun clearTracking(playerId: UUID) {
        currentInside.remove(playerId)
        teleportImmunity.remove(playerId)
        cooldowns.keys.removeIf { it.first == playerId }
    }

    fun syncPlayerLocation(player: Player, location: Location?) {
        currentInside[player.uniqueId] = containingPortals(location).mapTo(LinkedHashSet()) { it.id }
    }

    fun portalIds(): List<String> =
        portals.keys.toList()

    fun portals(): List<PortalDefinition> =
        portals.values.toList()

    fun portal(id: String): PortalDefinition? =
        portals[PortalNames.normalize(id)]

    fun createPortal(player: Player, id: String): PortalCreateResult {
        val normalizedId = PortalNames.normalize(id)
        if (!PortalNames.isValid(normalizedId)) {
            return PortalCreateResult.InvalidId
        }
        if (portals.containsKey(normalizedId)) {
            return PortalCreateResult.AlreadyExists(normalizedId)
        }

        val selection = selection(player)
        val pos1 = selection.pos1 ?: return PortalCreateResult.MissingSelection
        val pos2 = selection.pos2 ?: return PortalCreateResult.MissingSelection
        val bounds = PortalBounds.fromCorners(pos1, pos2) ?: return PortalCreateResult.MixedWorldSelection
        val definition = PortalDefinition(
            id = normalizedId,
            bounds = bounds,
            action = PortalAction.Spawn,
        )
        portals[normalizedId] = definition
        rebuildWorldIndex()
        save()
        return PortalCreateResult.Success(definition)
    }

    fun redefinePortal(player: Player, id: String): PortalUpdateResult {
        val normalizedId = PortalNames.normalize(id)
        val existing = portals[normalizedId] ?: return PortalUpdateResult.NotFound(normalizedId)
        val selection = selection(player)
        val pos1 = selection.pos1 ?: return PortalUpdateResult.MissingSelection
        val pos2 = selection.pos2 ?: return PortalUpdateResult.MissingSelection
        val bounds = PortalBounds.fromCorners(pos1, pos2) ?: return PortalUpdateResult.MixedWorldSelection
        val updated = existing.copy(bounds = bounds)
        portals[normalizedId] = updated
        rebuildWorldIndex()
        save()
        return PortalUpdateResult.Success(updated)
    }

    fun deletePortal(id: String): PortalDeleteResult {
        val normalizedId = PortalNames.normalize(id)
        val removed = portals.remove(normalizedId) ?: return PortalDeleteResult.NotFound(normalizedId)
        rebuildWorldIndex()
        save()
        return PortalDeleteResult.Success(removed)
    }

    fun setEnabled(id: String, enabled: Boolean): PortalUpdateResult {
        val normalizedId = PortalNames.normalize(id)
        val existing = portals[normalizedId] ?: return PortalUpdateResult.NotFound(normalizedId)
        val updated = existing.copy(enabled = enabled)
        portals[normalizedId] = updated
        rebuildWorldIndex()
        save()
        return PortalUpdateResult.Success(updated)
    }

    fun setCooldown(id: String, cooldownSeconds: Long): PortalUpdateResult {
        val normalizedId = PortalNames.normalize(id)
        val existing = portals[normalizedId] ?: return PortalUpdateResult.NotFound(normalizedId)
        val updated = existing.copy(cooldownSeconds = cooldownSeconds.coerceAtLeast(0L))
        portals[normalizedId] = updated
        rebuildWorldIndex()
        save()
        return PortalUpdateResult.Success(updated)
    }

    fun setParticles(id: String, preset: PortalParticlePreset): PortalUpdateResult {
        val normalizedId = PortalNames.normalize(id)
        val existing = portals[normalizedId] ?: return PortalUpdateResult.NotFound(normalizedId)
        val updated = existing.copy(particlePreset = preset)
        portals[normalizedId] = updated
        rebuildWorldIndex()
        save()
        return PortalUpdateResult.Success(updated)
    }

    fun setAction(id: String, action: PortalAction): PortalUpdateResult {
        val normalizedId = PortalNames.normalize(id)
        val existing = portals[normalizedId] ?: return PortalUpdateResult.NotFound(normalizedId)
        val updated = existing.copy(action = action)
        portals[normalizedId] = updated
        rebuildWorldIndex()
        save()
        return PortalUpdateResult.Success(updated)
    }

    fun handleMove(player: Player, destination: Location?) {
        if (!isEnabled()) {
            currentInside.remove(player.uniqueId)
            return
        }

        val current = containingPortals(destination)
        val previous = currentInside[player.uniqueId].orEmpty()
        currentInside[player.uniqueId] = current.mapTo(LinkedHashSet()) { it.id }
        if (current.isEmpty()) {
            return
        }

        val immunityUntil = teleportImmunity[player.uniqueId] ?: 0L
        if (immunityUntil > System.currentTimeMillis()) {
            return
        }

        val entered = current.firstOrNull { it.id !in previous } ?: return
        val cooldownKey = player.uniqueId to entered.id
        val cooldownUntil = cooldowns[cooldownKey] ?: 0L
        if (cooldownUntil > System.currentTimeMillis()) {
            return
        }

        execute(player, entered)
    }

    fun matchPortalIds(prefix: String): List<String> =
        StringUtil.copyPartialMatches(prefix, portalIds(), mutableListOf()).sorted()

    private fun execute(player: Player, portal: PortalDefinition) {
        if (!portal.enabled) {
            return
        }

        when (val action = portal.action) {
            PortalAction.Spawn -> executeSpawn(player, portal)
            is PortalAction.Warp -> executeWarp(player, portal, action)
            is PortalAction.Rtp -> executeRtp(player, portal, action)
            is PortalAction.Command -> executeCommand(player, portal, action)
        }
    }

    private fun executeSpawn(player: Player, portal: PortalDefinition) {
        val spawn = plugin.spawnService.getSpawn()
        if (spawn == null) {
            player.sendMessage(plugin.messages.text("command.spawn.not-set"))
            return
        }

        when (plugin.safeTeleports.teleport(player, spawn)) {
            SafeTeleportResult.SUCCESS -> {
                markTriggered(player, portal)
                player.sendMessage(plugin.messages.text("command.spawn.success"))
            }

            SafeTeleportResult.NO_SAFE_GROUND -> player.sendMessage(plugin.messages.text("error.no-safe-ground"))
            SafeTeleportResult.FAILED -> player.sendMessage(plugin.messages.text("error.teleport-failed"))
        }
    }

    private fun executeWarp(player: Player, portal: PortalDefinition, action: PortalAction.Warp) {
        val warp = plugin.warps.getWarp(action.warpName) ?: run {
            player.sendMessage(plugin.messages.text("command.warp.not-found", "warp" to action.warpName))
            return
        }

        val cooldown = plugin.warps.remainingTeleportCooldownSeconds(player)
        if (cooldown > 0 && !player.hasPermission("foxcore.warp.bypasscooldown")) {
            player.sendMessage(plugin.messages.text("command.warp.cooldown", "seconds" to cooldown.toString()))
            return
        }

        val world = plugin.server.getWorld(warp.location.worldName)
        if (world == null) {
            player.sendMessage(plugin.messages.text("command.warp.missing-world", "warp" to warp.name, "world" to warp.location.worldName))
            return
        }

        when (plugin.safeTeleports.teleport(player, warp.location.toBukkitLocation(world))) {
            SafeTeleportResult.SUCCESS -> {
                plugin.warps.markTeleportUsed(player)
                markTriggered(player, portal)
                player.sendMessage(plugin.messages.text("command.warp.success", "warp" to warp.name))
            }

            SafeTeleportResult.NO_SAFE_GROUND -> player.sendMessage(plugin.messages.text("error.no-safe-ground"))
            SafeTeleportResult.FAILED -> player.sendMessage(plugin.messages.text("error.teleport-failed"))
        }
    }

    private fun executeRtp(player: Player, portal: PortalDefinition, action: PortalAction.Rtp) {
        when (val start = plugin.rtpService.beginTeleport(player, action.worldName) { result ->
            handleRtpCompletion(player, portal, result)
        }) {
            RtpStartResult.Started -> {
                player.sendMessage(plugin.messages.text("command.rtp.searching", "world" to action.worldName))
            }

            RtpStartResult.Disabled -> player.sendMessage(plugin.messages.text("command.rtp.disabled"))
            RtpStartResult.WorldDisabled -> player.sendMessage(plugin.messages.text("command.rtp.world-disabled", "world" to action.worldName))
            RtpStartResult.WorldUnavailable -> player.sendMessage(plugin.messages.text("command.rtp.world-unavailable", "world" to action.worldName))
            RtpStartResult.AlreadySearching -> player.sendMessage(plugin.messages.text("command.rtp.already-searching"))
            is RtpStartResult.Cooldown -> {
                player.sendMessage(plugin.messages.text("command.rtp.cooldown", "seconds" to start.remainingSeconds.toString()))
            }
        }
    }

    private fun handleRtpCompletion(player: Player, portal: PortalDefinition, result: RtpResult) {
        if (!player.isOnline) {
            return
        }

        when (result) {
            is RtpResult.Success -> {
                markTriggered(player, portal)
                player.sendMessage(
                    plugin.messages.text(
                        "command.rtp.success",
                        "x" to result.location.blockX.toString(),
                        "y" to result.location.blockY.toString(),
                        "z" to result.location.blockZ.toString(),
                        "world" to requireNotNull(result.location.world).name,
                    ),
                )
            }

            RtpResult.NoLocationFound -> player.sendMessage(plugin.messages.text("command.rtp.failed"))
            RtpResult.Failed -> player.sendMessage(plugin.messages.text("error.teleport-failed"))
            RtpResult.Cancelled -> Unit
        }
    }

    private fun executeCommand(player: Player, portal: PortalDefinition, action: PortalAction.Command) {
        val command = action.command.trim().removePrefix("/")
        if (command.isEmpty()) {
            player.sendMessage(plugin.messages.text("command.portal.invalid-command"))
            return
        }

        val resolved = command.replace("%player%", player.name)
        plugin.server.dispatchCommand(plugin.server.consoleSender, resolved)
        markTriggered(player, portal)
    }

    private fun markTriggered(player: Player, portal: PortalDefinition) {
        val now = System.currentTimeMillis()
        cooldowns[player.uniqueId to portal.id] = now + (portal.cooldownSeconds * 1000L)
        teleportImmunity[player.uniqueId] = now + (plugin.config.getLong("portals.teleport-immunity-seconds", 2L).coerceAtLeast(0L) * 1000L)
        currentInside[player.uniqueId] = emptySet()
    }

    private fun containingPortals(location: Location?): List<PortalDefinition> {
        val worldName = location?.world?.name ?: return emptyList()
        val worldPortals = portalsByWorld[worldName.lowercase()].orEmpty()
        return worldPortals.filter { it.enabled && it.bounds.contains(location) }
    }

    private fun restartParticles() {
        particleTask?.cancel()
        particleTask = null
        if (!isEnabled()) {
            return
        }

        val interval = plugin.config.getLong("portals.particles.interval-ticks", 10L).coerceAtLeast(1L)
        particleTask = plugin.server.scheduler.runTaskTimer(
            plugin,
            Runnable { renderParticles() },
            interval,
            interval,
        )
    }

    private fun renderParticles() {
        val viewDistance = plugin.config.getDouble("portals.particles.view-distance-blocks", 24.0).coerceAtLeast(1.0)
        val maxDistanceSquared = viewDistance * viewDistance
        val random = ThreadLocalRandom.current()

        plugin.server.onlinePlayers.forEach { player ->
            val worldPortals = portalsByWorld[player.world.name.lowercase()].orEmpty()
            if (worldPortals.isEmpty()) {
                return@forEach
            }

            worldPortals.forEach { portal ->
                val centerX = portal.bounds.centerX()
                val centerY = portal.bounds.centerY()
                val centerZ = portal.bounds.centerZ()
                val dx = player.location.x - centerX
                val dy = player.location.y - centerY
                val dz = player.location.z - centerZ
                if ((dx * dx) + (dy * dy) + (dz * dz) > maxDistanceSquared) {
                    return@forEach
                }

                repeat(interiorParticleCount(portal)) {
                    portal.particlePreset.spawnInterior(player, portal.bounds.randomInteriorPoint(random), random)
                }

                repeat(frameParticleCount(portal)) {
                    portal.particlePreset.spawnFrame(player, portal.bounds.randomFramePoint(random), random)
                }
            }
        }
    }

    private fun interiorParticleCount(portal: PortalDefinition): Int {
        val area = when (portal.bounds.preferredPlane()) {
            PortalPlane.YZ -> ((portal.bounds.maxY - portal.bounds.minY) + 1) * ((portal.bounds.maxZ - portal.bounds.minZ) + 1)
            PortalPlane.XY -> ((portal.bounds.maxX - portal.bounds.minX) + 1) * ((portal.bounds.maxY - portal.bounds.minY) + 1)
            PortalPlane.XZ -> ((portal.bounds.maxX - portal.bounds.minX) + 1) * ((portal.bounds.maxZ - portal.bounds.minZ) + 1)
        }
        return (area / 3).coerceIn(4, 16)
    }

    private fun frameParticleCount(portal: PortalDefinition): Int =
        when (portal.particlePreset) {
            PortalParticlePreset.END -> 3
            else -> 4
        }

    private fun load() {
        portals.clear()
        if (!file.exists()) {
            save()
        }
        val config = YamlConfiguration.loadConfiguration(file)
        val section = config.getConfigurationSection("portals") ?: run {
            rebuildWorldIndex()
            return
        }

        section.getKeys(false).forEach { id ->
            val portalSection = section.getConfigurationSection(id) ?: return@forEach
            val actionType = portalSection.getString("action.type")?.trim()?.lowercase() ?: return@forEach
            val actionValue = portalSection.getString("action.value")?.trim().orEmpty()
            val action = when (actionType) {
                "spawn" -> PortalAction.Spawn
                "warp" -> actionValue.takeIf(String::isNotEmpty)?.let(PortalAction::Warp)
                "rtp" -> actionValue.takeIf(String::isNotEmpty)?.let(PortalAction::Rtp)
                "command" -> actionValue.takeIf(String::isNotEmpty)?.let(PortalAction::Command)
                else -> null
            } ?: return@forEach

            val worldName = portalSection.getString("world")?.trim().orEmpty()
            if (worldName.isEmpty()) {
                return@forEach
            }

            portals[PortalNames.normalize(id)] = PortalDefinition(
                id = PortalNames.normalize(id),
                bounds = PortalBounds(
                    worldName = worldName,
                    minX = portalSection.getInt("pos1.x").coerceAtMost(portalSection.getInt("pos2.x")),
                    minY = portalSection.getInt("pos1.y").coerceAtMost(portalSection.getInt("pos2.y")),
                    minZ = portalSection.getInt("pos1.z").coerceAtMost(portalSection.getInt("pos2.z")),
                    maxX = portalSection.getInt("pos1.x").coerceAtLeast(portalSection.getInt("pos2.x")),
                    maxY = portalSection.getInt("pos1.y").coerceAtLeast(portalSection.getInt("pos2.y")),
                    maxZ = portalSection.getInt("pos1.z").coerceAtLeast(portalSection.getInt("pos2.z")),
                ),
                action = action,
                enabled = portalSection.getBoolean("enabled", true),
                cooldownSeconds = portalSection.getLong("cooldown-seconds", 2L).coerceAtLeast(0L),
                particlePreset = PortalParticlePreset.fromKey(portalSection.getString("particles.preset")) ?: PortalParticlePreset.GOLD,
            )
        }

        rebuildWorldIndex()
    }

    private fun save() {
        val config = YamlConfiguration()
        portals.values.forEach { portal ->
            val root = "portals.${portal.id}"
            config.set("$root.enabled", portal.enabled)
            config.set("$root.world", portal.bounds.worldName)
            config.set("$root.pos1.x", portal.bounds.minX)
            config.set("$root.pos1.y", portal.bounds.minY)
            config.set("$root.pos1.z", portal.bounds.minZ)
            config.set("$root.pos2.x", portal.bounds.maxX)
            config.set("$root.pos2.y", portal.bounds.maxY)
            config.set("$root.pos2.z", portal.bounds.maxZ)
            config.set("$root.cooldown-seconds", portal.cooldownSeconds)
            config.set("$root.particles.preset", portal.particlePreset.key)
            when (val action = portal.action) {
                PortalAction.Spawn -> {
                    config.set("$root.action.type", "spawn")
                    config.set("$root.action.value", null)
                }

                is PortalAction.Warp -> {
                    config.set("$root.action.type", "warp")
                    config.set("$root.action.value", action.warpName)
                }

                is PortalAction.Rtp -> {
                    config.set("$root.action.type", "rtp")
                    config.set("$root.action.value", action.worldName)
                }

                is PortalAction.Command -> {
                    config.set("$root.action.type", "command")
                    config.set("$root.action.value", action.command)
                }
            }
        }
        config.save(file)
    }

    private fun rebuildWorldIndex() {
        portalsByWorld = portals.values
            .sortedBy(PortalDefinition::id)
            .groupBy { it.bounds.worldName.lowercase() }
    }

    private fun normalizeSelectionLocation(location: Location): Location =
        Location(
            location.world,
            location.blockX.toDouble(),
            location.blockY.toDouble(),
            location.blockZ.toDouble(),
        )
}

data class PortalSelection(
    val pos1: Location? = null,
    val pos2: Location? = null,
)

sealed interface PortalCreateResult {
    data object InvalidId : PortalCreateResult
    data object MissingSelection : PortalCreateResult
    data object MixedWorldSelection : PortalCreateResult
    data class AlreadyExists(val id: String) : PortalCreateResult
    data class Success(val portal: PortalDefinition) : PortalCreateResult
}

sealed interface PortalUpdateResult {
    data object MissingSelection : PortalUpdateResult
    data object MixedWorldSelection : PortalUpdateResult
    data class NotFound(val id: String) : PortalUpdateResult
    data class Success(val portal: PortalDefinition) : PortalUpdateResult
}

sealed interface PortalDeleteResult {
    data class NotFound(val id: String) : PortalDeleteResult
    data class Success(val portal: PortalDefinition) : PortalDeleteResult
}
