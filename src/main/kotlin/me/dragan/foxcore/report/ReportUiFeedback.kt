package me.dragan.foxcore.report

import org.bukkit.Sound
import org.bukkit.entity.Player

internal object ReportUiFeedback {
    fun navigation(player: Player) {
        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 0.7f, 1.1f)
    }

    fun open(player: Player) {
        player.playSound(player.location, Sound.BLOCK_CHEST_OPEN, 0.6f, 1.15f)
    }

    fun close(player: Player) {
        player.playSound(player.location, Sound.BLOCK_CHEST_CLOSE, 0.6f, 1.0f)
    }

    fun teleport(player: Player) {
        player.playSound(player.location, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.05f)
    }

    fun success(player: Player) {
        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.5f)
    }

    fun error(player: Player) {
        player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 0.6f, 1.0f)
    }
}
