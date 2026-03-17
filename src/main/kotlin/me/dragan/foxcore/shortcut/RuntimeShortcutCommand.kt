package me.dragan.foxcore.shortcut

import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.PluginIdentifiableCommand

class RuntimeShortcutCommand(
    private val plugin: FoxCorePlugin,
    val definition: ShortcutDefinition,
    private val executor: (CommandSender, String, Array<out String>) -> Boolean,
) : Command(
    definition.name,
    definition.description.ifBlank { "FoxCore shortcut command." },
    definition.usage ?: "/${definition.name}",
    definition.aliases,
), PluginIdentifiableCommand {

    override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean =
        executor(sender, commandLabel, args)

    override fun getPlugin(): FoxCorePlugin = plugin

    override fun tabComplete(sender: CommandSender, alias: String, args: Array<out String>): MutableList<String> =
        mutableListOf()
}
