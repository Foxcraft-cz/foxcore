package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class ItemNameCommand(
    private val plugin: FoxCorePlugin,
) : TabExecutor {
    private val miniMessage = MiniMessage.miniMessage()

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

        if (!player.hasPermission("foxcore.itemname")) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        if (args.isEmpty()) {
            player.sendMessage(plugin.messages.text("command.itemname.usage"))
            return true
        }

        val item = player.inventory.itemInMainHand
        if (item.type.isAir || !item.type.isItem) {
            player.sendMessage(plugin.messages.text("command.itemname.invalid-item"))
            return true
        }

        val rawName = args.joinToString(" ")
        item.editMeta { meta ->
            meta.displayName(miniMessage.deserialize(rawName))
        }

        player.sendMessage(plugin.messages.text("command.itemname.success"))
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> = emptyList()
}
