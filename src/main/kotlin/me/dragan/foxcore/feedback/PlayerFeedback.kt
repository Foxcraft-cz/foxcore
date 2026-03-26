package me.dragan.foxcore.feedback

import org.bukkit.Sound
import org.bukkit.entity.Player

object PlayerFeedback {
    fun success(player: Player) {
        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 0.55f, 1.5f)
    }

    fun softSuccess(player: Player) {
        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 1.3f)
    }

    fun error(player: Player) {
        player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 0.55f, 1.0f)
    }

    fun requestSent(player: Player) {
        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 0.6f, 1.25f)
    }

    fun requestReceived(player: Player) {
        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.75f, 1.2f)
    }

    fun guiOpen(player: Player) {
        player.playSound(player.location, Sound.BLOCK_CHEST_OPEN, 0.55f, 1.15f)
    }

    fun guiClose(player: Player) {
        player.playSound(player.location, Sound.BLOCK_CHEST_CLOSE, 0.55f, 1.0f)
    }

    fun navigation(player: Player) {
        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 0.6f, 1.1f)
    }

    fun teleport(player: Player) {
        player.playSound(player.location, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.05f)
    }

    fun voteStarted(player: Player) {
        player.playSound(player.location, Sound.BLOCK_BELL_USE, 0.55f, 1.3f)
    }

    fun voteCast(player: Player) {
        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BIT, 0.6f, 1.3f)
    }

    fun votePassed(player: Player) {
        player.playSound(player.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1.1f)
    }

    fun voteFailed(player: Player) {
        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.9f)
    }
}
