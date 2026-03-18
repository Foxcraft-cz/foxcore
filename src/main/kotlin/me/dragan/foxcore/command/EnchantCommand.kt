package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class EnchantCommand(
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

        if (!player.hasPermission("foxcore.enchant")) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        val request = parseRequest(args) ?: run {
            player.sendMessage(plugin.messages.text("command.enchant.usage"))
            return true
        }

        val item = player.inventory.itemInMainHand
        if (item.type.isAir || !item.type.isItem) {
            player.sendMessage(plugin.messages.text("command.enchant.invalid-item"))
            return true
        }

        val enchantment = resolveEnchantment(request.enchantmentInput) ?: run {
            player.sendMessage(plugin.messages.text("command.enchant.invalid-enchantment", "enchantment" to request.enchantmentInput))
            return true
        }

        item.addUnsafeEnchantment(enchantment, request.level)
        player.sendMessage(
            plugin.messages.text(
                "command.enchant.success",
                "enchantment" to enchantment.key.key,
                "level" to request.level.toString(),
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
        if (!sender.hasPermission("foxcore.enchant")) {
            return emptyList()
        }

        return when (args.size) {
            1 -> Registry.ENCHANTMENT.iterator()
                .asSequence()
                .map { it.key.key }
                .filter { it.startsWith(args[0], ignoreCase = true) }
                .sorted()
                .toList()

            2 -> listOf("1", "5", "10", "50", "100", "254")
                .filter { it.startsWith(args[1]) }

            else -> emptyList()
        }
    }

    private fun parseRequest(args: Array<out String>): EnchantRequest? {
        if (args.isEmpty() || args.size > 2) {
            return null
        }

        val level = if (args.size == 2) {
            args[1].toIntOrNull()?.takeIf { it > 0 }?.coerceAtMost(254) ?: return null
        } else {
            1
        }

        return EnchantRequest(args[0], level)
    }

    private fun resolveEnchantment(input: String) =
        Registry.ENCHANTMENT.get(NamespacedKey.minecraft(input.lowercase()))
            ?: NamespacedKey.fromString(input.lowercase())?.let(Registry.ENCHANTMENT::get)

    private data class EnchantRequest(
        val enchantmentInput: String,
        val level: Int,
    )
}
