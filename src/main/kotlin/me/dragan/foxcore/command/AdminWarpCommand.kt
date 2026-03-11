package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.warp.WarpConstraints
import me.dragan.foxcore.warp.WarpDeleteResult
import me.dragan.foxcore.warp.WarpNames
import me.dragan.foxcore.warp.WarpRenameResult
import me.dragan.foxcore.warp.WarpUpdateResult
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class AdminWarpCommand(
    private val plugin: FoxCorePlugin,
) : TabExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("foxcore.adminwarp")) {
            sender.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage(plugin.messages.text("command.adminwarp.usage"))
            return true
        }

        return when (args[0].lowercase()) {
            "delete" -> deleteWarp(sender, args)
            "rename" -> renameWarp(sender, args)
            "movehere" -> moveWarp(sender, args)
            "icon" -> iconWarp(sender, args)
            "title" -> titleWarp(sender, args)
            "description" -> descriptionWarp(sender, args)
            else -> {
                sender.sendMessage(plugin.messages.text("command.adminwarp.usage"))
                true
            }
        }
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        val warpNames = plugin.warps.listWarps().map { it.name }
        return when (args.size) {
            1 -> listOf("delete", "rename", "movehere", "icon", "title", "description")
                .filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> warpNames.filter { it.startsWith(args[1], ignoreCase = true) }
            3 -> if (args[0].equals("icon", ignoreCase = true)) {
                Material.entries.asSequence()
                    .filter(Material::isItem)
                    .map { it.name.lowercase() }
                    .filter { it.startsWith(args[2], ignoreCase = true) }
                    .sorted()
                    .toList()
            } else {
                emptyList()
            }
            else -> emptyList()
        }
    }

    private fun deleteWarp(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.size != 2) {
            sender.sendMessage(plugin.messages.text("command.adminwarp.delete-usage"))
            return true
        }
        return when (plugin.warps.deleteWarp(args[1])) {
            is WarpDeleteResult.NotFound -> sender.sendMessage(plugin.messages.text("command.warp.not-found", "warp" to args[1]))
            is WarpDeleteResult.Success -> sender.sendMessage(plugin.messages.text("command.adminwarp.deleted", "warp" to WarpNames.normalize(args[1])))
        }.let { true }
    }

    private fun renameWarp(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.size != 3) {
            sender.sendMessage(plugin.messages.text("command.adminwarp.rename-usage"))
            return true
        }
        return when (val result = plugin.warps.renameWarp(args[1], args[2])) {
            WarpRenameResult.InvalidName -> sender.sendMessage(plugin.messages.text("command.warp.invalid-name"))
            is WarpRenameResult.NotFound -> sender.sendMessage(plugin.messages.text("command.warp.not-found", "warp" to result.name))
            is WarpRenameResult.AlreadyExists -> sender.sendMessage(plugin.messages.text("command.warp.already-exists", "warp" to result.name))
            is WarpRenameResult.SameName -> sender.sendMessage(plugin.messages.text("command.warp.same-name", "warp" to result.name))
            is WarpRenameResult.Success -> sender.sendMessage(plugin.messages.text("command.adminwarp.renamed", "old" to result.oldName, "new" to result.newName))
        }.let { true }
    }

    private fun moveWarp(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender as? Player ?: run {
            sender.sendMessage(plugin.messages.text("error.player-only"))
            return true
        }
        if (args.size != 2) {
            player.sendMessage(plugin.messages.text("command.adminwarp.movehere-usage"))
            return true
        }
        return when (plugin.warps.moveWarpHere(player, args[1])) {
            is WarpUpdateResult.NotFound -> player.sendMessage(plugin.messages.text("command.warp.not-found", "warp" to args[1]))
            is WarpUpdateResult.Success -> player.sendMessage(plugin.messages.text("command.adminwarp.moved", "warp" to WarpNames.normalize(args[1])))
        }.let { true }
    }

    private fun iconWarp(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.size !in 2..3) {
            sender.sendMessage(plugin.messages.text("command.adminwarp.icon-usage"))
            return true
        }
        val material = resolveMaterial(sender as? Player, args.getOrNull(2)) ?: run {
            sender.sendMessage(plugin.messages.text("command.warp.invalid-icon"))
            return true
        }
        return when (plugin.warps.setWarpIcon(args[1], material)) {
            is WarpUpdateResult.NotFound -> sender.sendMessage(plugin.messages.text("command.warp.not-found", "warp" to args[1]))
            is WarpUpdateResult.Success -> sender.sendMessage(plugin.messages.text("command.adminwarp.icon-set", "warp" to WarpNames.normalize(args[1]), "icon" to material.name.lowercase()))
        }.let { true }
    }

    private fun titleWarp(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.size < 3) {
            sender.sendMessage(plugin.messages.text("command.adminwarp.title-usage"))
            return true
        }
        val title = args.drop(2).joinToString(" ")
        if (title.length > WarpConstraints.TITLE_MAX_LENGTH) {
            sender.sendMessage(plugin.messages.text("command.warp.title-too-long", "max" to WarpConstraints.TITLE_MAX_LENGTH.toString()))
            return true
        }
        return when (plugin.warps.setWarpTitle(args[1], title)) {
            is WarpUpdateResult.NotFound -> sender.sendMessage(plugin.messages.text("command.warp.not-found", "warp" to args[1]))
            is WarpUpdateResult.Success -> sender.sendMessage(plugin.messages.text("command.adminwarp.title-set", "warp" to WarpNames.normalize(args[1])))
        }.let { true }
    }

    private fun descriptionWarp(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.size < 3) {
            sender.sendMessage(plugin.messages.text("command.adminwarp.description-usage"))
            return true
        }
        val description = args.drop(2).joinToString(" ")
        if (description.length > WarpConstraints.DESCRIPTION_MAX_LENGTH) {
            sender.sendMessage(plugin.messages.text("command.warp.description-too-long", "max" to WarpConstraints.DESCRIPTION_MAX_LENGTH.toString()))
            return true
        }
        return when (plugin.warps.setWarpDescription(args[1], description)) {
            is WarpUpdateResult.NotFound -> sender.sendMessage(plugin.messages.text("command.warp.not-found", "warp" to args[1]))
            is WarpUpdateResult.Success -> sender.sendMessage(plugin.messages.text("command.adminwarp.description-set", "warp" to WarpNames.normalize(args[1])))
        }.let { true }
    }

    private fun resolveMaterial(player: Player?, materialInput: String?): Material? {
        if (materialInput != null) {
            return (Material.matchMaterial(materialInput.uppercase()) ?: Material.matchMaterial(materialInput, true))
                ?.takeIf(Material::isItem)
        }

        val held = player?.inventory?.itemInMainHand?.type ?: return null
        return held.takeIf { it != Material.AIR && it.isItem }
    }
}
