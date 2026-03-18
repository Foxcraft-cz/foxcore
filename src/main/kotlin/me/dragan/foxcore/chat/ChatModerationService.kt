package me.dragan.foxcore.chat

import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

class ChatModerationService(
    private val plugin: FoxCorePlugin,
) {
    private val publicStates = ConcurrentHashMap<UUID, SpamState>()
    private val privateStates = ConcurrentHashMap<UUID, SpamState>()

    @Volatile
    private var settings = ChatModerationSettings()

    @Volatile
    private var filterRules: List<ChatFilterRule> = emptyList()

    fun reload() {
        settings = ChatModerationSettings(
            enabled = plugin.config.getBoolean("chat.enabled", true) && plugin.config.getBoolean("chat.public.enabled", true),
            minIntervalMillis = plugin.config.getLong("chat.spam.min-interval-millis", 800L).coerceAtLeast(0L),
            maxLength = plugin.config.getInt("chat.spam.max-length", 256).coerceAtLeast(1),
            duplicateWindowMillis = plugin.config.getLong("chat.spam.duplicate-window-seconds", 15L).coerceAtLeast(0L) * 1000L,
            maxDuplicates = plugin.config.getInt("chat.spam.max-duplicates", 2).coerceAtLeast(1),
            maxRepeatRun = plugin.config.getInt("chat.spam.max-repeat-run", 4).coerceAtLeast(1),
            filtersEnabled = plugin.config.getBoolean("chat.filters.enabled", false),
            privateEnabled = plugin.config.getBoolean("chat.enabled", true) && plugin.config.getBoolean("chat.private.enabled", true),
            privateMinIntervalMillis = plugin.config.getLong("chat.private.min-interval-millis", 300L).coerceAtLeast(0L),
            privateMaxLength = plugin.config.getInt("chat.private.max-length", 256).coerceAtLeast(1),
            privateDuplicateWindowMillis = plugin.config.getLong("chat.private.duplicate-window-seconds", 10L).coerceAtLeast(0L) * 1000L,
            privateMaxDuplicates = plugin.config.getInt("chat.private.max-duplicates", 3).coerceAtLeast(1),
        )
        filterRules = plugin.config.getMapList("chat.filters.rules").mapNotNull(::toFilterRule)
    }

    fun isPublicChatEnabled(): Boolean = settings.enabled

    fun isPrivateMessagingEnabled(): Boolean = settings.privateEnabled

    fun moderatePublic(player: Player, rawInput: String): ChatModerationResult {
        return moderate(
            player = player,
            rawInput = rawInput,
            maxLength = settings.maxLength,
            maxRepeatRun = settings.maxRepeatRun,
            states = publicStates,
            minIntervalMillis = settings.minIntervalMillis,
            duplicateWindowMillis = settings.duplicateWindowMillis,
            maxDuplicates = settings.maxDuplicates,
            emptyKey = "chat.public.empty",
            tooLongKey = "chat.public.too-long",
            blockedKey = "chat.public.blocked",
            cooldownKey = "chat.public.spam-cooldown",
            duplicateKey = "chat.public.spam-duplicate",
        )
    }

    fun moderatePrivate(player: Player, rawInput: String): ChatModerationResult {
        if (!settings.privateEnabled) {
            return ChatModerationResult.denied("chat.private.disabled")
        }

        return moderate(
            player = player,
            rawInput = rawInput,
            maxLength = settings.privateMaxLength,
            maxRepeatRun = settings.maxRepeatRun,
            states = privateStates,
            minIntervalMillis = settings.privateMinIntervalMillis,
            duplicateWindowMillis = settings.privateDuplicateWindowMillis,
            maxDuplicates = settings.privateMaxDuplicates,
            emptyKey = "chat.private.empty",
            tooLongKey = "chat.private.too-long",
            blockedKey = "chat.private.blocked",
            cooldownKey = "chat.private.spam-cooldown",
            duplicateKey = "chat.private.spam-duplicate",
        )
    }

    private fun moderate(
        player: Player,
        rawInput: String,
        maxLength: Int,
        maxRepeatRun: Int,
        states: ConcurrentHashMap<UUID, SpamState>,
        minIntervalMillis: Long,
        duplicateWindowMillis: Long,
        maxDuplicates: Int,
        emptyKey: String,
        tooLongKey: String,
        blockedKey: String,
        cooldownKey: String,
        duplicateKey: String,
    ): ChatModerationResult {
        val sanitized = sanitize(rawInput, maxRepeatRun)
        if (sanitized.isBlank()) {
            return ChatModerationResult.denied(emptyKey)
        }
        if (sanitized.length > maxLength) {
            return ChatModerationResult.denied(tooLongKey, "length" to maxLength.toString())
        }

        val filtered = applyFilters(player, sanitized) ?: return ChatModerationResult.denied(blockedKey)
        val normalized = normalize(filtered)
        if (normalized.isBlank()) {
            return ChatModerationResult.denied(emptyKey)
        }

        if (!player.hasPermission("foxcore.chat.spam.bypass")) {
            val violation = checkSpam(
                playerId = player.uniqueId,
                normalized = normalized,
                states = states,
                minIntervalMillis = minIntervalMillis,
                duplicateWindowMillis = duplicateWindowMillis,
                maxDuplicates = maxDuplicates,
                cooldownKey = cooldownKey,
                duplicateKey = duplicateKey,
            )
            if (violation != null) {
                return ChatModerationResult.denied(violation)
            }
        }

        return ChatModerationResult.allowed(filtered)
    }

    private fun sanitize(input: String, maxRepeatRun: Int): String {
        val withoutControls = buildString(input.length) {
            input.forEach { char ->
                if (!char.isISOControl() || char == '\n' || char == '\t') {
                    append(char)
                }
            }
        }

        return limitRepeatedCharacters(withoutControls.trim().replace(Regex("\\s+"), " "), maxRepeatRun)
    }

    private fun applyFilters(player: Player, input: String): String? {
        if (!settings.filtersEnabled || player.hasPermission("foxcore.chat.filter.bypass")) {
            return input
        }

        var current = input
        for (rule in filterRules) {
            when (rule.action) {
                ChatFilterAction.BLOCK -> if (rule.pattern.matcher(current).find()) {
                    return null
                }
                ChatFilterAction.REPLACE -> {
                    current = rule.pattern.matcher(current).replaceAll(rule.replacement)
                }
            }
        }
        return current
    }

    private fun checkSpam(
        playerId: UUID,
        normalized: String,
        states: ConcurrentHashMap<UUID, SpamState>,
        minIntervalMillis: Long,
        duplicateWindowMillis: Long,
        maxDuplicates: Int,
        cooldownKey: String,
        duplicateKey: String,
    ): String? {
        val now = System.currentTimeMillis()
        var violation: String? = null

        states.compute(playerId) { _, existing ->
            val state = existing ?: SpamState()
            if (minIntervalMillis > 0 && now - state.lastMessageAt < minIntervalMillis) {
                violation = cooldownKey
                return@compute state
            }

            if (normalized == state.lastNormalized && now - state.lastDuplicateAt <= duplicateWindowMillis) {
                state.duplicateCount += 1
            } else {
                state.duplicateCount = 1
            }

            if (state.duplicateCount > maxDuplicates) {
                violation = duplicateKey
                return@compute state
            }

            state.lastMessageAt = now
            state.lastDuplicateAt = now
            state.lastNormalized = normalized
            state
        }

        return violation
    }

    private fun normalize(input: String): String = input.lowercase().trim()

    private fun limitRepeatedCharacters(input: String, maxRun: Int): String {
        if (input.isEmpty()) {
            return input
        }

        val builder = StringBuilder(input.length)
        var previous = input.first()
        var run = 1
        builder.append(previous)

        for (index in 1 until input.length) {
            val current = input[index]
            if (current == previous) {
                run += 1
                if (run <= maxRun) {
                    builder.append(current)
                }
            } else {
                previous = current
                run = 1
                builder.append(current)
            }
        }

        return builder.toString()
    }

    private fun toFilterRule(entry: Map<*, *>): ChatFilterRule? {
        val pattern = entry["pattern"]?.toString()?.trim().orEmpty()
        if (pattern.isBlank()) {
            return null
        }

        val action = when (entry["action"]?.toString()?.trim()?.lowercase()) {
            "block" -> ChatFilterAction.BLOCK
            else -> ChatFilterAction.REPLACE
        }
        val replacement = entry["replacement"]?.toString().orEmpty()

        return try {
            ChatFilterRule(Pattern.compile(pattern), action, replacement)
        } catch (_: Exception) {
            plugin.logger.warning("Skipping invalid chat filter regex: $pattern")
            null
        }
    }
}

