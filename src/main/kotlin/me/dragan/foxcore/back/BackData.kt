package me.dragan.foxcore.back

data class BackData(
    val playerId: String,
    val lastLocation: StoredLocation? = null,
    val lastLocationAtMillis: Long? = null,
    val lastDeathLocation: StoredLocation? = null,
    val lastDeathAtMillis: Long? = null,
)

