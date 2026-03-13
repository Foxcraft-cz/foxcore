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
            OVERWORLD -> player.spawnParticle(
                Particle.DUST,
                point.x,
                point.y + random.nextDouble(0.0, 0.08),
                point.z,
                1,
                0.04,
                0.08,
                0.04,
                0.01,
                Particle.DustOptions(Color.fromRGB(122, 201, 67), 1.2f),
            )

            NETHER -> player.spawnParticle(
                Particle.DUST,
                point.x,
                point.y + random.nextDouble(0.0, 0.1),
                point.z,
                1,
                0.03,
                0.1,
                0.03,
                0.01,
                Particle.DustOptions(Color.fromRGB(232, 88, 44), 1.2f),
            )

            END -> player.spawnParticle(
                Particle.PORTAL,
                point.x,
                point.y,
                point.z,
                1,
                0.06,
                0.12,
                0.06,
                0.02,
            )

            GOLD -> player.spawnParticle(
                Particle.DUST,
                point.x,
                point.y + random.nextDouble(0.0, 0.08),
                point.z,
                1,
                0.04,
                0.08,
                0.04,
                0.01,
                Particle.DustOptions(Color.fromRGB(245, 200, 66), 1.2f),
            )

            AQUA -> player.spawnParticle(
                Particle.DUST,
                point.x,
                point.y + random.nextDouble(0.0, 0.08),
                point.z,
                1,
                0.05,
                0.08,
                0.05,
                0.01,
                Particle.DustOptions(Color.fromRGB(75, 200, 233), 1.2f),
            )
        }
    }

    fun spawnFrame(player: Player, point: PortalParticlePoint, random: ThreadLocalRandom) {
        when (this) {
            OVERWORLD -> player.spawnParticle(
                Particle.DUST,
                point.x,
                point.y,
                point.z,
                1,
                0.01,
                0.03,
                0.01,
                0.0,
                Particle.DustOptions(Color.fromRGB(185, 234, 133), 0.9f),
            )

            NETHER -> {
                if (random.nextInt(5) == 0) {
                    player.spawnParticle(Particle.FLAME, point.x, point.y, point.z, 1, 0.01, 0.04, 0.01, 0.0)
                } else {
                    player.spawnParticle(
                        Particle.DUST,
                        point.x,
                        point.y,
                        point.z,
                        1,
                        0.01,
                        0.03,
                        0.01,
                        0.0,
                        Particle.DustOptions(Color.fromRGB(255, 145, 84), 0.9f),
                    )
                }
            }

            END -> player.spawnParticle(Particle.PORTAL, point.x, point.y, point.z, 1, 0.02, 0.03, 0.02, 0.01)

            GOLD -> player.spawnParticle(
                Particle.DUST,
                point.x,
                point.y,
                point.z,
                1,
                0.01,
                0.03,
                0.01,
                0.0,
                Particle.DustOptions(Color.fromRGB(255, 227, 120), 0.9f),
            )

            AQUA -> player.spawnParticle(
                Particle.DUST,
                point.x,
                point.y,
                point.z,
                1,
                0.01,
                0.03,
                0.01,
                0.0,
                Particle.DustOptions(Color.fromRGB(145, 240, 255), 0.9f),
            )
        }
    }

    companion object {
        fun fromKey(input: String?): PortalParticlePreset? =
            entries.firstOrNull { it.key.equals(input?.trim(), ignoreCase = true) }
    }
}
