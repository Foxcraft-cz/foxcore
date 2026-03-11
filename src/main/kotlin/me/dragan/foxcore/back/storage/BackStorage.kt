package me.dragan.foxcore.back.storage

import me.dragan.foxcore.back.BackData
import me.dragan.foxcore.warp.WarpData
import java.util.UUID

interface BackStorage {
    fun initialize()
    fun load(playerId: UUID): BackData?
    fun findByLastKnownName(name: String): BackData?
    fun loadAllWarps(): Map<String, WarpData>
    fun saveWarp(name: String, data: WarpData)
    fun deleteWarp(name: String)
    fun renameWarp(oldName: String, newName: String)
    fun save(playerId: UUID, data: BackData)
    fun close()
}
