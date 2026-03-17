package me.dragan.foxcore.shortcut

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.help.HelpCategory
import me.dragan.foxcore.help.HelpEntry
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.command.SimpleCommandMap
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File

class ShortcutService(
    private val plugin: FoxCorePlugin,
) {
    private val miniMessage = MiniMessage.miniMessage()
    private val commandMap by lazy { resolveCommandMap() }
    private val knownCommandsField by lazy {
        SimpleCommandMap::class.java.getDeclaredField("knownCommands").apply { isAccessible = true }
    }

    private var shortcutsByLabel: Map<String, ShortcutDefinition> = emptyMap()
    private var registeredCommands: List<RuntimeShortcutCommand> = emptyList()

    fun reload() {
        unregisterCommands()

        val file = plugin.yamlSynchronizer.sync("shortcuts.yml")
        val shortcuts = loadShortcuts(file)
        shortcutsByLabel = shortcuts.flatMap { shortcut ->
            buildList {
                add(shortcut.name to shortcut)
                shortcut.aliases.forEach { add(it to shortcut) }
            }
        }.toMap()

        registeredCommands = shortcuts.map { shortcut ->
            RuntimeShortcutCommand(plugin, shortcut) { sender, label, args ->
                executeShortcut(shortcut, sender, label, args)
            }.also { command ->
                commandMap.register(plugin.name.lowercase(), command)
            }
        }

        syncCommands()
    }

    fun shutdown() {
        unregisterCommands()
        shortcutsByLabel = emptyMap()
    }

    fun helpEntries(): List<HelpEntry> =
        registeredCommands.mapNotNull { command ->
            command.definition.help?.takeIf { it.enabled }?.let { help ->
                HelpEntry(
                    key = command.definition.name,
                    category = help.category,
                    icon = help.icon,
                    visibleTo = { _, player ->
                        command.definition.permission.isBlank() || player.hasPermission(command.definition.permission)
                    },
                ).apply {
                    customName = { miniMessage.deserialize(help.name) }
                    customDescription = { miniMessage.deserialize(help.description) }
                    customUsage = { help.usage }
                }
            }
        }

    private fun unregisterCommands() {
        if (registeredCommands.isEmpty()) {
            return
        }

        val knownCommands = knownCommands()
        registeredCommands.forEach { command ->
            command.unregister(commandMap)
            knownCommands.remove(command.name.lowercase())
            knownCommands.remove("${plugin.name.lowercase()}:${command.name.lowercase()}")
            command.aliases.forEach { alias ->
                knownCommands.remove(alias.lowercase())
                knownCommands.remove("${plugin.name.lowercase()}:${alias.lowercase()}")
            }
        }
        registeredCommands = emptyList()
        syncCommands()
    }

    private fun executeShortcut(
        shortcut: ShortcutDefinition,
        sender: CommandSender,
        label: String,
        args: Array<out String>,
    ): Boolean {
        if (shortcut.playerOnly && sender !is Player) {
            sender.sendMessage(plugin.messages.text("error.player-only"))
            return true
        }

        if (shortcut.permission.isNotBlank() && !sender.hasPermission(shortcut.permission)) {
            sender.sendMessage(plugin.messages.text("error.no-permission"))
            return true
        }

        if (!shortcut.allowArguments && args.isNotEmpty()) {
            shortcut.usage
                ?.takeIf(String::isNotBlank)
                ?.let { sender.sendMessage(renderLine(it, sender, label, args)) }
            return true
        }

        when (shortcut.type) {
            ShortcutType.MESSAGE -> shortcut.lines.forEach { line ->
                sender.sendMessage(renderLine(line, sender, label, args))
            }

            ShortcutType.COMMAND -> {
                val commandLine = buildCommandLine(shortcut, sender, label, args)
                if (commandLine.isBlank()) {
                    shortcut.usage
                        ?.takeIf(String::isNotBlank)
                        ?.let { sender.sendMessage(renderLine(it, sender, label, args)) }
                    return true
                }

                val success = if (shortcut.runAsConsole) {
                    plugin.server.dispatchCommand(plugin.server.consoleSender, commandLine)
                } else {
                    plugin.server.dispatchCommand(sender, commandLine)
                }

                if (!success) {
                    shortcut.usage
                        ?.takeIf(String::isNotBlank)
                        ?.let { sender.sendMessage(renderLine(it, sender, label, args)) }
                }
            }
        }

        return true
    }

    private fun buildCommandLine(
        shortcut: ShortcutDefinition,
        sender: CommandSender,
        label: String,
        args: Array<out String>,
    ): String {
        val base = shortcut.command.orEmpty().trim().removePrefix("/")
        if (base.isBlank()) {
            return ""
        }

        val resolvedBase = renderRaw(base, sender, label, args).trim()
        if (!shortcut.forwardArguments || args.isEmpty()) {
            return resolvedBase
        }

        return "$resolvedBase ${args.joinToString(" ")}".trim()
    }

    private fun renderLine(input: String, sender: CommandSender, label: String, args: Array<out String>): Component =
        miniMessage.deserialize(renderRaw(input, sender, label, args))

    private fun renderRaw(input: String, sender: CommandSender, label: String, args: Array<out String>): String {
        val player = sender as? Player
        return input
            .replace("%player%", player?.name ?: "CONSOLE")
            .replace("%display_name%", player?.name ?: "CONSOLE")
            .replace("%label%", label.lowercase())
            .replace("%command%", label.lowercase())
            .replace("%args%", args.joinToString(" "))
            .replace("%server%", plugin.server.name)
            .replace("%online%", plugin.server.onlinePlayers.size.toString())
            .replace("%max%", plugin.server.maxPlayers.toString())
    }

    private fun loadShortcuts(file: File): List<ShortcutDefinition> {
        val config = YamlConfiguration.loadConfiguration(file)
        val section = config.getConfigurationSection("shortcuts") ?: return emptyList()
        val shortcuts = mutableListOf<ShortcutDefinition>()

        for (key in section.getKeys(false)) {
            val shortcutSection = section.getConfigurationSection(key) ?: continue
            val definition = parseDefinition(key, shortcutSection) ?: continue
            val labels = buildList {
                add(definition.name)
                addAll(definition.aliases)
            }

            var blocked = false
            for (label in labels) {
                if (!isValidLabel(label)) {
                    plugin.logger.warning("Skipping shortcut '$key': '$label' is not a valid command label.")
                    blocked = true
                    break
                }

                if (isRegisteredElsewhere(label)) {
                    plugin.logger.warning("Skipping shortcut '$key': '$label' conflicts with an existing registered command.")
                    blocked = true
                    break
                }

                if (shortcuts.any { it.name == label || label in it.aliases }) {
                    plugin.logger.warning("Skipping shortcut '$key': '$label' is already used by another FoxCore shortcut.")
                    blocked = true
                    break
                }
            }

            if (!blocked) {
                shortcuts += definition
            }
        }

        return shortcuts
    }

    private fun parseDefinition(key: String, section: ConfigurationSection): ShortcutDefinition? {
        val typeName = section.getString("type", "message").orEmpty().lowercase()
        val type = when (typeName) {
            "message" -> ShortcutType.MESSAGE
            "command" -> ShortcutType.COMMAND
            else -> {
                plugin.logger.warning("Skipping shortcut '$key': unsupported type '$typeName'.")
                return null
            }
        }

        val lines = when {
            section.isList("lines") -> section.getStringList("lines").filter(String::isNotBlank)
            section.isString("lines") -> listOfNotNull(section.getString("lines")?.takeIf(String::isNotBlank))
            else -> emptyList()
        }

        val command = section.getString("command")?.trim()
        if (type == ShortcutType.MESSAGE && lines.isEmpty()) {
            plugin.logger.warning("Skipping shortcut '$key': message shortcuts need at least one line.")
            return null
        }
        if (type == ShortcutType.COMMAND && command.isNullOrBlank()) {
            plugin.logger.warning("Skipping shortcut '$key': command shortcuts need a target command.")
            return null
        }

        return ShortcutDefinition(
            name = key.lowercase(),
            aliases = section.getStringList("aliases")
                .map(String::trim)
                .filter(String::isNotBlank)
                .map(String::lowercase)
                .distinct(),
            type = type,
            permission = section.getString("permission").orEmpty(),
            playerOnly = section.getBoolean("player-only", true),
            allowArguments = section.getBoolean("allow-arguments", false),
            forwardArguments = section.getBoolean("forward-arguments", false),
            runAsConsole = section.getString("run-as", "player").equals("console", ignoreCase = true),
            description = section.getString("description").orEmpty(),
            usage = section.getString("usage")?.trim(),
            lines = lines,
            command = command,
            help = parseHelp(section, key),
        )
    }

    private fun parseHelp(section: ConfigurationSection, key: String): ShortcutHelpDefinition? {
        val helpSection = section.getConfigurationSection("help") ?: return null
        val icon = Material.matchMaterial(helpSection.getString("icon").orEmpty())
        if (icon == null) {
            plugin.logger.warning("Shortcut '$key' has invalid help icon '${helpSection.getString("icon")}'.")
            return null
        }

        val category = helpSection.getString("category")
            ?.trim()
            ?.uppercase()
            ?.let { value -> HelpCategory.entries.firstOrNull { it.name == value } }
        if (category == null) {
            plugin.logger.warning("Shortcut '$key' has invalid help category '${helpSection.getString("category")}'.")
            return null
        }

        val name = helpSection.getString("name")?.trim().orEmpty()
        val description = helpSection.getString("description")?.trim().orEmpty()
        if (name.isBlank() || description.isBlank()) {
            plugin.logger.warning("Shortcut '$key' needs help.name and help.description to appear in /foxhelp.")
            return null
        }

        return ShortcutHelpDefinition(
            enabled = helpSection.getBoolean("enabled", true),
            category = category,
            icon = icon,
            name = name,
            description = description,
            usage = helpSection.getString("usage")
                ?.trim()
                .orEmpty()
                .ifBlank { "/$key" },
        )
    }

    private fun isRegisteredElsewhere(label: String): Boolean {
        val existing = knownCommands()[label] ?: return false
        return existing !in registeredCommands
    }

    @Suppress("UNCHECKED_CAST")
    private fun knownCommands(): MutableMap<String, Any> =
        knownCommandsField.get(commandMap) as MutableMap<String, Any>

    private fun resolveCommandMap(): SimpleCommandMap {
        val field = plugin.server.javaClass.getDeclaredField("commandMap")
        field.isAccessible = true
        return field.get(plugin.server) as SimpleCommandMap
    }

    private fun syncCommands() {
        runCatching {
            val method = plugin.server.javaClass.getMethod("syncCommands")
            method.invoke(plugin.server)
        }.onFailure {
            plugin.server.onlinePlayers.forEach { player ->
                runCatching {
                    player.javaClass.getMethod("updateCommands").invoke(player)
                }
            }
        }
    }

    private fun isValidLabel(label: String): Boolean =
        label.matches(Regex("[a-z0-9_\\-]+"))
}

data class ShortcutDefinition(
    val name: String,
    val aliases: List<String>,
    val type: ShortcutType,
    val permission: String,
    val playerOnly: Boolean,
    val allowArguments: Boolean,
    val forwardArguments: Boolean,
    val runAsConsole: Boolean,
    val description: String,
    val usage: String?,
    val lines: List<String>,
    val command: String?,
    val help: ShortcutHelpDefinition?,
)

data class ShortcutHelpDefinition(
    val enabled: Boolean,
    val category: HelpCategory,
    val icon: Material,
    val name: String,
    val description: String,
    val usage: String,
)

enum class ShortcutType {
    MESSAGE,
    COMMAND,
}
