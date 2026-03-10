package me.dragan.foxcore.home

object HomeNames {
    const val DEFAULT = "home"

    private val validPattern = Regex("^[a-z0-9_-]{1,32}$")

    fun normalize(input: String): String =
        input.trim().lowercase()

    fun isValid(input: String): Boolean =
        validPattern.matches(normalize(input))
}
