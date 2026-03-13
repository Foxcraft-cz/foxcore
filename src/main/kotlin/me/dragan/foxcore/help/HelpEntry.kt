package me.dragan.foxcore.help

import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.Material
import org.bukkit.entity.Player

data class HelpEntry(
    val key: String,
    val category: HelpCategory,
    val icon: Material,
    val visibleTo: (FoxCorePlugin, Player) -> Boolean,
    val dynamicLore: (FoxCorePlugin, Player) -> List<String> = { _, _ -> emptyList() },
)
