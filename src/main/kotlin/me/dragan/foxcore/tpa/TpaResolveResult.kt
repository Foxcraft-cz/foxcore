package me.dragan.foxcore.tpa

import org.bukkit.entity.Player

data class TpaResolveResult(
    val status: TpaResolveStatus,
    val request: TpaRequest? = null,
    val requester: Player? = null,
)

enum class TpaResolveStatus {
    SUCCESS,
    NO_PENDING,
    NOT_FOUND_FOR_REQUESTER,
    REQUESTER_OFFLINE,
}

