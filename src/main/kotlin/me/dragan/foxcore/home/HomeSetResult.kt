package me.dragan.foxcore.home

sealed interface HomeSetResult {
    data object Loading : HomeSetResult
    data class LimitReached(val maxHomes: Int) : HomeSetResult
    data class Success(val homeName: String) : HomeSetResult
}
