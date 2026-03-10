package me.dragan.foxcore.home

sealed interface HomeListResult {
    data object Loading : HomeListResult
    data class NotFound(val requestedPlayerName: String) : HomeListResult
    data class Empty(val playerName: String) : HomeListResult
    data class Success(val playerName: String, val homes: List<String>) : HomeListResult
}
