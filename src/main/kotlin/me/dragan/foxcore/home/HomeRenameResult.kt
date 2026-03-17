package me.dragan.foxcore.home

sealed interface HomeRenameResult {
    data object Loading : HomeRenameResult
    data class NotFound(val homeName: String) : HomeRenameResult
    data class AlreadyExists(val homeName: String) : HomeRenameResult
    data class SameName(val homeName: String) : HomeRenameResult
    data class Success(val oldHomeName: String, val newHomeName: String) : HomeRenameResult
}
