package me.dragan.foxcore.warp

object WarpNames {
    private val validPattern = Regex("^[a-z0-9_-]{1,32}$")
    private val reservedNames = setOf(
        "create",
        "delete",
        "rename",
        "movehere",
        "icon",
        "title",
        "description",
    )

    fun normalize(input: String): String =
        input.trim().lowercase()

    fun isValid(input: String): Boolean {
        val normalized = normalize(input)
        return validPattern.matches(normalized) && normalized !in reservedNames
    }
}
