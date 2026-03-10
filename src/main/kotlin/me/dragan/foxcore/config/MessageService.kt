package me.dragan.foxcore.config

import me.dragan.foxcore.FoxCorePlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class MessageService(
    private val plugin: FoxCorePlugin,
) {
    private val translationsFolder = "translations"
    private val miniMessage = MiniMessage.miniMessage()
    private val fallbackMessages by lazy {
        plugin.getResource("$translationsFolder/messages_en.yml")
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { YamlConfiguration.loadConfiguration(it) }
            ?: YamlConfiguration()
    }
    private var messages = YamlConfiguration()

    fun reload() {
        val locale = plugin.config.getString("translations.locale", "en").orEmpty().ifBlank { "en" }
        val fileName = ensureTranslationExists(locale)
        messages = YamlConfiguration.loadConfiguration(File(plugin.dataFolder, "$translationsFolder/$fileName"))
    }

    fun text(path: String, vararg placeholders: Pair<String, String>): Component {
        var message = messages.getString(path)
            ?: fallbackMessages.getString(path)
            ?: "<red>Missing translation: $path</red>"

        for ((key, value) in placeholders) {
            message = message.replace("%$key%", value)
        }

        return miniMessage.deserialize(message)
    }

    private fun ensureTranslationExists(locale: String): String {
        val fileName = "messages_${locale.lowercase()}.yml"
        val resourcePath = "$translationsFolder/$fileName"

        if (plugin.getResource(resourcePath) != null) {
            plugin.yamlSynchronizer.sync(resourcePath)
            return fileName
        }

        plugin.logger.warning("Missing bundled translation '$fileName', falling back to messages_en.yml.")
        plugin.yamlSynchronizer.sync("$translationsFolder/messages_en.yml")
        return "messages_en.yml"
    }
}
