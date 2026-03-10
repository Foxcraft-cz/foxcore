package me.dragan.foxcore.back.storage

import me.dragan.foxcore.back.BackData
import java.util.UUID

interface BackStorage {
    fun initialize()
    fun load(playerId: UUID): BackData?
    fun save(playerId: UUID, data: BackData)
    fun close()
}
