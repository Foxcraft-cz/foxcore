package me.dragan.foxcore.home

sealed interface HomeIconChangeResult {
    data object Loading : HomeIconChangeResult
    data class NotFound(val homeName: String) : HomeIconChangeResult
    data class Success(val homeName: String, val materialKey: String) : HomeIconChangeResult
}
