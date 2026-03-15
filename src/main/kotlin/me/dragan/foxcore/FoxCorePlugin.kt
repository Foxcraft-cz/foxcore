package me.dragan.foxcore

import me.dragan.foxcore.afk.AfkService
import me.dragan.foxcore.back.BackCommand
import me.dragan.foxcore.back.BackService
import me.dragan.foxcore.broadcast.BroadcastService
import me.dragan.foxcore.back.storage.StorageFactory
import me.dragan.foxcore.command.AfkCommand
import me.dragan.foxcore.command.BroadcastCommand
import me.dragan.foxcore.command.FoxCoreCommand
import me.dragan.foxcore.command.FlyCommand
import me.dragan.foxcore.command.AdminWarpCommand
import me.dragan.foxcore.command.DeleteHomeCommand
import me.dragan.foxcore.command.DeleteWarpCommand
import me.dragan.foxcore.command.DisposeCommand
import me.dragan.foxcore.command.FeedCommand
import me.dragan.foxcore.command.WorldTimeShortcutCommand
import me.dragan.foxcore.command.WorldWeatherShortcutCommand
import me.dragan.foxcore.command.WhoisCommand
import me.dragan.foxcore.command.GamemodeShortcutCommand
import me.dragan.foxcore.command.HealCommand
import me.dragan.foxcore.command.HeadCommand
import me.dragan.foxcore.command.HelpCommand
import me.dragan.foxcore.command.HatCommand
import me.dragan.foxcore.command.HomeCommand
import me.dragan.foxcore.command.HomesCommand
import me.dragan.foxcore.command.InventoryOpenerCommand
import me.dragan.foxcore.command.OnlineTimeCommand
import me.dragan.foxcore.command.PortalCommand
import me.dragan.foxcore.command.RenameHomeCommand
import me.dragan.foxcore.command.RtpCommand
import me.dragan.foxcore.command.SeenCommand
import me.dragan.foxcore.command.SetSpawnCommand
import me.dragan.foxcore.command.SetHomeIconCommand
import me.dragan.foxcore.command.SetHomeCommand
import me.dragan.foxcore.command.SetWarpCommand
import me.dragan.foxcore.command.SpawnCommand
import me.dragan.foxcore.command.SpeedCommand
import me.dragan.foxcore.command.TpAcceptCommand
import me.dragan.foxcore.command.TpaCommand
import me.dragan.foxcore.command.TpaHereCommand
import me.dragan.foxcore.command.TpaDenyCommand
import me.dragan.foxcore.command.TeleportCommand
import me.dragan.foxcore.command.TeleportHereCommand
import me.dragan.foxcore.command.WarpCommand
import me.dragan.foxcore.config.MessageService
import me.dragan.foxcore.config.YamlResourceSynchronizer
import me.dragan.foxcore.gui.GuiManager
import me.dragan.foxcore.help.ResidenceHelpInfoService
import me.dragan.foxcore.help.PluginHelpInfoService
import me.dragan.foxcore.listener.BackTrackingListener
import me.dragan.foxcore.listener.AfkListener
import me.dragan.foxcore.listener.DisposeInventoryListener
import me.dragan.foxcore.listener.FlyPermissionListener
import me.dragan.foxcore.listener.GuiListener
import me.dragan.foxcore.listener.JoinMessageListener
import me.dragan.foxcore.listener.PortalListener
import me.dragan.foxcore.listener.SpawnJoinListener
import me.dragan.foxcore.listener.SpawnRespawnListener
import me.dragan.foxcore.listener.TpaRequestCleanupListener
import me.dragan.foxcore.portal.PortalService
import me.dragan.foxcore.rtp.RtpService
import me.dragan.foxcore.spawn.SpawnService
import me.dragan.foxcore.teleport.SafeTeleportService
import me.dragan.foxcore.teleport.TeleportEffectService
import me.dragan.foxcore.teleport.VanishService
import me.dragan.foxcore.tpa.TpaRequestService
import me.dragan.foxcore.placeholder.FoxCorePlaceholderExpansion
import me.dragan.foxcore.warp.WarpService
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.command.PluginCommand
import org.bukkit.plugin.java.JavaPlugin

