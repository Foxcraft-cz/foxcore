package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.report.ReportCreateResult
import me.dragan.foxcore.report.ReportText
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class ReportCommand(
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
        if (!player.hasPermission("foxcore.report.create")) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }
        if (args.size < 2) {
            player.sendMessage(plugin.messages.text("command.report.usage"))
            return true
        }

        val target = plugin.privateMessages.findVisibleTarget(player, args[0])
        if (target == null) {
            player.sendMessage(plugin.messages.text("command.report.not-found", "player" to args[0]))
            return true
        }

        plugin.reports.createReport(player, target, args.drop(1).joinToString(" ")) { result ->
            when (result) {
                ReportCreateResult.Disabled -> player.sendMessage(plugin.messages.text("command.report.disabled"))
                ReportCreateResult.SelfReport -> player.sendMessage(plugin.messages.text("command.report.self"))
                is ReportCreateResult.ReasonTooShort -> {
                    player.sendMessage(plugin.messages.text("command.report.reason-too-short", "length" to result.minLength.toString()))
                }

                is ReportCreateResult.ReasonTooLong -> {
                    player.sendMessage(plugin.messages.text("command.report.reason-too-long", "length" to result.maxLength.toString()))
                }

                is ReportCreateResult.Cooldown -> {
                    player.sendMessage(plugin.messages.text("command.report.cooldown", "seconds" to result.secondsRemaining.toString()))
                }

                ReportCreateResult.Duplicate -> player.sendMessage(plugin.messages.text("command.report.duplicate"))
                is ReportCreateResult.Success -> {
                    player.sendMessage(
                        plugin.messages.text(
                            "command.report.success",
                            "id" to result.reportId.toString(),
                            "type" to ReportText.type(plugin, result.type),
                            "player" to target.name,
                        ),
                    )
                }
            }
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> {
        val player = sender as? Player ?: return emptyList()
        if (!player.hasPermission("foxcore.report.create")) {
            return emptyList()
        }
        if (args.size != 1) {
            return emptyList()
        }

        return plugin.privateMessages.visiblePlayerNames(player, args[0])
    }
}