data class ChatModerationResult(
    val allowed: Boolean,
    val message: String,
    val denialKey: String?,
    val placeholders: Array<out Pair<String, String>>,
) {
    companion object {
        fun allowed(message: String): ChatModerationResult =
            ChatModerationResult(true, message, null, emptyArray())

        fun denied(path: String, vararg placeholders: Pair<String, String>): ChatModerationResult =
            ChatModerationResult(false, "", path, placeholders)
    }
}

private data class ChatModerationSettings(
    val enabled: Boolean = true,
    val minIntervalMillis: Long = 800L,
    val maxLength: Int = 256,
    val duplicateWindowMillis: Long = 15_000L,
    val maxDuplicates: Int = 2,
    val maxRepeatRun: Int = 4,
    val filtersEnabled: Boolean = false,
    val privateEnabled: Boolean = true,
    val privateMinIntervalMillis: Long = 300L,
    val privateMaxLength: Int = 256,
    val privateDuplicateWindowMillis: Long = 10_000L,
    val privateMaxDuplicates: Int = 3,
)

private data class SpamState(
    var lastMessageAt: Long = 0L,
    var lastDuplicateAt: Long = 0L,
    var lastNormalized: String = "",
    var duplicateCount: Int = 0,
)

private data class ChatFilterRule(
    val pattern: Pattern,
    val action: ChatFilterAction,
    val replacement: String,
)

private enum class ChatFilterAction {
    BLOCK,
    REPLACE,
}
