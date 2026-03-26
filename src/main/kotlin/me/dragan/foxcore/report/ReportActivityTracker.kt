package me.dragan.foxcore.report

import me.dragan.foxcore.FoxCorePlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

class ReportActivityTracker(
    private val plugin: FoxCorePlugin,
) {
    private val buffers = ConcurrentHashMap<UUID, PlayerActivityBuffer>()

    @Volatile
    private var settings = ReportActivitySettings()

    fun reload() {
        settings = ReportActivitySettings(
            chatHistorySize = plugin.config.getInt("reports.activity.chat-history-size", 10).coerceAtLeast(0),
            commandHistorySize = plugin.config.getInt("reports.activity.command-history-size", 10).coerceAtLeast(0),
            ignoredCommandNames = plugin.config.getStringList("reports.activity.ignore-commands")
                .asSequence()
                .map(String::trim)
                .filter(String::isNotEmpty)
                .map(String::lowercase)
                .toSet(),
            ignoredCommandPatterns = compilePatterns("reports.activity.ignore-command-regex"),
            ignoredChatPatterns = compilePatterns("reports.activity.ignore-chat-regex"),
        )
    }

    fun clear(playerId: UUID) {
        buffers.remove(playerId)
    }

    fun recordChat(playerId: UUID, message: String) {
        val trimmed = sanitize(message)
        val currentSettings = settings
        if (currentSettings.chatHistorySize <= 0 || trimmed.isEmpty()) {
            return
        }
        if (currentSettings.ignoredChatPatterns.any { it.matcher(trimmed).find() }) {
            return
        }

        buffer(playerId).addChat(trimmed, currentSettings.chatHistorySize)
    }

    fun recordCommand(playerId: UUID, commandLine: String) {
        val trimmed = sanitize(commandLine)
        val currentSettings = settings
        if (currentSettings.commandHistorySize <= 0 || trimmed.isEmpty()) {
            return
        }

        val commandName = trimmed.removePrefix("/")
            .substringBefore(' ')
            .substringAfterLast(':')
            .lowercase()
        if (commandName in currentSettings.ignoredCommandNames) {
            return
        }
        if (currentSettings.ignoredCommandPatterns.any { it.matcher(trimmed).find() }) {
            return
        }

        buffer(playerId).addCommand(trimmed, currentSettings.commandHistorySize)
    }

    fun snapshot(playerId: UUID): List<ReportActivitySnapshot> = buffer(playerId).snapshot()

    private fun buffer(playerId: UUID): PlayerActivityBuffer =
        buffers.computeIfAbsent(playerId) { PlayerActivityBuffer() }

    private fun compilePatterns(path: String): List<Pattern> =
        plugin.config.getStringList(path).mapNotNull { raw ->
            val pattern = raw.trim()
            if (pattern.isEmpty()) {
                return@mapNotNull null
            }

            runCatching { Pattern.compile(pattern) }
                .onFailure { plugin.logger.warning("Skipping invalid reports regex at $path: $pattern") }
                .getOrNull()
        }

    private fun sanitize(input: String): String =
        input.trim()
            .replace(Regex("\\s+"), " ")
            .replace('<', '‹')
            .replace('>', '›')
}

private data class ReportActivitySettings(
    val chatHistorySize: Int = 10,
    val commandHistorySize: Int = 10,
    val ignoredCommandNames: Set<String> = emptySet(),
    val ignoredCommandPatterns: List<Pattern> = emptyList(),
    val ignoredChatPatterns: List<Pattern> = emptyList(),
)

private class PlayerActivityBuffer {
    private val chats = ArrayDeque<ReportActivitySnapshot>()
    private val commands = ArrayDeque<ReportActivitySnapshot>()

    @Synchronized
    fun addChat(message: String, limit: Int) {
        chats.addLast(ReportActivitySnapshot(ReportActivityType.CHAT, message, System.currentTimeMillis()))
        trim(chats, limit)
    }

    @Synchronized
    fun addCommand(commandLine: String, limit: Int) {
        commands.addLast(ReportActivitySnapshot(ReportActivityType.COMMAND, commandLine, System.currentTimeMillis()))
        trim(commands, limit)
    }

    @Synchronized
    fun snapshot(): List<ReportActivitySnapshot> =
        (chats + commands).sortedBy(ReportActivitySnapshot::createdAtMillis)

    private fun trim(entries: ArrayDeque<ReportActivitySnapshot>, limit: Int) {
        while (entries.size > limit) {
            entries.removeFirstOrNull()
        }
    }
}
