package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.Bukkit
import org.bukkit.attribute.Attribute
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class HealCommand(
    private val plugin: FoxCorePlugin,
) : TabExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        return when (args.size) {
            0 -> healSelf(sender)
            1 -> healOther(sender, args[0])
            else -> {
                sender.sendMessage(plugin.messages.text("command.heal.usage"))
                true
            }
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> {
        if (args.size != 1 || !sender.hasPermission("foxcore.heal.others")) {
            return emptyList()
        }

        return Bukkit.getOnlinePlayers()
            .asSequence()
            .map(Player::getName)
            .filter { it.startsWith(args[0], ignoreCase = true) }
            .sorted()
            .toList()
    }

    private fun healSelf(sender: CommandSender): Boolean {
        val player = sender as? Player ?: run {
            sender.sendMessage(plugin.messages.text("error.player-only"))
            return true
        }

        if (!player.hasPermission("foxcore.heal")) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        applyHeal(player)
        player.sendMessage(plugin.messages.text("command.heal.success-self"))
        return true
    }

    private fun healOther(sender: CommandSender, targetName: String): Boolean {
        if (!sender.hasPermission("foxcore.heal.others")) {
            sender.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        val target = Bukkit.getPlayerExact(targetName)
            ?: Bukkit.getOnlinePlayers().firstOrNull { it.name.equals(targetName, ignoreCase = true) }

        if (target == null) {
            sender.sendMessage(plugin.messages.text("command.heal.not-found", "player" to targetName))
            return true
        }

        applyHeal(target)
        sender.sendMessage(plugin.messages.text("command.heal.success-other", "player" to target.name))
        target.sendMessage(plugin.messages.text("command.heal.success-by-other", "player" to sender.name))
        return true
    }

    private fun applyHeal(player: Player) {
        val maxHealth = player.getAttribute(Attribute.MAX_HEALTH)?.value?.coerceAtLeast(1.0) ?: 20.0
        player.health = maxHealth
        player.fireTicks = 0
    }
}
