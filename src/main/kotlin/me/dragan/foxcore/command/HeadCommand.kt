package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta

class HeadCommand(
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

        if (!player.hasPermission("foxcore.head")) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        val request = resolveRequest(player, args) ?: run {
            player.sendMessage(plugin.messages.text("command.head.usage"))
            return true
        }

        val head = createHead(request.name, request.amount) ?: run {
            player.sendMessage(plugin.messages.text("command.head.invalid-name"))
            return true
        }

        val overflow = player.inventory.addItem(head).values
        overflow.forEach { item -> player.world.dropItemNaturally(player.location, item) }

        player.sendMessage(
            plugin.messages.text(
                "command.head.success",
                "player" to request.name,
                "amount" to request.amount.toString(),
            ),
        )
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> {
        if (!sender.hasPermission("foxcore.head")) {
            return emptyList()
        }

        return when (args.size) {
            1 -> Bukkit.getOnlinePlayers()
                .asSequence()
                .map(Player::getName)
                .filter { it.startsWith(args[0], ignoreCase = true) }
                .sorted()
                .toList()

            2 -> listOf("1", "8", "16", "32", "64")
                .filter { it.startsWith(args[1]) }

            else -> emptyList()
        }
    }

    private fun resolveRequest(player: Player, args: Array<out String>): HeadRequest? {
        if (args.size > 2) {
            return null
        }

        if (args.isEmpty()) {
            return HeadRequest(player.name, 1)
        }

        if (args.size == 1) {
            val amount = args[0].toIntOrNull()
            return if (amount != null) {
                HeadRequest(player.name, amount.coerceIn(1, 64))
            } else {
                HeadRequest(args[0], 1)
            }
        }

        val amount = args[1].toIntOrNull() ?: return null
        return HeadRequest(args[0], amount.coerceIn(1, 64))
    }

    private fun createHead(name: String, amount: Int): ItemStack? {
        val ownerName = name.trim()
        if (ownerName.isEmpty()) {
            return null
        }

        val item = ItemStack(Material.PLAYER_HEAD, amount)
        val meta = item.itemMeta as? SkullMeta ?: return null
        meta.ownerProfile = Bukkit.createPlayerProfile(ownerName)
        meta.displayName(Component.text("$ownerName's Head"))
        item.itemMeta = meta
        return item
    }

    private data class HeadRequest(
        val name: String,
        val amount: Int,
    )
}
