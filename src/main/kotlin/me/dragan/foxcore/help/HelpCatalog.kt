package me.dragan.foxcore.help

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.back.BackType
import me.dragan.foxcore.home.HomeBrowseResult
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionAttachmentInfo

object HelpCatalog {
    val entries = listOf(
        HelpEntry("spawn", HelpCategory.TELEPORT, Material.BEACON, { _, player -> player.hasPermission("foxcore.spawn") }) { plugin, _ ->
            listOf(
                if (plugin.spawnService.getSpawn() != null) "help.dynamic.spawn.available" else "help.dynamic.spawn.unset",
            )
        },
        HelpEntry("back", HelpCategory.TELEPORT, Material.RECOVERY_COMPASS, { _, player -> canUseBack(player) }) { _, player ->
            val types = buildList {
                if (player.hasPermission("foxcore.back.teleport")) add(BackType.TELEPORT)
                if (player.hasPermission("foxcore.back.death")) add(BackType.DEATH)
            }
            listOf(
                "help.dynamic.back.types:${types.joinToString(", ") { it.name.lowercase() }}",
            )
        },
        HelpEntry("tp", HelpCategory.TELEPORT, Material.ENDER_EYE, { _, player -> player.hasPermission("foxcore.tp") }),
        HelpEntry("tphere", HelpCategory.TELEPORT, Material.CHORUS_FRUIT, { _, player -> player.hasPermission("foxcore.tphere") }),
        HelpEntry("tpa", HelpCategory.TELEPORT, Material.PAPER, { _, player -> player.hasPermission("foxcore.tpa") }) { plugin, _ ->
            listOf("help.dynamic.tpa.expiration:${plugin.config.getLong("tpa.request-expiration-seconds", 60L)}")
        },
        HelpEntry("tpahere", HelpCategory.TELEPORT, Material.WRITABLE_BOOK, { _, player -> player.hasPermission("foxcore.tpahere") }) { plugin, _ ->
            listOf("help.dynamic.tpa.expiration:${plugin.config.getLong("tpa.request-expiration-seconds", 60L)}")
        },
        HelpEntry("tpaccept", HelpCategory.TELEPORT, Material.LIME_CONCRETE, { _, player -> player.hasPermission("foxcore.tpaccept") }),
        HelpEntry("tpadeny", HelpCategory.TELEPORT, Material.RED_CONCRETE, { _, player -> player.hasPermission("foxcore.tpadeny") }),
        HelpEntry("rtp", HelpCategory.TELEPORT, Material.ENDER_PEARL, { _, player -> player.hasPermission("foxcore.rtp") }) { plugin, _ ->
            listOf("help.dynamic.rtp.worlds:${plugin.rtpService.availableWorlds().size}")
        },

        HelpEntry("home", HelpCategory.HOMES, Material.RED_BED, { _, player -> player.hasPermission("foxcore.home") }, ::homesLore),
        HelpEntry("homes", HelpCategory.HOMES, Material.CHEST, { _, player -> player.hasPermission("foxcore.homes") }, ::homesLore),
        HelpEntry("sethome", HelpCategory.HOMES, Material.NAME_TAG, { _, player -> player.hasPermission("foxcore.sethome") }, ::homesLore),
        HelpEntry("renamehome", HelpCategory.HOMES, Material.OAK_SIGN, { _, player -> player.hasPermission("foxcore.renamehome") }),
        HelpEntry("sethomeicon", HelpCategory.HOMES, Material.ITEM_FRAME, { _, player -> player.hasPermission("foxcore.sethomeicon") }),
        HelpEntry("delhome", HelpCategory.HOMES, Material.BARRIER, { _, player -> player.hasPermission("foxcore.delhome") }),
        HelpEntry("residence", HelpCategory.HOMES, Material.BRICKS, { plugin, _ -> plugin.residenceHelpInfo.isAvailable() }) { plugin, player ->
            buildList {
                add(
                    plugin.residenceHelpInfo.maxResidences(player)
                        ?.let { "help.dynamic.residence.max-count:$it" }
                        ?: "help.dynamic.residence.max-count-unavailable",
                )
                add(
                    plugin.residenceHelpInfo.maxSize(player)
                        ?.let { "help.dynamic.residence.max-size:$it" }
                        ?: "help.dynamic.residence.max-size-unavailable",
                )
            }
        },

        HelpEntry("warp", HelpCategory.WARPS, Material.COMPASS, { _, player -> player.hasPermission("foxcore.warp") }) { plugin, _ ->
            listOf("help.dynamic.warp.count:${plugin.warps.listWarps().size}")
        },

        HelpEntry("vote", HelpCategory.FEATURES, Material.EMERALD, { plugin, _ ->
            plugin.pluginHelpInfo.isPluginLoaded("help.integrations.vote")
        }) { plugin, player ->
            buildList {
                add(
                    plugin.pluginHelpInfo.resolveFirst(player, "help.integrations.vote.pending-placeholders")
                        ?.let { "help.dynamic.vote.pending:$it" }
                        ?: "help.dynamic.vote.pending-unavailable",
                )
            }
        },
        HelpEntry("kits", HelpCategory.FEATURES, Material.CHEST_MINECART, { plugin, _ ->
            plugin.pluginHelpInfo.isPluginLoaded("help.integrations.kits")
        }) { plugin, player ->
            buildList {
                add(
                    plugin.pluginHelpInfo.resolveFirst(player, "help.integrations.kits.available-placeholders")
                        ?.let { "help.dynamic.kits.available:$it" }
                        ?: "help.dynamic.kits.available-unavailable",
                )
            }
        },
        HelpEntry("skins", HelpCategory.FEATURES, Material.PLAYER_HEAD, { plugin, _ ->
            plugin.pluginHelpInfo.isPluginLoaded("help.integrations.skinsrestorer")
        }),
        HelpEntry("bannermaker", HelpCategory.FEATURES, Material.WHITE_BANNER, { plugin, _ ->
            plugin.pluginHelpInfo.isPluginLoaded("help.integrations.bannermaker")
        }),
        HelpEntry("armorstandeditor", HelpCategory.FEATURES, Material.ARMOR_STAND, { plugin, _ ->
            plugin.pluginHelpInfo.isPluginLoaded("help.integrations.armorstandeditor")
        }),
        HelpEntry("rosetimber", HelpCategory.FEATURES, Material.IRON_AXE, { plugin, _ ->
            plugin.pluginHelpInfo.isPluginLoaded("help.integrations.rosetimber")
        }),
        HelpEntry("interactivechat", HelpCategory.FEATURES, Material.BOOK, { plugin, _ ->
            plugin.pluginHelpInfo.isPluginLoaded("help.integrations.interactivechat")
        }),
        HelpEntry("trade", HelpCategory.FEATURES, Material.EMERALD, { plugin, _ ->
            plugin.pluginHelpInfo.isPluginLoaded("help.integrations.axtrade")
        }),

        HelpEntry("afk", HelpCategory.UTILITY, Material.CLOCK, { _, player -> player.hasPermission("foxcore.afk.command") }),
        HelpEntry("enchant", HelpCategory.UTILITY, Material.ENCHANTED_BOOK, { _, player -> player.hasPermission("foxcore.enchant") }),
        HelpEntry("feed", HelpCategory.UTILITY, Material.COOKED_BEEF, { _, player -> player.hasPermission("foxcore.feed") }),
        HelpEntry("fix", HelpCategory.UTILITY, Material.IRON_PICKAXE, { _, player -> player.hasPermission("foxcore.fix") }),
        HelpEntry("fixall", HelpCategory.UTILITY, Material.ANVIL, { _, player -> player.hasPermission("foxcore.fixall") }),
        HelpEntry("heal", HelpCategory.UTILITY, Material.GLISTERING_MELON_SLICE, { _, player -> player.hasPermission("foxcore.heal") }),
        HelpEntry("item", HelpCategory.UTILITY, Material.CHEST, { _, player -> player.hasPermission("foxcore.item") }),
        HelpEntry("itemname", HelpCategory.UTILITY, Material.NAME_TAG, { _, player -> player.hasPermission("foxcore.itemname") }),
        HelpEntry("description", HelpCategory.UTILITY, Material.PAPER, { _, player -> player.hasPermission("foxcore.description") }),
        HelpEntry("message", HelpCategory.UTILITY, Material.WRITABLE_BOOK, { _, player -> player.hasPermission("foxcore.message") }),
        HelpEntry("reply", HelpCategory.UTILITY, Material.PAPER, { _, player -> player.hasPermission("foxcore.reply") }),
        HelpEntry("report", HelpCategory.UTILITY, Material.PAPER, { _, player -> player.hasPermission("foxcore.report.create") }),
        HelpEntry("socialspy", HelpCategory.UTILITY, Material.ENDER_EYE, { _, player -> player.hasPermission("foxcore.socialspy") }),
        HelpEntry("commandspy", HelpCategory.UTILITY, Material.COMMAND_BLOCK, { _, player -> player.hasPermission("foxcore.commandspy") }),
        HelpEntry("voteday", HelpCategory.UTILITY, Material.CLOCK, { _, player -> player.hasPermission("foxcore.voteday") }),
        HelpEntry("votenight", HelpCategory.UTILITY, Material.BLACK_BED, { _, player -> player.hasPermission("foxcore.votenight") }),
        HelpEntry("votesun", HelpCategory.UTILITY, Material.SUNFLOWER, { _, player -> player.hasPermission("foxcore.votesun") }),
        HelpEntry("voterain", HelpCategory.UTILITY, Material.WATER_BUCKET, { _, player -> player.hasPermission("foxcore.voterain") }),
        HelpEntry("voteyes", HelpCategory.UTILITY, Material.LIME_CONCRETE, { _, _ -> true }),
        HelpEntry("voteno", HelpCategory.UTILITY, Material.RED_CONCRETE, { _, _ -> true }),
        HelpEntry("onlinetime", HelpCategory.UTILITY, Material.CLOCK, { _, player -> player.hasPermission("foxcore.onlinetime") }),
        HelpEntry("hat", HelpCategory.UTILITY, Material.LEATHER_HELMET, { _, player -> player.hasPermission("foxcore.hat") }),
        HelpEntry("head", HelpCategory.UTILITY, Material.PLAYER_HEAD, { _, player -> player.hasPermission("foxcore.head") }),
        HelpEntry("dispose", HelpCategory.UTILITY, Material.LAVA_BUCKET, { _, player -> player.hasPermission("foxcore.dispose") }),
        HelpEntry("craft", HelpCategory.UTILITY, Material.CRAFTING_TABLE, { _, player -> player.hasPermission("foxcore.craft") }),
        HelpEntry("enderchest", HelpCategory.UTILITY, Material.ENDER_CHEST, { _, player -> player.hasPermission("foxcore.enderchest") }),
        HelpEntry("anvil", HelpCategory.UTILITY, Material.ANVIL, { _, player -> player.hasPermission("foxcore.anvil") }),
        HelpEntry("cartographytable", HelpCategory.UTILITY, Material.CARTOGRAPHY_TABLE, { _, player -> player.hasPermission("foxcore.cartographytable") }),
        HelpEntry("loom", HelpCategory.UTILITY, Material.LOOM, { _, player -> player.hasPermission("foxcore.loom") }),
        HelpEntry("grindstone", HelpCategory.UTILITY, Material.GRINDSTONE, { _, player -> player.hasPermission("foxcore.grindstone") }),
        HelpEntry("smithingtable", HelpCategory.UTILITY, Material.SMITHING_TABLE, { _, player -> player.hasPermission("foxcore.smithingtable") }),
        HelpEntry("stonecutter", HelpCategory.UTILITY, Material.STONECUTTER, { _, player -> player.hasPermission("foxcore.stonecutter") }),
    )

    fun visibleEntries(plugin: FoxCorePlugin, player: Player): List<HelpEntry> =
        (entries + plugin.shortcuts.helpEntries())
            .filter { it.visibleTo(plugin, player) }

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
