package me.dragan.foxcore.help

import org.bukkit.Material

enum class HelpCategory(
    val key: String,
    val icon: Material,
) {
    TELEPORT("teleport", Material.ENDER_PEARL),
    HOMES("homes", Material.RED_BED),
    WARPS("warps", Material.COMPASS),
    UTILITY("utility", Material.BOOK),
}
