package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

abstract class PlayerTargetShortcutCommand(
    protected val plugin: FoxCorePlugin,
    private val selfPermission: String,
    private val othersPermission: String,
    private val usageKey: String,
) : TabExecutor {
    final override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        return when (args.size) {
            0 -> executeForSelf(sender)
            1 -> executeForOther(sender, args[0])
            else -> {
                sender.sendMessage(plugin.messages.text(usageKey))
                true
            }
        }
    }

    final override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> {
        if (args.size != 1 || !sender.hasPermission(othersPermission)) {
            return emptyList()
        }

        return Bukkit.getOnlinePlayers()
            .asSequence()
            .map(Player::getName)
            .filter { it.startsWith(args[0], ignoreCase = true) }
            .sorted()
            .toList()
    }

    private fun executeForSelf(sender: CommandSender): Boolean {
        val player = sender as? Player ?: run {
            sender.sendMessage(plugin.messages.text("error.player-only"))
            return true
        }

        if (!player.hasPermission(selfPermission)) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        return executeSelf(player)
    }

    private fun executeForOther(sender: CommandSender, targetName: String): Boolean {
        if (!sender.hasPermission(othersPermission)) {
            sender.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        val target = Bukkit.getPlayerExact(targetName)
            ?: Bukkit.getOnlinePlayers().firstOrNull { it.name.equals(targetName, ignoreCase = true) }

        if (target == null) {
            sender.sendMessage(plugin.messages.text("command.gamemode.not-found", "player" to targetName))
            return true
        }

        return executeOther(sender, target)
    }

    protected abstract fun executeSelf(player: Player): Boolean

    protected abstract fun executeOther(sender: CommandSender, target: Player): Boolean
}
