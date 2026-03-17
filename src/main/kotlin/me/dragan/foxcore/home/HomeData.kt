package me.dragan.foxcore.home

import me.dragan.foxcore.back.StoredLocation
import org.bukkit.Material

data class HomeData(
    val location: StoredLocation,
    val iconMaterialKey: String? = null,
) {
    fun iconMaterial(): Material =
        iconMaterialKey
            ?.let(Material::matchMaterial)
            ?.takeIf { it.isItem }
            ?: Material.RED_BED

    fun withIcon(material: Material): HomeData =
        copy(iconMaterialKey = material.key.toString())
}
