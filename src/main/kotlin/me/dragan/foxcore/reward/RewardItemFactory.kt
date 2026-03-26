package me.dragan.foxcore.reward

import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.inventory.ItemStack
import java.util.concurrent.ConcurrentHashMap

class RewardItemFactory(
    private val plugin: FoxCorePlugin,
) {
    private val warnedIds = ConcurrentHashMap.newKeySet<String>()

    fun createTrackBaseItem(track: RewardTrack): ItemStack =
        createOraxenItem(track.oraxenItemId)
            ?: ItemStack(track.iconMaterial())

    fun createRewardBaseItem(reward: RewardEntry): ItemStack =
        createOraxenItem(reward.oraxenItemId)
            ?: ItemStack(reward.iconMaterial())

    private fun createOraxenItem(itemId: String?): ItemStack? {
        val normalizedId = itemId?.trim().orEmpty()
        if (normalizedId.isEmpty()) {
            return null
        }

        val oraxenPlugin = plugin.server.pluginManager.getPlugin("Oraxen") ?: return null
        return runCatching {
            val apiClass = Class.forName(
                "io.th0rgal.oraxen.api.OraxenItems",
                true,
                oraxenPlugin.javaClass.classLoader,
            )
            val getItemById = apiClass.getMethod("getItemById", String::class.java)
            val builder = getItemById.invoke(null, normalizedId) ?: return null
            val build = builder.javaClass.getMethod("build")
            (build.invoke(builder) as? ItemStack)?.clone()
        }.onFailure {
            if (warnedIds.add(normalizedId)) {
                plugin.logger.warning("Failed to resolve Oraxen reward item '$normalizedId'. Falling back to vanilla icon.")
            }
        }.getOrNull()
    }
}
