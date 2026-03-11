package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class SpeedCommand(
    private val plugin: FoxCorePlugin,
) : TabExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        return when (args.size) {
            1 -> handleSelf(sender, args[0])
            2 -> handleOther(sender, args[0], args[1])
            else -> {
                sender.sendMessage(plugin.messages.text("command.speed.usage"))
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
        return when (args.size) {
            1 -> (1..10).map(Int::toString).filter { it.startsWith(args[0]) }
            2 -> {
                if (!sender.hasPermission("foxcore.speed.others")) {
                    return emptyList()
                }

                Bukkit.getOnlinePlayers()
                    .asSequence()
                    .map(Player::getName)
                    .filter { it.startsWith(args[1], ignoreCase = true) }
                    .sorted()
                    .toList()
            }

            else -> emptyList()
        }
    }

    private fun handleSelf(sender: CommandSender, rawSpeed: String): Boolean {
        val player = sender as? Player ?: run {
            sender.sendMessage(plugin.messages.text("error.player-only"))
            return true
        }

        if (!player.hasPermission("foxcore.speed")) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        val speed = parseSpeed(rawSpeed) ?: run {
            player.sendMessage(plugin.messages.text("command.speed.invalid-range"))
            return true
        }

        applySpeed(player, speed)
        player.sendMessage(plugin.messages.text("command.speed.success-self", "speed" to speed.toString()))
        return true
    }

    private fun handleOther(sender: CommandSender, rawSpeed: String, targetName: String): Boolean {
        if (!sender.hasPermission("foxcore.speed.others")) {
            sender.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        val speed = parseSpeed(rawSpeed) ?: run {
            sender.sendMessage(plugin.messages.text("command.speed.invalid-range"))
            return true
        }

        val target = Bukkit.getPlayerExact(targetName)
            ?: Bukkit.getOnlinePlayers().firstOrNull { it.name.equals(targetName, ignoreCase = true) }

        if (target == null) {
            sender.sendMessage(plugin.messages.text("command.speed.not-found", "player" to targetName))
            return true
        }

        applySpeed(target, speed)
        sender.sendMessage(
            plugin.messages.text(
                "command.speed.success-other",
                "player" to target.name,
                "speed" to speed.toString(),
            ),
        )
        target.sendMessage(
            plugin.messages.text(
                "command.speed.success-by-other",
                "player" to sender.name,
                "speed" to speed.toString(),
            ),
        )
        return true
    }

    private fun parseSpeed(input: String): Int? =
        input.toIntOrNull()?.takeIf { it in 1..10 }

    private fun applySpeed(player: Player, speed: Int) {
        player.flySpeed = speed / 10.0f
    }
}
