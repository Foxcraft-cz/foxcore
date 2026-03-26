package me.dragan.foxcore.reward

import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.Locale

class RewardTrackRegistry(
    private val plugin: FoxCorePlugin,
) {
    @Volatile
    private var tracks: List<RewardTrack> = emptyList()

    fun reload() {
        val folder = File(plugin.dataFolder, TRACKS_FOLDER)
        folder.mkdirs()

        tracks = folder.listFiles()
            .orEmpty()
            .asSequence()
            .filter { it.isFile && it.extension.equals("yml", ignoreCase = true) }
            .mapNotNull(::loadTrack)
            .sortedWith(compareBy<RewardTrack>({ it.order }, { it.id }))
            .toList()
    }

    fun allTracks(): List<RewardTrack> = tracks

    fun findTrack(trackId: String): RewardTrack? =
        tracks.firstOrNull { it.id.equals(trackId, ignoreCase = true) }

    private fun loadTrack(file: File): RewardTrack? {
        val yaml = YamlConfiguration.loadConfiguration(file)
        val id = yaml.getString("id")
            ?.trim()
            .orEmpty()
            .ifBlank { file.nameWithoutExtension.lowercase(Locale.ROOT) }
        if (!VALID_ID.matches(id)) {
            plugin.logger.warning("Skipping reward track '${file.name}': invalid id '$id'.")
            return null
        }

        val progress = loadProgress(id, yaml.getConfigurationSection("progress"), file) ?: return null
        val rewards = yaml.getMapList("rewards")
            .mapIndexedNotNull { index, raw -> loadReward(id, index, raw, file) }
            .sortedWith(compareBy<RewardEntry>({ it.requiredProgress }, { it.id }))
        if (rewards.isEmpty()) {
            plugin.logger.warning("Skipping reward track '${file.name}': no valid rewards were defined.")
            return null
        }

        return RewardTrack(
            id = id,
            order = yaml.getInt("order", 0),
            title = yaml.getString("title")?.trim().orEmpty().ifBlank { id },
            iconMaterialKey = yaml.getString("icon")?.trim()?.ifBlank { null },
            oraxenItemId = yaml.getString("oraxen-item")?.trim()?.ifBlank { null },
            summary = yaml.getStringList("summary").map(String::trim).filter(String::isNotEmpty),
            viewPermission = yaml.getString("view-permission")
                ?.trim()
                .orEmpty()
                .ifBlank { "foxcore.rewards.$id.view" },
            claimPermission = yaml.getString("claim-permission")
                ?.trim()
                .orEmpty()
                .ifBlank { "foxcore.rewards.$id.claim" },
            progress = progress,
            rewards = rewards,
        )
    }

    private fun loadProgress(
        trackId: String,
        section: ConfigurationSection?,
        file: File,
    ): RewardProgressDefinition? {
        if (section == null) {
            plugin.logger.warning("Skipping reward track '${file.name}': missing progress section.")
            return null
        }

        return when (section.getString("type")?.trim()?.lowercase(Locale.ROOT)) {
            "placeholder" -> {
                val placeholder = section.getString("value-placeholder")?.trim().orEmpty()
                if (placeholder.isBlank()) {
                    plugin.logger.warning("Skipping reward track '${file.name}': placeholder progress requires value-placeholder.")
                    return null
                }

                PlaceholderRewardProgressDefinition(
                    valuePlaceholder = placeholder,
                    cyclePlaceholder = section.getString("cycle-placeholder")?.trim()?.ifBlank { null },
                    cycleKey = section.getString("cycle-key")?.trim().orEmpty().ifBlank { "global" },
                )
            }

            "daily_streak" -> DailyStreakRewardProgressDefinition(
                timezone = section.getString("timezone")?.trim().orEmpty().ifBlank { DEFAULT_TIMEZONE },
            )

            else -> {
                plugin.logger.warning("Skipping reward track '${file.name}': unsupported progress type for track '$trackId'.")
                null
            }
        }
    }

    private fun loadReward(
        trackId: String,
        index: Int,
        raw: Map<*, *>,
        file: File,
    ): RewardEntry? {
        val id = raw["id"]?.toString()?.trim().orEmpty().ifBlank { "reward_${index + 1}" }
        if (!VALID_ID.matches(id)) {
            plugin.logger.warning("Skipping reward '${file.name}#$id': invalid reward id in track '$trackId'.")
            return null
        }

        val required = raw["required"]?.toString()?.toIntOrNull()?.coerceAtLeast(1)
        if (required == null) {
            plugin.logger.warning("Skipping reward '${file.name}#$id': required must be a positive integer.")
            return null
        }

        val commands = stringList(raw, "commands")
        if (commands.isEmpty()) {
            plugin.logger.warning("Skipping reward '${file.name}#$id': reward must define at least one command.")
            return null
        }

        return RewardEntry(
            id = id,
            requiredProgress = required,
            iconMaterialKey = raw["icon"]?.toString()?.trim()?.ifBlank { null },
            oraxenItemId = raw["oraxen-item"]?.toString()?.trim()?.ifBlank { null },
            title = raw["title"]?.toString()?.trim()?.ifBlank { null },
            description = stringList(raw, "description"),
            commands = commands,
        )
    }

    private fun stringList(raw: Map<*, *>, key: String): List<String> {
        val value = raw[key] ?: return emptyList()
        return when (value) {
            is String -> listOf(value)
            is Iterable<*> -> value.mapNotNull { it?.toString()?.trim() }.filter(String::isNotEmpty)
            else -> emptyList()
        }
    }

    companion object {
        const val TRACKS_FOLDER: String = "rewards/tracks"
        private const val DEFAULT_TIMEZONE: String = "UTC"
        private val VALID_ID = Regex("[a-z0-9_-]{1,64}")
    }
}