class FoxCorePlugin : JavaPlugin() {
    lateinit var afk: AfkService
        private set
    lateinit var backService: BackService
        private set
    lateinit var broadcasts: BroadcastService
        private set
    lateinit var messages: MessageService
        private set
    lateinit var residenceHelpInfo: ResidenceHelpInfoService
        private set
    lateinit var pluginHelpInfo: PluginHelpInfoService
        private set
    lateinit var guiManager: GuiManager
        private set
    lateinit var safeTeleports: SafeTeleportService
        private set
    lateinit var teleportEffects: TeleportEffectService
        private set
    lateinit var vanishService: VanishService
        private set
    lateinit var spawnService: SpawnService
        private set
    lateinit var rtpService: RtpService
        private set
    lateinit var portals: PortalService
        private set
    lateinit var tpaRequests: TpaRequestService
        private set
    lateinit var warps: WarpService
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
        broadcasts = BroadcastService(this)
        residenceHelpInfo = ResidenceHelpInfoService(this)
        pluginHelpInfo = PluginHelpInfoService(this)
        guiManager = GuiManager()
        tpaRequests = TpaRequestService()
        afk = AfkService(this)
        teleportEffects = TeleportEffectService(this)
        vanishService = VanishService(this)
        safeTeleports = SafeTeleportService(this)
        spawnService = SpawnService(this)
        rtpService = RtpService(this)
        portals = PortalService(this)
        warps = WarpService(this, storage)
        messages = MessageService(this).also { it.reload() }

