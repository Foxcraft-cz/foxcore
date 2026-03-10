package me.dragan.foxcore.tpa

import java.util.UUID

data class TpaRequest(
    val requesterId: UUID,
    val requesterName: String,
    val targetId: UUID,
    val type: TpaRequestType,
    val expiresAtMillis: Long,
)

enum class TpaRequestType {
    TELEPORT_TO_TARGET,
    TELEPORT_TARGET_HERE,
}
