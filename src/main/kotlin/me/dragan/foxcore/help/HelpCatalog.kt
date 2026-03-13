package me.dragan.foxcore.help

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.back.BackType
import me.dragan.foxcore.home.HomeBrowseResult
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionAttachmentInfo

object HelpCatalog {
    val entries = listOf(
        HelpEntry("spawn", HelpCategory.TELEPORT, Material.BEACON, { it.hasPermission("foxcore.spawn") }) { plugin, _ ->
            listOf(
                if (plugin.spawnService.getSpawn() != null) "help.dynamic.spawn.available" else "help.dynamic.spawn.unset",
            )
        },
        HelpEntry("back", HelpCategory.TELEPORT, Material.RECOVERY_COMPASS, { canUseBack(it) }) { _, player ->
            val types = buildList {
                if (player.hasPermission("foxcore.back.teleport")) add(BackType.TELEPORT)
                if (player.hasPermission("foxcore.back.death")) add(BackType.DEATH)
            }
            listOf(
                "help.dynamic.back.types:${types.joinToString(", ") { it.name.lowercase() }}",
            )
        },
        HelpEntry("tp", HelpCategory.TELEPORT, Material.ENDER_EYE, { it.hasPermission("foxcore.tp") }),
        HelpEntry("tphere", HelpCategory.TELEPORT, Material.CHORUS_FRUIT, { it.hasPermission("foxcore.tphere") }),
        HelpEntry("tpa", HelpCategory.TELEPORT, Material.PAPER, { it.hasPermission("foxcore.tpa") }) { plugin, _ ->
            listOf("help.dynamic.tpa.expiration:${plugin.config.getLong("tpa.request-expiration-seconds", 60L)}")
        },
        HelpEntry("tpahere", HelpCategory.TELEPORT, Material.WRITABLE_BOOK, { it.hasPermission("foxcore.tpahere") }) { plugin, _ ->
            listOf("help.dynamic.tpa.expiration:${plugin.config.getLong("tpa.request-expiration-seconds", 60L)}")
        },
        HelpEntry("tpaccept", HelpCategory.TELEPORT, Material.LIME_CONCRETE, { it.hasPermission("foxcore.tpaccept") }),
        HelpEntry("tpadeny", HelpCategory.TELEPORT, Material.RED_CONCRETE, { it.hasPermission("foxcore.tpadeny") }),
        HelpEntry("rtp", HelpCategory.TELEPORT, Material.ENDER_PEARL, { it.hasPermission("foxcore.rtp") }) { plugin, _ ->
            listOf("help.dynamic.rtp.worlds:${plugin.rtpService.availableWorlds().size}")
        },

        HelpEntry("home", HelpCategory.HOMES, Material.RED_BED, { it.hasPermission("foxcore.home") }, ::homesLore),
        HelpEntry("homes", HelpCategory.HOMES, Material.CHEST, { it.hasPermission("foxcore.homes") }, ::homesLore),
        HelpEntry("sethome", HelpCategory.HOMES, Material.NAME_TAG, { it.hasPermission("foxcore.sethome") }, ::homesLore),
        HelpEntry("renamehome", HelpCategory.HOMES, Material.OAK_SIGN, { it.hasPermission("foxcore.renamehome") }),
        HelpEntry("sethomeicon", HelpCategory.HOMES, Material.ITEM_FRAME, { it.hasPermission("foxcore.sethomeicon") }),
        HelpEntry("delhome", HelpCategory.HOMES, Material.BARRIER, { it.hasPermission("foxcore.delhome") }),

        HelpEntry("warp", HelpCategory.WARPS, Material.COMPASS, { it.hasPermission("foxcore.warp") }) { plugin, _ ->
            listOf("help.dynamic.warp.count:${plugin.warps.listWarps().size}")
        },

        HelpEntry("afk", HelpCategory.UTILITY, Material.CLOCK, { it.hasPermission("foxcore.afk.command") }),
        HelpEntry("onlinetime", HelpCategory.UTILITY, Material.CLOCK, { it.hasPermission("foxcore.onlinetime") }),
        HelpEntry("hat", HelpCategory.UTILITY, Material.LEATHER_HELMET, { it.hasPermission("foxcore.hat") }),
        HelpEntry("head", HelpCategory.UTILITY, Material.PLAYER_HEAD, { it.hasPermission("foxcore.head") }),
        HelpEntry("craft", HelpCategory.UTILITY, Material.CRAFTING_TABLE, { it.hasPermission("foxcore.craft") }),
        HelpEntry("enderchest", HelpCategory.UTILITY, Material.ENDER_CHEST, { it.hasPermission("foxcore.enderchest") }),
        HelpEntry("anvil", HelpCategory.UTILITY, Material.ANVIL, { it.hasPermission("foxcore.anvil") }),
        HelpEntry("cartographytable", HelpCategory.UTILITY, Material.CARTOGRAPHY_TABLE, { it.hasPermission("foxcore.cartographytable") }),
        HelpEntry("loom", HelpCategory.UTILITY, Material.LOOM, { it.hasPermission("foxcore.loom") }),
        HelpEntry("grindstone", HelpCategory.UTILITY, Material.GRINDSTONE, { it.hasPermission("foxcore.grindstone") }),
        HelpEntry("smithingtable", HelpCategory.UTILITY, Material.SMITHING_TABLE, { it.hasPermission("foxcore.smithingtable") }),
        HelpEntry("stonecutter", HelpCategory.UTILITY, Material.STONECUTTER, { it.hasPermission("foxcore.stonecutter") }),
    )

    fun visibleEntries(plugin: FoxCorePlugin, player: Player): List<HelpEntry> =
        entries.filter { it.visibleTo(player) }

    private fun canUseBack(player: Player): Boolean =
        player.hasPermission("foxcore.back.teleport") || player.hasPermission("foxcore.back.death")

    private fun homesLore(plugin: FoxCorePlugin, player: Player): List<String> {
        val maxHomes = resolveMaxHomes(plugin, player)
        val currentHomes = when (val result = plugin.backService.browseHomes(player)) {
            HomeBrowseResult.Loading -> return listOf("help.dynamic.homes.loading")
            is HomeBrowseResult.Empty -> 0
            is HomeBrowseResult.Success -> result.homes.size
            is HomeBrowseResult.NotFound -> 0
        }

        return buildList {
            add(
                if (maxHomes == Int.MAX_VALUE) {
                    "help.dynamic.homes.limit.unlimited"
                } else {
                    "help.dynamic.homes.limit:$maxHomes"
                },
            )
            add("help.dynamic.homes.current:$currentHomes")
        }
    }

    private fun resolveMaxHomes(plugin: FoxCorePlugin, player: Player): Int {
        if (player.hasPermission("foxcore.sethome.limit.unlimited")) {
            return Int.MAX_VALUE
        }

        val permissionMax = player.effectivePermissions
            .asSequence()
            .filter(PermissionAttachmentInfo::getValue)
            .map { it.permission.lowercase() }
            .filter { it.startsWith("foxcore.sethome.limit.") }
            .mapNotNull { it.removePrefix("foxcore.sethome.limit.").toIntOrNull() }
            .filter { it >= 0 }
            .maxOrNull()

        val configMax = plugin.config.getInt("homes.default-max-count", 1).coerceAtLeast(0)
        return permissionMax ?: configMax
    }
}
