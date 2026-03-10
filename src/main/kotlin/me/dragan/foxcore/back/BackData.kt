package me.dragan.foxcore.back

import me.dragan.foxcore.home.HomeData

data class BackData(
    val playerId: String,
    val playerName: String? = null,
    val lastLocation: StoredLocation? = null,
    val lastLocationAtMillis: Long? = null,
    val lastDeathLocation: StoredLocation? = null,
    val lastDeathAtMillis: Long? = null,
    val homes: Map<String, HomeData> = emptyMap(),
)
