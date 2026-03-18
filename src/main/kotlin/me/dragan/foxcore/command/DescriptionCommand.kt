package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class DescriptionCommand(private val plugin: FoxCorePlugin) : TabExecutor {
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

        if (!player.hasPermission("foxcore.description")) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        if (args.size < 2) {
            player.sendMessage(plugin.messages.text("command.description.usage"))
            return true
        }

        val lineIndex = args[0].toIntOrNull()?.takeIf { it >= 1 } ?: run {
            player.sendMessage(plugin.messages.text("command.description.invalid-line"))
            return true
        }

        val item = player.inventory.itemInMainHand
        if (item.type.isAir || !item.type.isItem) {
            player.sendMessage(plugin.messages.text("command.description.invalid-item"))
            return true
        }

        val description = args.slice(1 until args.size).joinToString(" ")
        val component = miniMessage.deserialize(description)

        item.editMeta { meta ->
            val lore = meta.lore()?.toMutableList() ?: mutableListOf()
            while (lore.size < lineIndex) {
                lore.add(Component.empty())
            }
            lore[lineIndex - 1] = component
            meta.lore(lore)
        }
        player.sendMessage(plugin.messages.text("command.description.success", "line" to lineIndex.toString()))
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> = emptyList()
}
