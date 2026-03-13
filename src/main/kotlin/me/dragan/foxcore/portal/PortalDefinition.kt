package me.dragan.foxcore.portal

data class PortalDefinition(
    val id: String,
    val bounds: PortalBounds,
    val action: PortalAction,
    val enabled: Boolean = true,
    val cooldownSeconds: Long = 2L,
    val particlePreset: PortalParticlePreset = PortalParticlePreset.GOLD,
)
