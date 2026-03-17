package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class FixCommand(
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

        if (!player.hasPermission("foxcore.fix")) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        if (args.isNotEmpty()) {
            player.sendMessage(plugin.messages.text("command.fix.usage"))
            return true
        }

        when (RepairSupport.repair(player.inventory.itemInMainHand)) {
            RepairResult.INVALID_ITEM -> player.sendMessage(plugin.messages.text("command.fix.invalid-item"))
            RepairResult.NOT_REPAIRABLE -> player.sendMessage(plugin.messages.text("command.fix.not-repairable"))
            RepairResult.ALREADY_REPAIRED -> player.sendMessage(plugin.messages.text("command.fix.already-repaired"))
            RepairResult.REPAIRED -> player.sendMessage(plugin.messages.text("command.fix.success"))
        }

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> = emptyList()
}
