package me.dragan.foxcore.portal

import org.bukkit.Location

data class PortalBounds(
    val worldName: String,
    val minX: Int,
    val minY: Int,
    val minZ: Int,
    val maxX: Int,
    val maxY: Int,
    val maxZ: Int,
) {
    fun contains(location: Location): Boolean {
        val world = location.world ?: return false
        if (!world.name.equals(worldName, ignoreCase = true)) {
            return false
        }

        return location.blockX in minX..maxX &&
            location.blockY in minY..maxY &&
            location.blockZ in minZ..maxZ
    }

    fun centerX(): Double = (minX + maxX + 1) / 2.0

    fun centerY(): Double = (minY + maxY + 1) / 2.0

    fun centerZ(): Double = (minZ + maxZ + 1) / 2.0

    fun outlinePoints(step: Double = 1.0): List<PortalParticlePoint> {
        val x1 = minX.toDouble()
        val y1 = minY.toDouble()
        val z1 = minZ.toDouble()
        val x2 = maxX + 1.0
        val y2 = maxY + 1.0
        val z2 = maxZ + 1.0
        val points = LinkedHashSet<PortalParticlePoint>()

        sampleLine(x1, y1, z1, x2, y1, z1, step, points)
        sampleLine(x1, y1, z2, x2, y1, z2, step, points)
        sampleLine(x1, y2, z1, x2, y2, z1, step, points)
        sampleLine(x1, y2, z2, x2, y2, z2, step, points)

        sampleLine(x1, y1, z1, x1, y1, z2, step, points)
        sampleLine(x2, y1, z1, x2, y1, z2, step, points)
        sampleLine(x1, y2, z1, x1, y2, z2, step, points)
        sampleLine(x2, y2, z1, x2, y2, z2, step, points)

        sampleLine(x1, y1, z1, x1, y2, z1, step, points)
        sampleLine(x2, y1, z1, x2, y2, z1, step, points)
        sampleLine(x1, y1, z2, x1, y2, z2, step, points)
        sampleLine(x2, y1, z2, x2, y2, z2, step, points)

        return points.toList()
    }

    fun preferredPlane(): PortalPlane {
        val widthX = (maxX - minX) + 1
        val widthY = (maxY - minY) + 1
        val widthZ = (maxZ - minZ) + 1

        return when {
            widthX <= widthZ && widthX <= widthY -> PortalPlane.YZ
            widthZ <= widthX && widthZ <= widthY -> PortalPlane.XY
            else -> PortalPlane.XZ
        }
    }

    fun randomInteriorPoint(random: java.util.concurrent.ThreadLocalRandom): PortalParticlePoint {
        val inset = 0.15
        return when (preferredPlane()) {
            PortalPlane.YZ -> PortalParticlePoint(
                x = centerX() + random.nextDouble(-0.08, 0.08),
                y = minY + inset + random.nextDouble((maxY - minY + 1).toDouble() - (inset * 2.0)),
                z = minZ + inset + random.nextDouble((maxZ - minZ + 1).toDouble() - (inset * 2.0)),
            )

            PortalPlane.XY -> PortalParticlePoint(
                x = minX + inset + random.nextDouble((maxX - minX + 1).toDouble() - (inset * 2.0)),
                y = minY + inset + random.nextDouble((maxY - minY + 1).toDouble() - (inset * 2.0)),
                z = centerZ() + random.nextDouble(-0.08, 0.08),
            )

            PortalPlane.XZ -> PortalParticlePoint(
                x = minX + inset + random.nextDouble((maxX - minX + 1).toDouble() - (inset * 2.0)),
                y = centerY() + random.nextDouble(-0.08, 0.08),
                z = minZ + inset + random.nextDouble((maxZ - minZ + 1).toDouble() - (inset * 2.0)),
            )
        }
    }

    fun randomFramePoint(random: java.util.concurrent.ThreadLocalRandom): PortalParticlePoint {
        val edgePoints = outlinePoints(step = 1.25)
        return edgePoints[random.nextInt(edgePoints.size)]
    }

    companion object {
        fun fromCorners(first: Location, second: Location): PortalBounds? {
            val firstWorld = first.world ?: return null
            val secondWorld = second.world ?: return null
            if (!firstWorld.name.equals(secondWorld.name, ignoreCase = true)) {
                return null
            }

            return PortalBounds(
                worldName = firstWorld.name,
                minX = minOf(first.blockX, second.blockX),
                minY = minOf(first.blockY, second.blockY),
                minZ = minOf(first.blockZ, second.blockZ),
                maxX = maxOf(first.blockX, second.blockX),
                maxY = maxOf(first.blockY, second.blockY),
                maxZ = maxOf(first.blockZ, second.blockZ),
            )
        }

        private fun sampleLine(
            startX: Double,
            startY: Double,
            startZ: Double,
            endX: Double,
            endY: Double,
            endZ: Double,
            step: Double,
            points: MutableSet<PortalParticlePoint>,
        ) {
            val distance = kotlin.math.sqrt(
                ((endX - startX) * (endX - startX)) +
                    ((endY - startY) * (endY - startY)) +
                    ((endZ - startZ) * (endZ - startZ)),
            )
            val segments = maxOf(1, kotlin.math.ceil(distance / step).toInt())
            for (index in 0..segments) {
                val progress = index.toDouble() / segments.toDouble()
                points += PortalParticlePoint(
                    x = startX + ((endX - startX) * progress),
                    y = startY + ((endY - startY) * progress),
                    z = startZ + ((endZ - startZ) * progress),
                )
            }
        }
    }
}

data class PortalParticlePoint(
    val x: Double,
    val y: Double,
    val z: Double,
)

enum class PortalPlane {
    XY,
    XZ,
    YZ,
}
