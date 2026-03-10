package me.dragan.foxcore.home

sealed interface HomeDeleteResult {
    data object Loading : HomeDeleteResult
    data class PlayerNotFound(val playerName: String) : HomeDeleteResult
    data class HomeNotFound(val playerName: String, val homeName: String) : HomeDeleteResult
    data class Success(val playerName: String, val homeName: String) : HomeDeleteResult
}
