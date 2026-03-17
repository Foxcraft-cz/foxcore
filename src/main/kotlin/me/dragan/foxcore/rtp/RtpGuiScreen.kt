package me.dragan.foxcore.rtp

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.gui.GuiScreen
import me.dragan.foxcore.gui.GuiSession
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack

class RtpGuiScreen(
    private val plugin: FoxCorePlugin,
    private val worlds: List<RtpWorldSettings>,
) : GuiScreen {
    override val size: Int =
        if (worlds.size <= 7) 27 else 54

    private val miniMessage = MiniMessage.miniMessage()
    private val worldSlots = if (size == 27) {
        listOf(10, 11, 12, 13, 14, 15, 16)
    } else {
        listOf(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
        )
    }

    override fun title(): Component =
        plugin.messages.text("command.rtp.menu-title")

    override fun render(session: GuiSession) {
        val inventory = session.inventory
        inventory.clear()

        for (slot in 0 until size) {
            inventory.setItem(slot, fillerItem())
        }

        val slots = centeredSlots(worlds.size.coerceAtMost(worldSlots.size))
        worlds.take(slots.size).forEachIndexed { index, world ->
            inventory.setItem(slots[index], worldItem(world))
        }

        inventory.setItem(
            closeSlot(),
            ItemStack(Material.BARRIER).apply {
                editMeta { meta ->
                    meta.displayName(plugin.messages.text("command.rtp.menu-close"))
                }
            },
        )
    }

    override fun handleClick(session: GuiSession, rawSlot: Int, click: ClickType) {
        val viewer = session.viewer() ?: return
        if (rawSlot == closeSlot()) {
            viewer.closeInventory()
            return
        }

        val worldIndex = centeredSlots(worlds.size.coerceAtMost(worldSlots.size)).indexOf(rawSlot)
        val world = worlds.getOrNull(worldIndex) ?: return

        viewer.closeInventory()
        when (val start = plugin.rtpService.beginTeleport(viewer, world.worldName) { result -> handleCompletion(viewer, result) }) {
            RtpStartResult.Started -> {
                viewer.sendMessage(plugin.messages.text("command.rtp.searching", "world" to world.worldName))
            }

            RtpStartResult.Disabled -> {
                viewer.sendMessage(plugin.messages.text("command.rtp.disabled"))
            }

            RtpStartResult.WorldDisabled -> {
                viewer.sendMessage(plugin.messages.text("command.rtp.world-disabled", "world" to world.worldName))
            }

            RtpStartResult.WorldUnavailable -> {
                viewer.sendMessage(plugin.messages.text("command.rtp.world-unavailable", "world" to world.worldName))
            }

            RtpStartResult.AlreadySearching -> {
                viewer.sendMessage(plugin.messages.text("command.rtp.already-searching"))
            }

            is RtpStartResult.Cooldown -> {
                viewer.sendMessage(plugin.messages.text("command.rtp.cooldown", "seconds" to start.remainingSeconds.toString()))
            }
        }
    }

    private fun handleCompletion(player: org.bukkit.entity.Player, result: RtpResult) {
        if (!player.isOnline) {
            return
        }

        when (result) {
            is RtpResult.Success -> {
                player.sendMessage(
                    plugin.messages.text(
                        "command.rtp.success",
                        "x" to result.location.blockX.toString(),
                        "y" to result.location.blockY.toString(),
                        "z" to result.location.blockZ.toString(),
                        "world" to requireNotNull(result.location.world).name,
                    ),
                )
            }

            RtpResult.NoLocationFound -> {
                player.sendMessage(plugin.messages.text("command.rtp.failed"))
            }

            RtpResult.Failed -> {
                player.sendMessage(plugin.messages.text("error.teleport-failed"))
            }

            RtpResult.Cancelled -> Unit
        }
    }

    private fun fillerItem(): ItemStack =
        ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
            editMeta { meta -> meta.displayName(Component.text(" ")) }
        }

    private fun worldItem(world: RtpWorldSettings): ItemStack =
        ItemStack(world.icon).apply {
            editMeta { meta ->
                meta.displayName(miniMessage.deserialize("<gold>${world.worldName}</gold>"))
                meta.lore(
                    listOf(
                        miniMessage.deserialize("<gray>Radius: <white>${world.minRadius}</white> - <white>${world.maxRadius}</white></gray>"),
                        miniMessage.deserialize("<gray>Center: <white>${world.centerX.toInt()}</white>, <white>${world.centerZ.toInt()}</white></gray>"),
                        miniMessage.deserialize("<yellow>Click to random teleport</yellow>"),
                    ),
                )
            }
        }

    private fun closeSlot(): Int =
        if (size == 27) 22 else 49

    private fun centeredSlots(count: Int): List<Int> {
        if (size == 27) {
            val layouts = mapOf(
                1 to listOf(13),
                2 to listOf(12, 14),
                3 to listOf(11, 13, 15),
                4 to listOf(10, 12, 14, 16),
                5 to listOf(10, 11, 13, 15, 16),
                6 to listOf(10, 11, 12, 14, 15, 16),
                7 to listOf(10, 11, 12, 13, 14, 15, 16),
            )
            return layouts[count] ?: worldSlots.take(count)
        }

        return worldSlots.take(count)
    }
}
