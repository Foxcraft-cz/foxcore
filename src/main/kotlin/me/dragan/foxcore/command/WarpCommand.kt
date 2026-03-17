package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.teleport.SafeTeleportResult
import me.dragan.foxcore.warp.WarpConstraints
import me.dragan.foxcore.warp.WarpCreateResult
import me.dragan.foxcore.warp.WarpData
import me.dragan.foxcore.warp.WarpGuiScreen
import me.dragan.foxcore.warp.WarpNames
import me.dragan.foxcore.warp.WarpRenameResult
import me.dragan.foxcore.warp.WarpScope
import me.dragan.foxcore.warp.WarpUpdateResult
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class WarpCommand(
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

        if (!player.hasPermission("foxcore.warp")) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        if (args.isEmpty()) {
            val warps = plugin.warps.listWarps()
            if (warps.isEmpty()) {
                player.sendMessage(plugin.messages.text("command.warp.none"))
                return true
            }

            plugin.guiManager.open(player, WarpGuiScreen(plugin, warps))
            return true
        }

        return when (args[0].lowercase()) {
            "create" -> createWarp(player, args)
            "delete" -> deleteWarp(player, args)
            "rename" -> renameWarp(player, args)
            "movehere" -> moveWarp(player, args)
            "icon" -> iconWarp(player, args)
            "title" -> titleWarp(player, args)
            "description" -> descriptionWarp(player, args)
            else -> teleportToWarp(player, args)
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> {
        val player = sender as? Player ?: return emptyList()
        val ownWarps = plugin.warps.getOwnedWarpNames(player)
        return when (args.size) {
            1 -> listOf("create", "delete", "rename", "movehere", "icon", "title", "description")
                .plus(plugin.warps.listWarps().map(WarpData::name))
                .filter { it.startsWith(args[0], ignoreCase = true) }
                .sorted()
            2 -> when (args[0].lowercase()) {
                "delete", "movehere", "icon", "title", "description" -> ownWarps.filter { it.startsWith(args[1], ignoreCase = true) }
                "rename" -> ownWarps.filter { it.startsWith(args[1], ignoreCase = true) }
                else -> emptyList()
            }
            3 -> when (args[0].lowercase()) {
                "icon" -> Material.entries.asSequence()
                    .filter(Material::isItem)
                    .map { it.name.lowercase() }
                    .filter { it.startsWith(args[2], ignoreCase = true) }
                    .sorted()
                    .toList()
                else -> emptyList()
            }
            else -> emptyList()
        }
    }

    private fun teleportToWarp(player: Player, args: Array<out String>): Boolean {
        if (args.size != 1) {
            player.sendMessage(plugin.messages.text("command.warp.usage"))
            return true
        }

        val warp = plugin.warps.getWarp(args[0]) ?: run {
            player.sendMessage(plugin.messages.text("command.warp.not-found", "warp" to args[0]))
            return true
        }

        val cooldown = plugin.warps.remainingTeleportCooldownSeconds(player)
        if (cooldown > 0 && !player.hasPermission("foxcore.warp.bypasscooldown")) {
            player.sendMessage(plugin.messages.text("command.warp.cooldown", "seconds" to cooldown.toString()))
            return true
        }

        val world = plugin.server.getWorld(warp.location.worldName)
        if (world == null) {
            player.sendMessage(plugin.messages.text("command.warp.missing-world", "warp" to warp.name, "world" to warp.location.worldName))
            return true
        }

        when (plugin.safeTeleports.teleport(player, warp.location.toBukkitLocation(world))) {
            SafeTeleportResult.SUCCESS -> {
                plugin.warps.markTeleportUsed(player)
                player.sendMessage(plugin.messages.text("command.warp.success", "warp" to warp.name))
            }

            SafeTeleportResult.NO_SAFE_GROUND -> player.sendMessage(plugin.messages.text("error.no-safe-ground"))
            SafeTeleportResult.FAILED -> player.sendMessage(plugin.messages.text("error.teleport-failed"))
        }
        return true
    }

    private fun createWarp(player: Player, args: Array<out String>): Boolean {
        if (!player.hasPermission("foxcore.warp.create")) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }
        if (args.size != 2) {
            player.sendMessage(plugin.messages.text("command.warp.create-usage"))
            return true
        }

        return when (val result = plugin.warps.createPlayerWarp(player, args[1], maxWarps(player))) {
            WarpCreateResult.InvalidName -> {
                player.sendMessage(plugin.messages.text("command.warp.invalid-name"))
                true
            }

            is WarpCreateResult.AlreadyExists -> {
                player.sendMessage(plugin.messages.text("command.warp.already-exists", "warp" to result.name))
                true
            }

            is WarpCreateResult.LimitReached -> {
                player.sendMessage(plugin.messages.text("command.warp.limit-reached", "max" to result.max.toString()))
                true
            }

            is WarpCreateResult.Success -> {
                player.sendMessage(plugin.messages.text("command.warp.created", "warp" to result.warp.name))
                true
            }
        }
    }

    private fun deleteWarp(player: Player, args: Array<out String>): Boolean {
        if (args.size != 2) {
            player.sendMessage(plugin.messages.text("command.warp.delete-usage"))
            return true
        }

        val warp = ownPlayerWarp(player, args[1]) ?: return true
        return when (plugin.warps.deleteWarp(warp.name)) {
            is me.dragan.foxcore.warp.WarpDeleteResult.Success -> {
                player.sendMessage(plugin.messages.text("command.warp.deleted", "warp" to warp.name))
                true
            }

            is me.dragan.foxcore.warp.WarpDeleteResult.NotFound -> {
                player.sendMessage(plugin.messages.text("command.warp.not-found", "warp" to args[1]))
                true
            }
        }
    }

    private fun renameWarp(player: Player, args: Array<out String>): Boolean {
        if (args.size != 3) {
            player.sendMessage(plugin.messages.text("command.warp.rename-usage"))
            return true
        }

        ownPlayerWarp(player, args[1]) ?: return true
        return when (val result = plugin.warps.renameWarp(args[1], args[2])) {
            WarpRenameResult.InvalidName -> {
                player.sendMessage(plugin.messages.text("command.warp.invalid-name"))
                true
            }

            is WarpRenameResult.NotFound -> {
                player.sendMessage(plugin.messages.text("command.warp.not-found", "warp" to result.name))
                true
            }

            is WarpRenameResult.AlreadyExists -> {
                player.sendMessage(plugin.messages.text("command.warp.already-exists", "warp" to result.name))
                true
            }

            is WarpRenameResult.SameName -> {
                player.sendMessage(plugin.messages.text("command.warp.same-name", "warp" to result.name))
                true
            }

            is WarpRenameResult.Success -> {
                player.sendMessage(
                    plugin.messages.text(
                        "command.warp.renamed",
                        "old" to result.oldName,
                        "new" to result.newName,
                    ),
                )
                true
            }
        }
    }

    private fun moveWarp(player: Player, args: Array<out String>): Boolean {
        if (args.size != 2) {
            player.sendMessage(plugin.messages.text("command.warp.movehere-usage"))
            return true
        }

        ownPlayerWarp(player, args[1]) ?: return true
        return when (plugin.warps.moveWarpHere(player, args[1])) {
            is WarpUpdateResult.NotFound -> {
                player.sendMessage(plugin.messages.text("command.warp.not-found", "warp" to args[1]))
                true
            }

            is WarpUpdateResult.Success -> {
                player.sendMessage(plugin.messages.text("command.warp.moved", "warp" to WarpNames.normalize(args[1])))
                true
            }
        }
    }

    private fun iconWarp(player: Player, args: Array<out String>): Boolean {
        if (args.size !in 2..3) {
            player.sendMessage(plugin.messages.text("command.warp.icon-usage"))
            return true
        }

        ownPlayerWarp(player, args[1]) ?: return true
        val material = resolveMaterial(player, args.getOrNull(2)) ?: run {
            player.sendMessage(plugin.messages.text("command.warp.invalid-icon"))
            return true
        }

        return when (plugin.warps.setWarpIcon(args[1], material)) {
            is WarpUpdateResult.NotFound -> {
                player.sendMessage(plugin.messages.text("command.warp.not-found", "warp" to args[1]))
                true
            }

            is WarpUpdateResult.Success -> {
                player.sendMessage(plugin.messages.text("command.warp.icon-set", "warp" to WarpNames.normalize(args[1]), "icon" to material.name.lowercase()))
                true
            }
        }
    }

    private fun titleWarp(player: Player, args: Array<out String>): Boolean {
        if (args.size < 3) {
            player.sendMessage(plugin.messages.text("command.warp.title-usage"))
            return true
        }

        ownPlayerWarp(player, args[1]) ?: return true
        val title = args.drop(2).joinToString(" ")
        if (title.length > WarpConstraints.TITLE_MAX_LENGTH) {
            player.sendMessage(plugin.messages.text("command.warp.title-too-long", "max" to WarpConstraints.TITLE_MAX_LENGTH.toString()))
            return true
        }

        return when (plugin.warps.setWarpTitle(args[1], title)) {
            is WarpUpdateResult.NotFound -> {
                player.sendMessage(plugin.messages.text("command.warp.not-found", "warp" to args[1]))
                true
            }

            is WarpUpdateResult.Success -> {
                player.sendMessage(plugin.messages.text("command.warp.title-set", "warp" to WarpNames.normalize(args[1])))
                true
            }
        }
    }

    private fun descriptionWarp(player: Player, args: Array<out String>): Boolean {
        if (args.size < 3) {
            player.sendMessage(plugin.messages.text("command.warp.description-usage"))
            return true
        }

        ownPlayerWarp(player, args[1]) ?: return true
        val description = args.drop(2).joinToString(" ")
        if (description.length > WarpConstraints.DESCRIPTION_MAX_LENGTH) {
            player.sendMessage(plugin.messages.text("command.warp.description-too-long", "max" to WarpConstraints.DESCRIPTION_MAX_LENGTH.toString()))
            return true
        }

        return when (plugin.warps.setWarpDescription(args[1], description)) {
            is WarpUpdateResult.NotFound -> {
                player.sendMessage(plugin.messages.text("command.warp.not-found", "warp" to args[1]))
                true
            }

            is WarpUpdateResult.Success -> {
                player.sendMessage(plugin.messages.text("command.warp.description-set", "warp" to WarpNames.normalize(args[1])))
                true
            }
        }
    }

    private fun ownPlayerWarp(player: Player, name: String): WarpData? {
        val warp = plugin.warps.getWarp(name) ?: run {
            player.sendMessage(plugin.messages.text("command.warp.not-found", "warp" to name))
            return null
        }
        if (warp.scope != WarpScope.PLAYER) {
            player.sendMessage(plugin.messages.text("command.warp.not-player-owned", "warp" to warp.name))
            return null
        }
        if (!plugin.warps.isOwner(player, warp)) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return null
        }
        if (!player.hasPermission("foxcore.warp.edit")) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return null
        }
        return warp
    }

    private fun maxWarps(player: Player): Int {
        if (player.hasPermission("foxcore.warp.limit.unlimited")) {
            return Int.MAX_VALUE
        }

        val matched = player.effectivePermissions
            .asSequence()
            .filter { it.value }
            .map { it.permission }
            .filter { it.startsWith("foxcore.warp.limit.") }
            .mapNotNull { it.substringAfter("foxcore.warp.limit.").toIntOrNull() }
            .maxOrNull()

        return matched ?: 1
    }

    private fun resolveMaterial(player: Player, materialInput: String?): Material? {
        if (materialInput != null) {
            return (Material.matchMaterial(materialInput.uppercase()) ?: Material.matchMaterial(materialInput, true))
                ?.takeIf(Material::isItem)
        }

        val held = player.inventory.itemInMainHand.type
        return held.takeIf { it != Material.AIR && it.isItem }
    }
}
