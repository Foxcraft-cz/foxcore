package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.dispose.DisposeInventoryHolder
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class DisposeCommand(
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

        if (args.isNotEmpty()) {
            player.sendMessage(plugin.messages.text("command.dispose.usage"))
            return true
        }

        val holder = DisposeInventoryHolder(player.uniqueId)
        val inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, plugin.messages.text("command.dispose.title"))
        holder.backingInventory = inventory
        player.openInventory(inventory)
        player.sendMessage(plugin.messages.text("command.dispose.success"))
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> = emptyList()

    companion object {
        private const val PERMISSION = "foxcore.dispose"
        private const val INVENTORY_SIZE = 27
    }
}
