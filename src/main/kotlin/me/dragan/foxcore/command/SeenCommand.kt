package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.feedback.PlayerFeedback
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class SeenCommand(
    private val plugin: FoxCorePlugin,
) : TabExecutor {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        if (!sender.hasPermission(PERMISSION)) {
            (sender as? Player)?.let(PlayerFeedback::error)
            sender.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        if (args.size != 1) {
            (sender as? Player)?.let(PlayerFeedback::error)
            sender.sendMessage(plugin.messages.text("command.seen.usage"))
            return true
        }

        val target = findTarget(args[0])
        if (target == null) {
            (sender as? Player)?.let(PlayerFeedback::error)
            sender.sendMessage(plugin.messages.text("command.seen.not-found", "player" to args[0]))
            return true
        }

        (sender as? Player)?.let(PlayerFeedback::softSuccess)
        sendSeenDetails(sender, target)
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> {
        if (args.size != 1 || !sender.hasPermission(PERMISSION)) {
            return emptyList()
        }

        return Bukkit.getOnlinePlayers()
            .asSequence()
            .map(Player::getName)
            .filter { it.startsWith(args[0], ignoreCase = true) }
            .sorted()
            .toList()
    }

    private fun sendSeenDetails(sender: CommandSender, target: OfflinePlayer) {
        val targetName = target.name ?: target.uniqueId.toString()
        sender.sendMessage(plugin.messages.text("command.seen.player", "player" to targetName))

        if (target.isOnline) {
            sender.sendMessage(plugin.messages.text("command.seen.online-now"))
            sender.sendMessage(
                plugin.messages.text(
                    "command.seen.last-seen",
                    "date" to formatDate(System.currentTimeMillis()),
                    "days" to "0",
                ),
            )
            return
        }

        val lastSeen = target.lastSeen.takeIf { it > 0L } ?: target.lastPlayed.takeIf { it > 0L }
        if (lastSeen == null) {
            sender.sendMessage(plugin.messages.text("command.seen.never-seen"))
            return
        }

        val daysAgo = ChronoUnit.DAYS.between(
            Instant.ofEpochMilli(lastSeen).atZone(ZoneId.systemDefault()).toLocalDate(),
            Instant.now().atZone(ZoneId.systemDefault()).toLocalDate(),
        ).coerceAtLeast(0)

        sender.sendMessage(
            plugin.messages.text(
                "command.seen.last-seen",
                "date" to formatDate(lastSeen),
                "days" to daysAgo.toString(),
            ),
        )
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

    companion object {
        private const val PERMISSION = "foxcore.seen"
        private val DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z", Locale.ROOT)
    }
}
