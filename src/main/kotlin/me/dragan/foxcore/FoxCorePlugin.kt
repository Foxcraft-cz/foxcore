package me.dragan.foxcore

import me.dragan.foxcore.back.BackCommand
import me.dragan.foxcore.back.BackService
import me.dragan.foxcore.back.storage.StorageFactory
import me.dragan.foxcore.command.FoxCoreCommand
import me.dragan.foxcore.command.FlyCommand
import me.dragan.foxcore.command.HomeCommand
import me.dragan.foxcore.command.HomesCommand
import me.dragan.foxcore.command.RenameHomeCommand
import me.dragan.foxcore.command.SetSpawnCommand
import me.dragan.foxcore.command.SetHomeIconCommand
import me.dragan.foxcore.command.SetHomeCommand
import me.dragan.foxcore.command.SpawnCommand
import me.dragan.foxcore.command.TpAcceptCommand
import me.dragan.foxcore.command.TpaCommand
import me.dragan.foxcore.command.TpaHereCommand
import me.dragan.foxcore.command.TpaDenyCommand
import me.dragan.foxcore.command.TeleportCommand
import me.dragan.foxcore.command.TeleportHereCommand
import me.dragan.foxcore.config.MessageService
import me.dragan.foxcore.config.YamlResourceSynchronizer
import me.dragan.foxcore.gui.GuiManager
import me.dragan.foxcore.listener.BackTrackingListener
import me.dragan.foxcore.listener.FlyPermissionListener
import me.dragan.foxcore.listener.GuiListener
import me.dragan.foxcore.listener.SpawnJoinListener
import me.dragan.foxcore.listener.SpawnRespawnListener
import me.dragan.foxcore.listener.TpaRequestCleanupListener
import me.dragan.foxcore.spawn.SpawnService
import me.dragan.foxcore.teleport.SafeTeleportService
import me.dragan.foxcore.tpa.TpaRequestService
import org.bukkit.command.PluginCommand
import org.bukkit.plugin.java.JavaPlugin

class FoxCorePlugin : JavaPlugin() {
    lateinit var backService: BackService
        private set
    lateinit var messages: MessageService
        private set
    lateinit var guiManager: GuiManager
        private set
    lateinit var safeTeleports: SafeTeleportService
        private set
    lateinit var spawnService: SpawnService
        private set
    lateinit var tpaRequests: TpaRequestService
        private set
    lateinit var yamlSynchronizer: YamlResourceSynchronizer
        private set

    override fun onEnable() {
        yamlSynchronizer = YamlResourceSynchronizer(this)
        syncBundledFiles()
        reloadConfig()

        val storage = StorageFactory.create(this, config)
        storage.initialize()
        backService = BackService(this, storage)
        guiManager = GuiManager()
        tpaRequests = TpaRequestService()
        safeTeleports = SafeTeleportService(this)
        spawnService = SpawnService(this)
        messages = MessageService(this).also { it.reload() }

        registerCommand("back", BackCommand(this))
        registerCommand("fly", FlyCommand(this))
        registerCommand("home", HomeCommand(this))
        registerCommand("homes", HomesCommand(this))
        registerCommand("renamehome", RenameHomeCommand(this))
        registerCommand("sethome", SetHomeCommand(this))
        registerCommand("sethomeicon", SetHomeIconCommand(this))
        registerCommand("setspawn", SetSpawnCommand(this))
        registerCommand("spawn", SpawnCommand(this))
        registerCommand("tp", TeleportCommand(this))
        registerCommand("tphere", TeleportHereCommand(this))
        registerCommand("tpa", TpaCommand(this))
        registerCommand("tpahere", TpaHereCommand(this))
        registerCommand("tpaccept", TpAcceptCommand(this))
        registerCommand("tpadeny", TpaDenyCommand(this))
        registerCommand("foxcore", FoxCoreCommand(this))
        server.onlinePlayers.forEach { backService.loadPlayer(it.uniqueId) }
        server.pluginManager.registerEvents(BackTrackingListener(this), this)
        server.pluginManager.registerEvents(FlyPermissionListener(this), this)
        server.pluginManager.registerEvents(GuiListener(this), this)
        server.pluginManager.registerEvents(SpawnJoinListener(this), this)
        server.pluginManager.registerEvents(SpawnRespawnListener(this), this)
        server.pluginManager.registerEvents(TpaRequestCleanupListener(this), this)
        logger.info("FoxCore enabled.")
    }

    fun reloadPlugin() {
        syncBundledFiles()
        reloadConfig()
        messages.reload()
    }

    override fun onDisable() {
        if (::backService.isInitialized) {
            backService.shutdownAndFlush(server.onlinePlayers.toList())
        }
    }

    private fun syncBundledFiles() {
        yamlSynchronizer.sync("config.yml")
        yamlSynchronizer.sync("translations/messages_en.yml")
    }

    private fun registerCommand(name: String, executor: org.bukkit.command.TabExecutor) {
        val command = requireCommand(name)
        command.setExecutor(executor)
        command.tabCompleter = executor
    }

    private fun requireCommand(name: String): PluginCommand =
        requireNotNull(getCommand(name)) { "Command '$name' is missing from plugin.yml" }
}
