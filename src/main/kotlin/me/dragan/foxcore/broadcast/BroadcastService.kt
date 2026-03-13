package me.dragan.foxcore.broadcast

import me.dragan.foxcore.FoxCorePlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.scheduler.BukkitTask
import java.util.concurrent.ThreadLocalRandom

class BroadcastService(
    private val plugin: FoxCorePlugin,
) {
    private val miniMessage = MiniMessage.miniMessage()
    private var task: BukkitTask? = null
    private var messages: List<List<String>> = emptyList()
    private var lastBroadcastIndex: Int? = null

    fun reload() {
        stop()
        load()
        start()
    }

    fun stop() {
        task?.cancel()
        task = null
    }

    fun broadcastRaw(lines: List<String>) {
        if (lines.isEmpty()) {
            return
        }

        val placeholders = commonPlaceholders()
        val rendered = lines.map { line ->
            var text = line
            for ((key, value) in placeholders) {
                text = text.replace("%$key%", value)
            }
            miniMessage.deserialize(text)
        }
        broadcast(rendered)
    }

    private fun load() {
        messages = loadMessages(plugin.config.getConfigurationSection("broadcasts"))
        lastBroadcastIndex = null
    }

    private fun start() {
        if (!plugin.config.getBoolean("broadcasts.enabled", false) || messages.isEmpty()) {
            return
        }

        val intervalTicks = plugin.config.getLong("broadcasts.interval-seconds", 600L).coerceAtLeast(1L) * 20L
        task = plugin.server.scheduler.runTaskTimer(
            plugin,
            Runnable { broadcastNext() },
            intervalTicks,
            intervalTicks,
        )
    }

    private fun broadcastNext() {
        if (messages.isEmpty()) {
            return
        }

        val nextIndex = when (plugin.config.getString("broadcasts.mode", "random")?.lowercase()) {
            "sequential" -> ((lastBroadcastIndex ?: -1) + 1) % messages.size
            else -> randomIndex()
        }

        lastBroadcastIndex = nextIndex
        broadcastRaw(messages[nextIndex])
    }

    private fun randomIndex(): Int {
        if (messages.size <= 1) {
            return 0
        }

        val previous = lastBroadcastIndex
        var next = ThreadLocalRandom.current().nextInt(messages.size)
        while (next == previous) {
            next = ThreadLocalRandom.current().nextInt(messages.size)
        }
        return next
    }

    private fun broadcast(lines: List<Component>) {
        lines.forEach { line ->
            plugin.server.onlinePlayers.forEach { player -> player.sendMessage(line) }
            plugin.server.consoleSender.sendMessage(line)
        }
    }

    private fun commonPlaceholders(): Array<Pair<String, String>> =
        arrayOf(
            "online" to plugin.server.onlinePlayers.size.toString(),
            "max" to plugin.server.maxPlayers.toString(),
            "server" to plugin.server.name,
        )

    private fun loadMessages(section: ConfigurationSection?): List<List<String>> {
        if (section == null) {
            return emptyList()
        }

        val messagesSection = section.getList("messages") ?: return emptyList()
        return messagesSection.mapNotNull { entry ->
            when (entry) {
                is String -> listOf(entry)
                is List<*> -> entry.mapNotNull { it?.toString() }.takeIf(List<String>::isNotEmpty)
                else -> null
            }
        }
    }
}