        registerCommand("back", BackCommand(this))
        registerCommand("broadcast", BroadcastCommand(this))
        registerCommand(
            "anvil",
            InventoryOpenerCommand(this, "foxcore.anvil", "command.anvil") { player ->
                player.openAnvil(null, true)
            },
        )
        registerCommand("afk", AfkCommand(this))
        registerCommand(
            "cartographytable",
            InventoryOpenerCommand(this, "foxcore.cartographytable", "command.cartographytable") { player ->
                player.openCartographyTable(null, true)
            },
        )
        registerCommand(
            "craft",
            InventoryOpenerCommand(this, "foxcore.craft", "command.craft") { player ->
                player.openWorkbench(null, true)
            },
        )
        registerCommand("delhome", DeleteHomeCommand(this))
        registerCommand("dispose", DisposeCommand(this))
        registerCommand("day", WorldTimeShortcutCommand(this, "foxcore.day", "day", 1000L))
        registerCommand("night", WorldTimeShortcutCommand(this, "foxcore.night", "night", 13000L))
        registerCommand(
            "enderchest",
            InventoryOpenerCommand(this, "foxcore.enderchest", "command.enderchest") { player ->
                player.openInventory(player.enderChest)
            },
        )
        registerCommand("feed", FeedCommand(this))
        registerCommand("fly", FlyCommand(this))
        registerCommand("gma", GamemodeShortcutCommand(this, GameMode.ADVENTURE, "gma", "foxcore.gma", "foxcore.gma.others"))
        registerCommand("gmc", GamemodeShortcutCommand(this, GameMode.CREATIVE, "gmc", "foxcore.gmc", "foxcore.gmc.others"))
        registerCommand("gms", GamemodeShortcutCommand(this, GameMode.SURVIVAL, "gms", "foxcore.gms", "foxcore.gms.others"))
        registerCommand("gmsp", GamemodeShortcutCommand(this, GameMode.SPECTATOR, "gmsp", "foxcore.gmsp", "foxcore.gmsp.others"))
        registerCommand(
            "grindstone",
            InventoryOpenerCommand(this, "foxcore.grindstone", "command.grindstone") { player ->
                player.openGrindstone(null, true)
            },
        )
        registerCommand("hat", HatCommand(this))
        registerCommand("heal", HealCommand(this))
        registerCommand("head", HeadCommand(this))
        registerCommand("help", HelpCommand(this))
        registerCommand("home", HomeCommand(this))
        registerCommand("homes", HomesCommand(this))
        registerCommand("onlinetime", OnlineTimeCommand(this))
        registerCommand("portal", PortalCommand(this))
        registerCommand(
            "loom",
            InventoryOpenerCommand(this, "foxcore.loom", "command.loom") { player ->
                player.openLoom(null, true)
            },
        )
        registerCommand("renamehome", RenameHomeCommand(this))
        registerCommand("rain", WorldWeatherShortcutCommand(this, "foxcore.rain", "rain", true))
        registerCommand("warp", WarpCommand(this))
        registerCommand("setwarp", SetWarpCommand(this))
        registerCommand("delwarp", DeleteWarpCommand(this))
        registerCommand("adminwarp", AdminWarpCommand(this))
        registerCommand("rtp", RtpCommand(this))
        registerCommand("seen", SeenCommand(this))
        registerCommand("sethome", SetHomeCommand(this))
        registerCommand("sethomeicon", SetHomeIconCommand(this))
        registerCommand("setspawn", SetSpawnCommand(this))
        registerCommand("sun", WorldWeatherShortcutCommand(this, "foxcore.sun", "sun", false))
        registerCommand("speed", SpeedCommand(this))
        registerCommand(
            "smithingtable",
            InventoryOpenerCommand(this, "foxcore.smithingtable", "command.smithingtable") { player ->
                player.openSmithingTable(null, true)
            },
        )
        registerCommand("spawn", SpawnCommand(this))
        registerCommand(
            "stonecutter",
            InventoryOpenerCommand(this, "foxcore.stonecutter", "command.stonecutter") { player ->
                player.openStonecutter(null, true)
            },
        )
        registerCommand("tp", TeleportCommand(this))
        registerCommand("whois", WhoisCommand(this))
        registerCommand("tphere", TeleportHereCommand(this))
        registerCommand("tpa", TpaCommand(this))
        registerCommand("tpahere", TpaHereCommand(this))
        registerCommand("tpaccept", TpAcceptCommand(this))
        registerCommand("tpadeny", TpaDenyCommand(this))
        registerCommand("foxcore", FoxCoreCommand(this))
        server.onlinePlayers.forEach { backService.loadPlayer(it.uniqueId) }
        afk.start()
        broadcasts.reload()
        maybeRegisterPlaceholderExpansion()
        server.pluginManager.registerEvents(AfkListener(this), this)
        server.pluginManager.registerEvents(BackTrackingListener(this), this)
        server.pluginManager.registerEvents(DisposeInventoryListener(), this)
        server.pluginManager.registerEvents(FlyPermissionListener(this), this)
        server.pluginManager.registerEvents(GuiListener(this), this)
        server.pluginManager.registerEvents(JoinMessageListener(this), this)
        server.pluginManager.registerEvents(PortalListener(this), this)
        server.pluginManager.registerEvents(SpawnJoinListener(this), this)
        server.pluginManager.registerEvents(SpawnRespawnListener(this), this)
        server.pluginManager.registerEvents(TpaRequestCleanupListener(this), this)
        logger.info("FoxCore enabled.")
    }

    fun reloadPlugin() {
        syncBundledFiles()
        reloadConfig()
        afk.start()
        broadcasts.reload()
        rtpService.reload()
        portals.reload()
        warps.reload()
        spawnService.reload()
        messages.reload()
    }

    override fun onDisable() {
        if (::afk.isInitialized) {
            afk.stop()
        }
        if (::broadcasts.isInitialized) {
            broadcasts.stop()
        }
        if (::portals.isInitialized) {
            portals.shutdown()
        }
        if (::warps.isInitialized) {
            warps.shutdown()
        }
        if (::backService.isInitialized) {
            backService.shutdownAndFlush(server.onlinePlayers.toList())
        }
    }

    private fun syncBundledFiles() {
        yamlSynchronizer.sync("config.yml")
        yamlSynchronizer.sync("translations/messages_en.yml")
    }

    fun broadcastAfkState(player: Player, becameAfk: Boolean) {
        if (!config.getBoolean("afk.broadcast-state-changes", true)) {
            return
        }

        val messageKey = if (becameAfk) "afk.enter" else "afk.leave"
        val message = messages.text(messageKey, "player" to player.name)
        server.onlinePlayers.forEach { it.sendMessage(message) }
        server.consoleSender.sendMessage(message)
    }

    private fun maybeRegisterPlaceholderExpansion() {
        if (server.pluginManager.getPlugin("PlaceholderAPI") == null) {
            return
        }

        FoxCorePlaceholderExpansion(this).register()
    }

    private fun registerCommand(name: String, executor: org.bukkit.command.TabExecutor) {
        val command = requireCommand(name)
        command.setExecutor(executor)
        command.tabCompleter = executor
    }

    private fun requireCommand(name: String): PluginCommand =
        requireNotNull(getCommand(name)) { "Command '$name' is missing from plugin.yml" }
}
