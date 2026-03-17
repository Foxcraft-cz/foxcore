package me.dragan.foxcore.command

import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.GameMode
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class GamemodeShortcutCommand(
    plugin: FoxCorePlugin,
    private val mode: GameMode,
    private val commandKey: String,
    selfPermission: String,
    othersPermission: String,
) : PlayerTargetShortcutCommand(plugin, selfPermission, othersPermission, "command.gamemode.$commandKey.usage") {
    override fun executeSelf(player: Player): Boolean {
        player.gameMode = mode
        player.sendMessage(plugin.messages.text("command.gamemode.$commandKey.success-self"))
        return true
    }

    override fun executeOther(sender: CommandSender, target: Player): Boolean {
        target.gameMode = mode
        sender.sendMessage(plugin.messages.text("command.gamemode.$commandKey.success-other", "player" to target.name))
        target.sendMessage(plugin.messages.text("command.gamemode.$commandKey.success-by-other", "player" to sender.name))
        return true
    }
}
