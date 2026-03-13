package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.portal.PortalAction
import me.dragan.foxcore.portal.PortalCreateResult
import me.dragan.foxcore.portal.PortalDeleteResult
import me.dragan.foxcore.portal.PortalNames
import me.dragan.foxcore.portal.PortalParticlePreset
import me.dragan.foxcore.portal.PortalUpdateResult
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import org.bukkit.util.StringUtil

class PortalCommand(
    private val plugin: FoxCorePlugin,
) : TabExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        val player = sender as? Player ?: run {
            sender.sendMessage(plugin.messages.text("error.player-only"))
            return true
        }

        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        if (args.isEmpty()) {
            player.sendMessage(plugin.messages.text("command.portal.usage"))
            return true
        }

        return when (args[0].lowercase()) {
            "wand" -> giveWand(player)
            "pos1" -> setPos1(player)
            "pos2" -> setPos2(player)
            "create" -> create(player, args)
            "redefine" -> redefine(player, args)
            "setaction" -> setAction(player, args)
            "setcooldown" -> setCooldown(player, args)
            "setparticles" -> setParticles(player, args)
            "enable" -> setEnabled(player, args, true)
            "disable" -> setEnabled(player, args, false)
            "delete" -> delete(player, args)
            "info" -> info(player, args)
            "list" -> list(player)
            else -> {
                player.sendMessage(plugin.messages.text("command.portal.usage"))
                true
            }
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> {
        if (!sender.hasPermission(PERMISSION)) {
            return emptyList()
        }

        return when (args.size) {
            1 -> StringUtil.copyPartialMatches(
                args[0],
                listOf("wand", "pos1", "pos2", "create", "redefine", "setaction", "setcooldown", "setparticles", "enable", "disable", "delete", "info", "list"),
                mutableListOf(),
            ).sorted()

            2 -> when (args[0].lowercase()) {
                "redefine", "setaction", "setcooldown", "setparticles", "enable", "disable", "delete", "info" ->
                    plugin.portals.matchPortalIds(args[1])
                else -> emptyList()
            }

            3 -> when (args[0].lowercase()) {
                "setaction" -> StringUtil.copyPartialMatches(args[2], listOf("spawn", "warp", "rtp", "command"), mutableListOf()).sorted()
                "setparticles" -> StringUtil.copyPartialMatches(
                    args[2],
                    PortalParticlePreset.entries.map(PortalParticlePreset::key),
                    mutableListOf(),
                ).sorted()
                else -> emptyList()
            }

            4 -> when (args[0].lowercase()) {
                "setaction" -> when (args[2].lowercase()) {
                    "warp" -> StringUtil.copyPartialMatches(
                        args[3],
                        plugin.warps.listWarps().map { it.name },
                        mutableListOf(),
                    ).sorted()

                    "rtp" -> StringUtil.copyPartialMatches(
                        args[3],
                        plugin.rtpService.availableWorlds().map { it.worldName },
                        mutableListOf(),
                    ).sorted()

                    else -> emptyList()
                }

                else -> emptyList()
            }

            else -> emptyList()
        }
    }

    private fun giveWand(player: Player): Boolean {
        plugin.portals.giveWand(player)
        player.sendMessage(plugin.messages.text("command.portal.wand-given"))
        return true
    }

    private fun setPos1(player: Player): Boolean {
        plugin.portals.setSelectionPos1(player, player.location)
        player.sendMessage(
            plugin.messages.text(
                "command.portal.pos1-set",
                "world" to player.world.name,
                "x" to player.location.blockX.toString(),
                "y" to player.location.blockY.toString(),
                "z" to player.location.blockZ.toString(),
            ),
        )
        return true
    }

    private fun setPos2(player: Player): Boolean {
        plugin.portals.setSelectionPos2(player, player.location)
        player.sendMessage(
            plugin.messages.text(
                "command.portal.pos2-set",
                "world" to player.world.name,
                "x" to player.location.blockX.toString(),
                "y" to player.location.blockY.toString(),
                "z" to player.location.blockZ.toString(),
            ),
        )
        return true
    }

    private fun create(player: Player, args: Array<out String>): Boolean {
        if (args.size != 2) {
            player.sendMessage(plugin.messages.text("command.portal.create-usage"))
            return true
        }

        return when (val result = plugin.portals.createPortal(player, args[1])) {
            PortalCreateResult.InvalidId -> {
                player.sendMessage(plugin.messages.text("command.portal.invalid-id"))
                true
            }

            PortalCreateResult.MissingSelection -> {
                player.sendMessage(plugin.messages.text("command.portal.selection-missing"))
                true
            }

            PortalCreateResult.MixedWorldSelection -> {
                player.sendMessage(plugin.messages.text("command.portal.selection-world-mismatch"))
                true
            }

            is PortalCreateResult.AlreadyExists -> {
                player.sendMessage(plugin.messages.text("command.portal.already-exists", "portal" to result.id))
                true
            }

            is PortalCreateResult.Success -> {
                player.sendMessage(plugin.messages.text("command.portal.created", "portal" to result.portal.id))
                true
            }
        }
    }

    private fun redefine(player: Player, args: Array<out String>): Boolean {
        if (args.size != 2) {
            player.sendMessage(plugin.messages.text("command.portal.redefine-usage"))
            return true
        }

        return handleUpdate(player, plugin.portals.redefinePortal(player, args[1])) {
            player.sendMessage(plugin.messages.text("command.portal.redefined", "portal" to it.id))
        }
    }

    private fun setAction(player: Player, args: Array<out String>): Boolean {
        if (args.size < 3) {
            player.sendMessage(plugin.messages.text("command.portal.setaction-usage"))
            return true
        }

        val action = when (args[2].lowercase()) {
            "spawn" -> {
                if (args.size != 3) {
                    player.sendMessage(plugin.messages.text("command.portal.setaction-spawn-usage"))
                    return true
                }
                PortalAction.Spawn
            }

            "warp" -> {
                if (args.size != 4) {
                    player.sendMessage(plugin.messages.text("command.portal.setaction-warp-usage"))
                    return true
                }
                PortalAction.Warp(PortalNames.normalize(args[3]))
            }

            "rtp" -> {
                if (args.size != 4) {
                    player.sendMessage(plugin.messages.text("command.portal.setaction-rtp-usage"))
                    return true
                }
                PortalAction.Rtp(args[3])
            }

            "command" -> {
                if (args.size < 4) {
                    player.sendMessage(plugin.messages.text("command.portal.setaction-command-usage"))
                    return true
                }
                PortalAction.Command(args.drop(3).joinToString(" "))
            }

            else -> {
                player.sendMessage(plugin.messages.text("command.portal.setaction-usage"))
                return true
            }
        }

        return handleUpdate(player, plugin.portals.setAction(args[1], action)) {
            player.sendMessage(
                plugin.messages.text(
                    "command.portal.action-set",
                    "portal" to it.id,
                    "action" to describeAction(it.action),
                ),
            )
        }
    }

    private fun setCooldown(player: Player, args: Array<out String>): Boolean {
        if (args.size != 3) {
            player.sendMessage(plugin.messages.text("command.portal.setcooldown-usage"))
            return true
        }

        val seconds = args[2].toLongOrNull()
        if (seconds == null || seconds < 0L) {
            player.sendMessage(plugin.messages.text("command.portal.invalid-cooldown"))
            return true
        }

        return handleUpdate(player, plugin.portals.setCooldown(args[1], seconds)) {
            player.sendMessage(
                plugin.messages.text(
                    "command.portal.cooldown-set",
                    "portal" to it.id,
                    "seconds" to it.cooldownSeconds.toString(),
                ),
            )
        }
    }

    private fun setParticles(player: Player, args: Array<out String>): Boolean {
        if (args.size != 3) {
            player.sendMessage(plugin.messages.text("command.portal.setparticles-usage"))
            return true
        }

        val preset = PortalParticlePreset.fromKey(args[2])
        if (preset == null) {
            player.sendMessage(plugin.messages.text("command.portal.invalid-particles"))
            return true
        }

        return handleUpdate(player, plugin.portals.setParticles(args[1], preset)) {
            player.sendMessage(
                plugin.messages.text(
                    "command.portal.particles-set",
                    "portal" to it.id,
                    "preset" to it.particlePreset.key,
                ),
            )
        }
    }

    private fun setEnabled(player: Player, args: Array<out String>, enabled: Boolean): Boolean {
        if (args.size != 2) {
            player.sendMessage(plugin.messages.text(if (enabled) "command.portal.enable-usage" else "command.portal.disable-usage"))
            return true
        }

        return handleUpdate(player, plugin.portals.setEnabled(args[1], enabled)) {
            player.sendMessage(plugin.messages.text(if (enabled) "command.portal.enabled" else "command.portal.disabled", "portal" to it.id))
        }
    }

    private fun delete(player: Player, args: Array<out String>): Boolean {
        if (args.size != 2) {
            player.sendMessage(plugin.messages.text("command.portal.delete-usage"))
            return true
        }

        return when (val result = plugin.portals.deletePortal(args[1])) {
            is PortalDeleteResult.NotFound -> {
                player.sendMessage(plugin.messages.text("command.portal.not-found", "portal" to result.id))
                true
            }

            is PortalDeleteResult.Success -> {
                player.sendMessage(plugin.messages.text("command.portal.deleted", "portal" to result.portal.id))
                true
            }
        }
    }

    private fun info(player: Player, args: Array<out String>): Boolean {
        if (args.size != 2) {
            player.sendMessage(plugin.messages.text("command.portal.info-usage"))
            return true
        }

        val portal = plugin.portals.portal(args[1]) ?: run {
            player.sendMessage(plugin.messages.text("command.portal.not-found", "portal" to args[1]))
            return true
        }

        player.sendMessage(plugin.messages.text("command.portal.info-header", "portal" to portal.id))
        player.sendMessage(plugin.messages.text("command.portal.info-world", "world" to portal.bounds.worldName))
        player.sendMessage(
            plugin.messages.text(
                "command.portal.info-bounds",
                "x1" to portal.bounds.minX.toString(),
                "y1" to portal.bounds.minY.toString(),
                "z1" to portal.bounds.minZ.toString(),
                "x2" to portal.bounds.maxX.toString(),
                "y2" to portal.bounds.maxY.toString(),
                "z2" to portal.bounds.maxZ.toString(),
            ),
        )
        player.sendMessage(plugin.messages.text("command.portal.info-action", "action" to describeAction(portal.action)))
        player.sendMessage(plugin.messages.text("command.portal.info-cooldown", "seconds" to portal.cooldownSeconds.toString()))
        player.sendMessage(plugin.messages.text("command.portal.info-particles", "preset" to portal.particlePreset.key))
        player.sendMessage(plugin.messages.text("command.portal.info-enabled", "enabled" to portal.enabled.toString()))
        return true
    }

    private fun list(player: Player): Boolean {
        val portals = plugin.portals.portals()
        if (portals.isEmpty()) {
            player.sendMessage(plugin.messages.text("command.portal.none"))
            return true
        }

        player.sendMessage(plugin.messages.text("command.portal.list-header", "count" to portals.size.toString()))
        portals.forEach { portal ->
            player.sendMessage(
                plugin.messages.text(
                    "command.portal.list-entry",
                    "portal" to portal.id,
                    "world" to portal.bounds.worldName,
                    "action" to describeAction(portal.action),
                    "enabled" to if (portal.enabled) "enabled" else "disabled",
                ),
            )
        }
        return true
    }

    private fun handleUpdate(
        player: Player,
        result: PortalUpdateResult,
        onSuccess: (me.dragan.foxcore.portal.PortalDefinition) -> Unit,
    ): Boolean {
        when (result) {
            PortalUpdateResult.MissingSelection -> player.sendMessage(plugin.messages.text("command.portal.selection-missing"))
            PortalUpdateResult.MixedWorldSelection -> player.sendMessage(plugin.messages.text("command.portal.selection-world-mismatch"))
            is PortalUpdateResult.NotFound -> player.sendMessage(plugin.messages.text("command.portal.not-found", "portal" to result.id))
            is PortalUpdateResult.Success -> onSuccess(result.portal)
        }
        return true
    }

    private fun describeAction(action: PortalAction): String =
        when (action) {
            PortalAction.Spawn -> "spawn"
            is PortalAction.Warp -> "warp:${action.warpName}"
            is PortalAction.Rtp -> "rtp:${action.worldName}"
            is PortalAction.Command -> "command:${action.command}"
        }

    companion object {
        private const val PERMISSION = "foxcore.portal.admin"
    }
}
