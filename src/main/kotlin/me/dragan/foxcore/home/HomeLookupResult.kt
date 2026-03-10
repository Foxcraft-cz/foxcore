package me.dragan.foxcore.home

import org.bukkit.Location

sealed interface HomeLookupResult {
    data object Loading : HomeLookupResult
    data class NotFound(val homeName: String) : HomeLookupResult
    data class MissingWorld(val homeName: String, val worldName: String) : HomeLookupResult
    data class Success(val homeName: String, val location: Location) : HomeLookupResult
}
