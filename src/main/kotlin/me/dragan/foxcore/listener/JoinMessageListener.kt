package me.dragan.foxcore.listener

import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class JoinMessageListener(
    private val plugin: FoxCorePlugin,
) : Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        val placeholders = commonPlaceholders(player.name)
        val isFirstJoin = !player.hasPlayedBefore()

        event.joinMessage(
            when {
                isFirstJoin && plugin.config.getBoolean("join-messages.first-join-broadcast-enabled", true) ->
                    plugin.messages.text("event.join.first-broadcast", *placeholders)

                !isFirstJoin && plugin.config.getBoolean("join-messages.join-broadcast-enabled", true) ->
                    plugin.messages.text("event.join.broadcast", *placeholders)

                else -> null
            },
        )

        if (!plugin.config.getBoolean("join-messages.personal-enabled", true)) {
            return
        }

        plugin.messages.lines("event.join.personal-lines", *placeholders).forEach(player::sendMessage)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        val placeholders = commonPlaceholders(player.name)
        event.quitMessage(
            if (plugin.config.getBoolean("join-messages.quit-broadcast-enabled", true)) {
                plugin.messages.text("event.quit.broadcast", *placeholders)
            } else {
                null
            },
        )
    }

    private fun commonPlaceholders(playerName: String): Array<Pair<String, String>> =
        arrayOf(
            "player" to playerName,
            "online" to plugin.server.onlinePlayers.size.toString(),
            "max" to plugin.server.maxPlayers.toString(),
            "server" to plugin.server.name,
        )
}
