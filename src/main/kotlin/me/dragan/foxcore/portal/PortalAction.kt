package me.dragan.foxcore.portal

sealed interface PortalAction {
    data object Spawn : PortalAction

    data class Warp(
        val warpName: String,
    ) : PortalAction

    data class Rtp(
        val worldName: String,
    ) : PortalAction

    data class Command(
        val command: String,
    ) : PortalAction
}
