package me.dragan.foxcore.back

import org.bukkit.Location

sealed interface BackLookupResult {
    data object Loading : BackLookupResult
    data object None : BackLookupResult
    data object NoEligiblePermission : BackLookupResult
    data class MissingLocation(
        val type: BackType,
    ) : BackLookupResult
    data class MissingWorld(val worldName: String) : BackLookupResult
    data class Success(
        val location: Location,
        val type: BackType,
    ) : BackLookupResult
}

enum class BackMode {
    AUTO,
    TELEPORT,
    DEATH,
}

enum class BackType {
    TELEPORT,
    DEATH,
}
