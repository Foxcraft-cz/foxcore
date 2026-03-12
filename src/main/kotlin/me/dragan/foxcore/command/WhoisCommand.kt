package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.back.OfflineLocationLookup
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
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
import java.time.temporal.ChronoUnit
import java.util.Locale

class WhoisCommand(
    private val plugin: FoxCorePlugin,
) : TabExecutor {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        if (args.size != 1) {
            sender.sendMessage(plugin.messages.text("command.whois.usage"))
            return true
        }

        val target = findTarget(args[0])
        if (target == null) {
            sender.sendMessage(plugin.messages.text("command.whois.not-found", "player" to args[0]))
            return true
        }

        if (target.isOnline) {
            val onlineTarget = target.player ?: return true
            sendWhois(sender, target, onlineTarget.location)
            return true
        }

        val targetName = target.name ?: args[0]
        plugin.backService.findOfflineLastLocationByName(targetName) { result ->
            val location = when (result) {
                is OfflineLocationLookup.Success -> result.location
                else -> null
            }
            sendWhois(sender, target, location)
        }
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

    private fun sendWhois(sender: CommandSender, target: OfflinePlayer, location: Location?) {
        val targetName = target.name ?: target.uniqueId.toString()

        sendPrefixed(
            sender,
            plugin.messages.text("command.whois.player-label").append(copyableValue(targetName, targetName, "command.whois.player-hover")),
        )
        sendPrefixed(
            sender,
            plugin.messages.text("command.whois.uuid-label").append(copyableValue(target.uniqueId.toString(), target.uniqueId.toString(), "command.whois.uuid-hover")),
        )

        val statusKey = if (target.isOnline) "command.whois.status-online" else "command.whois.status-offline"
        sendPrefixed(sender, plugin.messages.text("command.whois.status", "status" to plainText(statusKey)))

        if (location != null) {
            sendPrefixed(
                sender,
                plugin.messages.text("command.whois.location-label").append(locationValue(targetName, location)),
            )
        } else {
            sendPrefixed(sender, plugin.messages.text("command.whois.location-unavailable"))
        }

        if (target.isOnline) {
            sendPrefixed(sender, plugin.messages.text("command.whois.online-now"))
            sendPrefixed(
                sender,
                plugin.messages.text(
                    "command.whois.last-seen",
                    "date" to formatDate(System.currentTimeMillis()),
                    "days" to "0",
                ),
            )
        } else {
            val lastSeen = target.lastSeen.takeIf { it > 0L } ?: target.lastPlayed.takeIf { it > 0L }
            if (lastSeen != null) {
                val daysAgo = ChronoUnit.DAYS.between(
                    Instant.ofEpochMilli(lastSeen).atZone(ZoneId.systemDefault()).toLocalDate(),
                    Instant.now().atZone(ZoneId.systemDefault()).toLocalDate(),
                ).coerceAtLeast(0)
                sendPrefixed(
                    sender,
                    plugin.messages.text(
                        "command.whois.last-seen",
                        "date" to formatDate(lastSeen),
                        "days" to daysAgo.toString(),
                    ),
                )
            } else {
                sendPrefixed(sender, plugin.messages.text("command.whois.last-seen-unknown"))
            }
        }

        if (target.firstPlayed > 0L) {
            sendPrefixed(
                sender,
                plugin.messages.text(
                    "command.whois.first-joined",
                    "date" to formatDate(target.firstPlayed),
                ),
            )
        } else {
            sendPrefixed(sender, plugin.messages.text("command.whois.first-joined-unknown"))
        }

        val totalTicks = target.getStatistic(Statistic.PLAY_ONE_MINUTE).toLong().coerceAtLeast(0L)
        sendPrefixed(
            sender,
            plugin.messages.text(
                "command.whois.total-playtime",
                "duration" to formatDuration(Duration.ofSeconds(totalTicks / 20L)),
            ),
        )

        if (target.isOnline) {
            val sessionDuration = Duration.ofMillis((System.currentTimeMillis() - target.lastLogin).coerceAtLeast(0L))
            sendPrefixed(
                sender,
                plugin.messages.text(
                    "command.whois.session",
                    "duration" to formatDuration(sessionDuration),
                ),
            )
        } else {
            sendPrefixed(sender, plugin.messages.text("command.whois.session-unavailable"))
        }
    }

    private fun sendPrefixed(sender: CommandSender, body: Component) {
        sender.sendMessage(plugin.messages.text("prefix").append(body))
    }

    private fun copyableValue(value: String, clipboardValue: String, hoverPath: String): Component =
        Component.text(value, NamedTextColor.WHITE)
            .clickEvent(ClickEvent.copyToClipboard(clipboardValue))
            .hoverEvent(HoverEvent.showText(plugin.messages.text(hoverPath)))

    private fun locationValue(targetName: String, location: Location): Component {
        val display = "${requireNotNull(location.world).name} ${location.blockX}, ${location.blockY}, ${location.blockZ}"
        return Component.text(display, NamedTextColor.WHITE)
            .clickEvent(ClickEvent.runCommand("/tp $targetName"))
            .hoverEvent(HoverEvent.showText(plugin.messages.text("command.whois.location-hover")))
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

    private fun plainText(path: String): String =
        when (path) {
            "command.whois.status-online" -> "online"
            "command.whois.status-offline" -> "offline"
            else -> ""
        }

    companion object {
        private const val PERMISSION = "foxcore.whois"
        private val DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z", Locale.ROOT)
    }
}
