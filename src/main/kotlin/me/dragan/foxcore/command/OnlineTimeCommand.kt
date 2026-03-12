package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.Statistic
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class OnlineTimeCommand(
    private val plugin: FoxCorePlugin,
) : TabExecutor {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        return when (args.size) {
            0 -> showSelf(sender)
            1 -> showOther(sender, args[0])
            else -> {
                sender.sendMessage(plugin.messages.text("command.onlinetime.usage"))
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
        if (args.size != 1 || !sender.hasPermission(PERMISSION_OTHERS)) {
            return emptyList()
        }

        return Bukkit.getOnlinePlayers()
            .asSequence()
            .map(Player::getName)
            .filter { it.startsWith(args[0], ignoreCase = true) }
            .sorted()
            .toList()
    }

    private fun showSelf(sender: CommandSender): Boolean {
        val player = sender as? Player ?: run {
            sender.sendMessage(plugin.messages.text("error.player-only"))
            return true
        }

        if (!player.hasPermission(PERMISSION_SELF)) {
            player.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        sendTimeDetails(sender = player, target = player, showPlayerLine = false)
        return true
    }

    private fun showOther(sender: CommandSender, input: String): Boolean {
        if (!sender.hasPermission(PERMISSION_OTHERS)) {
            sender.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        val target = findTarget(input)
        if (target == null) {
            sender.sendMessage(plugin.messages.text("command.onlinetime.not-found", "player" to input))
            return true
        }

        sendTimeDetails(sender = sender, target = target, showPlayerLine = true)
        return true
    }

    private fun sendTimeDetails(sender: CommandSender, target: OfflinePlayer, showPlayerLine: Boolean) {
        val targetName = target.name ?: target.uniqueId.toString()

        if (showPlayerLine) {
            sender.sendMessage(plugin.messages.text("command.onlinetime.player", "player" to targetName))
        }

        if (target.isOnline) {
            val sessionDuration = Duration.ofMillis((System.currentTimeMillis() - target.lastLogin).coerceAtLeast(0L))
            sender.sendMessage(
                plugin.messages.text(
                    "command.onlinetime.session",
                    "duration" to formatDuration(sessionDuration),
                ),
            )
        } else {
            sender.sendMessage(plugin.messages.text("command.onlinetime.session-unavailable"))
        }

        val totalTicks = target.getStatistic(Statistic.PLAY_ONE_MINUTE).toLong().coerceAtLeast(0L)
        sender.sendMessage(
            plugin.messages.text(
                "command.onlinetime.total",
                "duration" to formatDuration(Duration.ofSeconds(totalTicks / 20L)),
            ),
        )

        if (target.firstPlayed > 0L) {
            sender.sendMessage(
                plugin.messages.text(
                    "command.onlinetime.first-joined",
                    "date" to formatDate(target.firstPlayed),
                ),
            )
        } else {
            sender.sendMessage(plugin.messages.text("command.onlinetime.first-joined-unknown"))
        }
    }

    private fun findTarget(input: String): OfflinePlayer? {
        val online = Bukkit.getPlayerExact(input)
            ?: Bukkit.getOnlinePlayers().firstOrNull { it.name.equals(input, ignoreCase = true) }
        if (online != null) {
            return online
        }

        val cached = plugin.server.getOfflinePlayerIfCached(input) ?: return null
        return cached.takeIf { it.hasPlayedBefore() || it.isOnline }
    }

    private fun formatDate(epochMillis: Long): String =
        DATE_FORMATTER.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

    private fun formatDuration(duration: Duration): String {
        val totalSeconds = duration.seconds.coerceAtLeast(0L)
        val days = totalSeconds / 86_400
        val hours = (totalSeconds % 86_400) / 3_600
        val minutes = (totalSeconds % 3_600) / 60
        val seconds = totalSeconds % 60

        val parts = mutableListOf<String>()
        if (days > 0) {
            parts += "${days}d"
        }
        if (hours > 0) {
            parts += "${hours}h"
        }
        if (minutes > 0) {
            parts += "${minutes}m"
        }
        if (seconds > 0 || parts.isEmpty()) {
            parts += "${seconds}s"
        }

        return parts.joinToString(" ")
    }

    companion object {
        private const val PERMISSION_SELF = "foxcore.onlinetime"
        private const val PERMISSION_OTHERS = "foxcore.onlinetime.others"
        private val DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z", Locale.ROOT)
    }
}
