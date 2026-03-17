package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class FixAllCommand(
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

        if (!player.hasPermission("foxcore.fixall")) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        if (args.isNotEmpty()) {
            player.sendMessage(plugin.messages.text("command.fixall.usage"))
            return true
        }

        val repaired = repairInventory(player)
        if (repaired == 0) {
            player.sendMessage(plugin.messages.text("command.fixall.none-damaged"))
            return true
        }

        player.sendMessage(plugin.messages.text("command.fixall.success", "count" to repaired.toString()))
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> = emptyList()

    private fun repairInventory(player: Player): Int {
        var repaired = 0

        player.inventory.storageContents.forEach { item ->
            if (RepairSupport.repair(item) == RepairResult.REPAIRED) {
                repaired++
            }
        }

        player.inventory.armorContents.forEach { item ->
            if (RepairSupport.repair(item) == RepairResult.REPAIRED) {
                repaired++
            }
        }

        if (RepairSupport.repair(player.inventory.itemInOffHand) == RepairResult.REPAIRED) {
            repaired++
        }

        return repaired
    }
}
