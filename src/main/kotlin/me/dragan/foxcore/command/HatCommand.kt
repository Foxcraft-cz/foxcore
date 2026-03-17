package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class HatCommand(
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

        if (!player.hasPermission("foxcore.hat")) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        if (args.isNotEmpty()) {
            player.sendMessage(plugin.messages.text("command.hat.usage"))
            return true
        }

        val handItem = player.inventory.itemInMainHand
        if (!isValidHatItem(handItem)) {
            player.sendMessage(plugin.messages.text("command.hat.invalid-item"))
            return true
        }

        val previousHelmet = player.inventory.helmet?.clone() ?: ItemStack(Material.AIR)
        player.inventory.helmet = handItem.clone()
        player.inventory.setItemInMainHand(previousHelmet)
        player.sendMessage(plugin.messages.text("command.hat.success"))
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> = emptyList()

    private fun isValidHatItem(item: ItemStack): Boolean =
        item.type != Material.AIR && item.amount > 0 && item.type.isItem
}
