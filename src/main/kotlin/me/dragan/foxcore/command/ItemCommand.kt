package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class ItemCommand(
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

        if (!player.hasPermission("foxcore.item")) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        val request = parseRequest(args) ?: run {
            player.sendMessage(plugin.messages.text("command.item.usage"))
            return true
        }

        val material = resolveMaterial(request.materialInput) ?: run {
            player.sendMessage(plugin.messages.text("command.item.invalid-material", "material" to request.materialInput))
            return true
        }

        if (!material.isItem || material == Material.AIR) {
            player.sendMessage(plugin.messages.text("command.item.invalid-material", "material" to request.materialInput))
            return true
        }

        val item = ItemStack(material, request.amount)
        val overflow = player.inventory.addItem(item).values
        overflow.forEach { leftover -> player.world.dropItemNaturally(player.location, leftover) }

        player.sendMessage(
            plugin.messages.text(
                if (overflow.isEmpty()) "command.item.success" else "command.item.success-overflow",
                "material" to material.key.key,
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
        if (!sender.hasPermission("foxcore.item")) {
            return emptyList()
        }

        return when (args.size) {
            1 -> Material.entries
                .asSequence()
                .filter { it.isItem && it != Material.AIR }
                .map { it.key.key }
                .filter { it.startsWith(args[0], ignoreCase = true) }
                .sorted()
                .toList()

            2 -> listOf("1", "8", "16", "32", "64")
                .filter { it.startsWith(args[1]) }

            else -> emptyList()
        }
    }

    private fun parseRequest(args: Array<out String>): ItemRequest? {
        if (args.isEmpty() || args.size > 2) {
            return null
        }

        val amount = if (args.size == 2) {
            args[1].toIntOrNull()?.takeIf { it > 0 }?.coerceAtMost(64) ?: return null
        } else {
            1
        }

        return ItemRequest(args[0], amount)
    }

    private fun resolveMaterial(input: String): Material? =
        Material.matchMaterial(input.uppercase()) ?: Material.matchMaterial(input, true)

    private data class ItemRequest(
        val materialInput: String,
        val amount: Int,
    )
}
