package me.dragan.foxcore.portal

import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.entity.Player
import java.util.concurrent.ThreadLocalRandom

enum class PortalParticlePreset(
    val key: String,
) {
    OVERWORLD("overworld"),
    NETHER("nether"),
    END("end"),
    GOLD("gold"),
    AQUA("aqua");

    fun spawnInterior(player: Player, point: PortalParticlePoint, random: ThreadLocalRandom) {
        when (this) {
            OVERWORLD -> {
                spawnDust(player, point, 122, 201, 67, 1.15f, 0.05, 0.06, 0.05, 0.008, random)
                if (random.nextInt(5) == 0) {
                    player.spawnParticle(Particle.HAPPY_VILLAGER, point.x, point.y + 0.1, point.z, 1, 0.06, 0.05, 0.06, 0.0)
                }
            }

            NETHER -> {
                spawnDust(player, point, 232, 88, 44, 1.15f, 0.03, 0.12, 0.03, 0.015, random)
                if (random.nextInt(4) == 0) {
                    player.spawnParticle(Particle.SMOKE, point.x, point.y + 0.05, point.z, 1, 0.02, 0.08, 0.02, 0.01)
                }
                if (random.nextInt(7) == 0) {
                    player.spawnParticle(Particle.FLAME, point.x, point.y, point.z, 1, 0.01, 0.03, 0.01, 0.0)
                }
            }

            END -> {
                player.spawnParticle(Particle.PORTAL, point.x, point.y, point.z, 1, 0.05, 0.12, 0.05, 0.03)
                if (random.nextInt(4) == 0) {
                    player.spawnParticle(Particle.REVERSE_PORTAL, point.x, point.y, point.z, 1, 0.03, 0.08, 0.03, 0.02)
                }
            }

            GOLD -> {
                spawnDust(player, point, 245, 200, 66, 1.15f, 0.04, 0.08, 0.04, 0.01, random)
                if (random.nextInt(4) == 0) {
                    player.spawnParticle(Particle.ENCHANT, point.x, point.y + 0.08, point.z, 1, 0.05, 0.06, 0.05, 0.02)
                }
            }

            AQUA -> {
                spawnDust(player, point, 75, 200, 233, 1.15f, 0.06, 0.04, 0.06, 0.004, random)
                if (random.nextInt(4) == 0) {
                    player.spawnParticle(Particle.GLOW, point.x, point.y, point.z, 1, 0.04, 0.03, 0.04, 0.0)
                }
            }
        }
    }

    fun spawnFrame(player: Player, point: PortalParticlePoint, random: ThreadLocalRandom) {
        when (this) {
            OVERWORLD -> {
                spawnDust(player, point, 185, 234, 133, 0.9f, 0.015, 0.03, 0.015, 0.0, random)
                if (random.nextInt(6) == 0) {
                    player.spawnParticle(Particle.SPORE_BLOSSOM_AIR, point.x, point.y, point.z, 1, 0.0, 0.02, 0.0, 0.0)
                }
            }

            NETHER -> {
                if (random.nextInt(5) == 0) {
                    player.spawnParticle(Particle.FLAME, point.x, point.y, point.z, 1, 0.01, 0.04, 0.01, 0.0)
                } else {
                    spawnDust(player, point, 255, 145, 84, 0.9f, 0.015, 0.04, 0.015, 0.0, random)
                }
            }

            END -> {
                player.spawnParticle(Particle.PORTAL, point.x, point.y, point.z, 1, 0.02, 0.03, 0.02, 0.01)
                if (random.nextInt(5) == 0) {
                    player.spawnParticle(Particle.END_ROD, point.x, point.y, point.z, 1, 0.01, 0.02, 0.01, 0.0)
                }
            }

            GOLD -> {
                spawnDust(player, point, 255, 227, 120, 0.9f, 0.015, 0.03, 0.015, 0.0, random)
                if (random.nextInt(5) == 0) {
                    player.spawnParticle(Particle.ELECTRIC_SPARK, point.x, point.y, point.z, 1, 0.01, 0.02, 0.01, 0.0)
                }
            }

            AQUA -> {
                spawnDust(player, point, 145, 240, 255, 0.9f, 0.015, 0.025, 0.015, 0.0, random)
                if (random.nextInt(5) == 0) {
                    player.spawnParticle(Particle.BUBBLE_POP, point.x, point.y, point.z, 1, 0.01, 0.02, 0.01, 0.0)
                }
            }
        }
    }

    private fun spawnDust(
        player: Player,
        point: PortalParticlePoint,
        red: Int,
        green: Int,
        blue: Int,
        size: Float,
        offsetX: Double,
        offsetY: Double,
        offsetZ: Double,
        speed: Double,
        random: ThreadLocalRandom,
    ) {
        player.spawnParticle(
            Particle.DUST,
            point.x + random.nextDouble(-0.015, 0.015),
            point.y + random.nextDouble(0.0, 0.08),
            point.z + random.nextDouble(-0.015, 0.015),
            1,
            offsetX,
            offsetY,
            offsetZ,
            speed,
            Particle.DustOptions(Color.fromRGB(red, green, blue), size),
        )
    }

    companion object {
        fun fromKey(input: String?): PortalParticlePreset? =
            entries.firstOrNull { it.key.equals(input?.trim(), ignoreCase = true) }
    }
}
