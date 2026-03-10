package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.tpa.TpaRequestType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class TpaCommand(
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

        if (!player.hasPermission("foxcore.tpa")) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        if (args.size != 1) {
            player.sendMessage(plugin.messages.text("command.tpa.usage"))
            return true
        }

        val target = findOnlinePlayer(args[0])
        if (target == null) {
            player.sendMessage(plugin.messages.text("command.tpa.not-found", "player" to args[0]))
            return true
        }

        if (target.uniqueId == player.uniqueId) {
            player.sendMessage(plugin.messages.text("command.tpa.self"))
            return true
        }

        val expirySeconds = plugin.config.getLong("tpa.request-expiration-seconds", 60L).coerceAtLeast(1L)
        plugin.tpaRequests.createRequest(player, target, TpaRequestType.TELEPORT_TO_TARGET, expirySeconds)

        player.sendMessage(
            plugin.messages.text(
                "command.tpa.sent",
                "player" to target.name,
                "seconds" to expirySeconds.toString(),
            ),
        )
        target.sendMessage(
            plugin.messages.text(
                "command.tpa.received",
                "player" to player.name,
                "seconds" to expirySeconds.toString(),
            ),
        )
        target.sendMessage(buildActionRow(player.name))
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> {
        if (args.size != 1) {
            return emptyList()
        }

        return Bukkit.getOnlinePlayers()
            .asSequence()
            .map(Player::getName)
            .filter { it.startsWith(args[0], ignoreCase = true) }
            .sorted()
            .toList()
    }

    private fun findOnlinePlayer(input: String): Player? =
        Bukkit.getPlayerExact(input)
            ?: Bukkit.getOnlinePlayers().firstOrNull { it.name.equals(input, ignoreCase = true) }

    private fun buildActionRow(requesterName: String): Component {
        val acceptCommand = "/tpaccept $requesterName"
        val denyCommand = "/tpadeny $requesterName"

        val acceptButton = plugin.messages.text("command.tpa.buttons.accept")
            .clickEvent(ClickEvent.runCommand(acceptCommand))
            .hoverEvent(HoverEvent.showText(plugin.messages.text("command.tpa.buttons.accept-hover", "player" to requesterName)))

        val denyButton = plugin.messages.text("command.tpa.buttons.deny")
            .clickEvent(ClickEvent.runCommand(denyCommand))
            .hoverEvent(HoverEvent.showText(plugin.messages.text("command.tpa.buttons.deny-hover", "player" to requesterName)))

        return Component.empty()
            .append(plugin.messages.text("command.tpa.received-hint-prefix"))
            .appendSpace()
            .append(acceptButton)
            .appendSpace()
            .append(denyButton)
    }
}
