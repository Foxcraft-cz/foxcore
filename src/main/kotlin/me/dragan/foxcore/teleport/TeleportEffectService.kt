package me.dragan.foxcore.teleport

import me.dragan.foxcore.FoxCorePlugin
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player

class TeleportEffectService(
    private val plugin: FoxCorePlugin,
) {
    fun play(player: Player, origin: Location, destination: Location) {
        if (!plugin.config.getBoolean("teleport.effects.enabled", true)) {
            return
        }
        if (plugin.vanishService.isVanished(player)) {
            return
        }

        if (plugin.config.getBoolean("teleport.effects.particles.enabled", true)) {
            spawnOriginParticles(origin)
            spawnDestinationParticles(destination)
        }

        if (plugin.config.getBoolean("teleport.effects.sounds.enabled", true)) {
            playOriginSound(origin)
            playDestinationSound(destination)
        }
    }

    private fun spawnOriginParticles(origin: Location) {
        val world = origin.world ?: return
        world.spawnParticle(Particle.PORTAL, origin.x, origin.y + 1.0, origin.z, 36, 0.35, 0.55, 0.35, 0.18)
        world.spawnParticle(Particle.REVERSE_PORTAL, origin.x, origin.y + 1.0, origin.z, 10, 0.2, 0.35, 0.2, 0.03)
    }

    private fun spawnDestinationParticles(destination: Location) {
        val world = destination.world ?: return
        world.spawnParticle(Particle.PORTAL, destination.x, destination.y + 1.0, destination.z, 54, 0.45, 0.7, 0.45, 0.22)
        world.spawnParticle(Particle.ENCHANT, destination.x, destination.y + 1.0, destination.z, 20, 0.3, 0.45, 0.3, 0.05)
    }

    private fun playOriginSound(origin: Location) {
        val world = origin.world ?: return
        world.playSound(origin, Sound.ENTITY_ENDERMAN_TELEPORT, 0.75f, 1.1f)
    }

    private fun playDestinationSound(destination: Location) {
        val world = destination.world ?: return
        world.playSound(destination, Sound.ENTITY_PLAYER_TELEPORT, 0.9f, 1.0f)
        world.playSound(destination, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.45f, 1.6f)
    }
}
