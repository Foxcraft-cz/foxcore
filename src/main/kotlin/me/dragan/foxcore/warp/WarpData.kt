package me.dragan.foxcore.warp

import me.dragan.foxcore.back.StoredLocation
import org.bukkit.Material
import java.util.UUID

data class WarpData(
    val name: String,
    val scope: WarpScope,
    val location: StoredLocation,
    val ownerId: UUID? = null,
    val ownerName: String? = null,
    val iconMaterialKey: String? = null,
    val title: String? = null,
    val description: String? = null,
) {
    fun iconMaterial(): Material =
        iconMaterialKey
            ?.let(Material::matchMaterial)
            ?.takeIf(Material::isItem)
            ?: when (scope) {
                WarpScope.SERVER -> Material.LODESTONE
                WarpScope.PLAYER -> Material.ENDER_PEARL
            }

    fun withLocation(location: StoredLocation): WarpData =
        copy(location = location)

    fun withIcon(material: Material): WarpData =
        copy(iconMaterialKey = material.key.toString())

    fun withTitle(title: String?): WarpData =
        copy(title = title)

    fun withDescription(description: String?): WarpData =
        copy(description = description)
}

enum class WarpScope {
    SERVER,
    PLAYER,
}
