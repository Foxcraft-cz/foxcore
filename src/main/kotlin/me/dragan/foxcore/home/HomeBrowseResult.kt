package me.dragan.foxcore.home

import me.dragan.foxcore.back.StoredLocation

sealed interface HomeBrowseResult {
    data object Loading : HomeBrowseResult
    data class NotFound(val requestedPlayerName: String) : HomeBrowseResult
    data class Empty(val playerName: String) : HomeBrowseResult
    data class Success(val playerName: String, val homes: Map<String, HomeData>) : HomeBrowseResult
}
