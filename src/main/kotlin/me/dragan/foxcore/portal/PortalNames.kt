package me.dragan.foxcore.portal

object PortalNames {
    private val validName = Regex("^[a-z0-9_-]{1,32}$")

    fun normalize(input: String): String =
        input.trim().lowercase()

    fun isValid(input: String): Boolean =
        validName.matches(normalize(input))
}
