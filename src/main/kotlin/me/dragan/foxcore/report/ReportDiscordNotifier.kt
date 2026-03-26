package me.dragan.foxcore.report

import me.dragan.foxcore.FoxCorePlugin

class ReportDiscordNotifier(
    private val plugin: FoxCorePlugin,
) {
    @Volatile
    private var enabled = false

    @Volatile
    private var channelId = ""

    fun reload() {
        enabled = plugin.config.getBoolean("reports.discord.enabled", false) &&
            plugin.config.getBoolean("reports.discord.use-discordsrv", true)
        channelId = plugin.config.getString("reports.discord.channel-id", "").orEmpty().trim()
    }

    fun sendCreatedNotification(reportId: Long, type: ReportType, reporterName: String, reportedName: String, reason: String) {
        if (!enabled || channelId.isEmpty()) {
            return
        }

        val pluginInstance = plugin.server.pluginManager.getPlugin("DiscordSRV") ?: return
        val message = buildString {
            append("**New ")
            append(type.name.lowercase())
            append(" report** #")
            append(reportId)
            append('\n')
            append("Reported: `")
            append(reportedName)
            append("`\n")
            append("Reporter: `")
            append(reporterName)
            append("`\n")
            append("Reason: ")
            append(reason)
        }

        runCatching {
            val discordsrvClass = Class.forName("github.scarsz.discordsrv.DiscordSRV")
            val getPlugin = discordsrvClass.getMethod("getPlugin")
            val discordsrv = getPlugin.invoke(null) ?: pluginInstance
            val getJda = discordsrv.javaClass.getMethod("getJda")
            val jda = getJda.invoke(discordsrv)
            val getTextChannelById = jda.javaClass.getMethod("getTextChannelById", String::class.java)
            val channel = getTextChannelById.invoke(jda, channelId) ?: return
            val sendMessage = channel.javaClass.getMethod("sendMessage", CharSequence::class.java)
            val action = sendMessage.invoke(channel, message)
            val queue = action.javaClass.getMethod("queue")
            queue.invoke(action)
        }.onFailure { error ->
            plugin.logger.warning("Failed to send report notification to DiscordSRV: ${error.message}")
        }
    }
}
