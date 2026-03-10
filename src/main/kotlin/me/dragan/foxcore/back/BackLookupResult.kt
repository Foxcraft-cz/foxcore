package me.dragan.foxcore.back

import org.bukkit.Location

sealed interface BackLookupResult {
    data object Loading : BackLookupResult
    data object None : BackLookupResult
    data class MissingWorld(val worldName: String) : BackLookupResult
    data class Success(val location: Location) : BackLookupResult
}

