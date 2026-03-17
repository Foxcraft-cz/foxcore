package me.dragan.foxcore.back.storage

import com.zaxxer.hikari.HikariDataSource
import me.dragan.foxcore.back.BackData
import me.dragan.foxcore.back.StoredLocation
import me.dragan.foxcore.home.HomeData
import me.dragan.foxcore.warp.WarpData
import me.dragan.foxcore.warp.WarpScope
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.util.UUID

abstract class JdbcBackStorage(
    private val dataSource: HikariDataSource,
    protected val tableName: String,
    protected val homeTableName: String,
    protected val warpTableName: String,
) : BackStorage {

    override fun initialize() {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(createTableSql())
                statement.executeUpdate(createHomeTableSql())
                statement.executeUpdate(createWarpTableSql())
                migrateSchema(statement)
            }
        }
    }

    override fun load(playerId: UUID): BackData? {
        dataSource.connection.use { connection ->
            connection.prepareStatement(selectSql()).use { statement ->
                statement.setString(1, playerId.toString())
                statement.executeQuery().use { result ->
                    if (!result.next()) {
                        return null
                    }

                    val playerUuid = result.getString("player_uuid")
                    return BackData(
                        playerId = playerUuid,
                        playerName = result.getString("player_name"),
                        lastLocation = readLocation(
                            world = result.getString("last_world"),
                            x = result.getDouble("last_x"),
                            y = result.getDouble("last_y"),
                            z = result.getDouble("last_z"),
                            yaw = result.getFloat("last_yaw"),
                            pitch = result.getFloat("last_pitch"),
                        ),
                        lastLocationAtMillis = result.getLongOrNull("last_location_at"),
                        lastDeathLocation = readLocation(
                            world = result.getString("death_world"),
                            x = result.getDouble("death_x"),
                            y = result.getDouble("death_y"),
                            z = result.getDouble("death_z"),
                            yaw = result.getFloat("death_yaw"),
                            pitch = result.getFloat("death_pitch"),
                        ),
                        lastDeathAtMillis = result.getLongOrNull("death_location_at"),
                        homes = loadHomes(connection, playerUuid),
                    )
                }
            }
        }
    }

    override fun findByLastKnownName(name: String): BackData? {
        dataSource.connection.use { connection ->
            connection.prepareStatement(findByNameSql()).use { statement ->
                statement.setString(1, name.lowercase())
                statement.executeQuery().use { result ->
                    if (!result.next()) {
                        return null
                    }

                    val playerUuid = result.getString("player_uuid")
                    return BackData(
                        playerId = playerUuid,
                        playerName = result.getString("player_name"),
                        lastLocation = readLocation(
                            world = result.getString("last_world"),
                            x = result.getDouble("last_x"),
                            y = result.getDouble("last_y"),
                            z = result.getDouble("last_z"),
                            yaw = result.getFloat("last_yaw"),
                            pitch = result.getFloat("last_pitch"),
                        ),
                        lastLocationAtMillis = result.getLongOrNull("last_location_at"),
                        lastDeathLocation = readLocation(
                            world = result.getString("death_world"),
                            x = result.getDouble("death_x"),
                            y = result.getDouble("death_y"),
                            z = result.getDouble("death_z"),
                            yaw = result.getFloat("death_yaw"),
                            pitch = result.getFloat("death_pitch"),
                        ),
                        lastDeathAtMillis = result.getLongOrNull("death_location_at"),
                        homes = loadHomes(connection, playerUuid),
                    )
                }
            }
        }
    }

    override fun save(playerId: UUID, data: BackData) {
        dataSource.connection.use { connection ->
            val previousAutoCommit = connection.autoCommit
            connection.autoCommit = false
            try {
                connection.prepareStatement(upsertSql()).use { statement ->
                    fillBaseColumns(statement, playerId, data)
                    bindUpsertTail(statement, playerId, data)
                    statement.executeUpdate()
                }
                connection.prepareStatement(deleteHomesSql()).use { statement ->
                    statement.setString(1, playerId.toString())
                    statement.executeUpdate()
                }
                connection.prepareStatement(insertHomeSql()).use { statement ->
                    data.homes.entries
                        .sortedBy { it.key }
                        .forEach { (homeName, homeData) ->
                            statement.setString(1, playerId.toString())
                            statement.setString(2, homeName)
                            bindLocation(statement, 3, homeData.location)
                            statement.setString(9, homeData.iconMaterialKey)
                            statement.addBatch()
                        }
                    statement.executeBatch()
                }
                connection.commit()
            } catch (exception: Exception) {
                connection.rollback()
                throw exception
            } finally {
                connection.autoCommit = previousAutoCommit
            }
        }
    }

    override fun loadAllWarps(): Map<String, WarpData> {
        dataSource.connection.use { connection ->
            connection.prepareStatement(selectWarpsSql()).use { statement ->
                statement.executeQuery().use { result ->
                    val warps = linkedMapOf<String, WarpData>()
                    while (result.next()) {
                        val location = readLocation(
                            world = result.getString("world_name"),
                            x = result.getDouble("x"),
                            y = result.getDouble("y"),
                            z = result.getDouble("z"),
                            yaw = result.getFloat("yaw"),
                            pitch = result.getFloat("pitch"),
                        ) ?: continue

                        val name = result.getString("warp_name")
                        warps[name] = WarpData(
                            name = name,
                            scope = WarpScope.valueOf(result.getString("scope")),
                            location = location,
                            ownerId = result.getString("owner_uuid")?.let(UUID::fromString),
                            ownerName = result.getString("owner_name"),
                            iconMaterialKey = result.getString("icon_material"),
                            title = result.getString("title"),
                            description = result.getString("description"),
                        )
                    }
                    return warps.toSortedMap()
                }
            }
        }
    }

    override fun saveWarp(name: String, data: WarpData) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(upsertWarpSql()).use { statement ->
                statement.setString(1, name)
                statement.setString(2, data.scope.name)
                statement.setString(3, data.ownerId?.toString())
                statement.setString(4, data.ownerName)
                bindLocation(statement, 5, data.location)
                statement.setString(11, data.iconMaterialKey)
                statement.setString(12, data.title)
                statement.setString(13, data.description)
                bindWarpUpsertTail(statement, name, data)
                statement.executeUpdate()
            }
        }
    }

    override fun deleteWarp(name: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(deleteWarpSql()).use { statement ->
                statement.setString(1, name)
                statement.executeUpdate()
            }
        }
    }

    override fun renameWarp(oldName: String, newName: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(renameWarpSql()).use { statement ->
                statement.setString(1, newName)
                statement.setString(2, oldName)
                statement.executeUpdate()
            }
        }
    }

    override fun close() {
        dataSource.close()
    }

    protected abstract fun createTableSql(): String

    protected abstract fun createHomeTableSql(): String

    protected abstract fun createWarpTableSql(): String

    protected abstract fun upsertSql(): String

    protected abstract fun upsertWarpSql(): String

    protected abstract fun bindWarpUpsertTail(statement: PreparedStatement, name: String, data: WarpData)

    protected abstract fun renameWarpSql(): String

    protected abstract fun bindUpsertTail(statement: PreparedStatement, playerId: UUID, data: BackData)

    protected abstract fun migrateSchema(statement: java.sql.Statement)

    protected open fun selectSql(): String =
        """
        SELECT player_uuid, player_name,
               last_world, last_x, last_y, last_z, last_yaw, last_pitch, last_location_at,
               death_world, death_x, death_y, death_z, death_yaw, death_pitch, death_location_at
        FROM $tableName
        WHERE player_uuid = ?
        """.trimIndent()

    protected open fun findByNameSql(): String =
        """
        SELECT player_uuid, player_name,
               last_world, last_x, last_y, last_z, last_yaw, last_pitch, last_location_at,
               death_world, death_x, death_y, death_z, death_yaw, death_pitch, death_location_at
        FROM $tableName
        WHERE LOWER(player_name) = ?
        ORDER BY COALESCE(last_location_at, 0) DESC
        LIMIT 1
        """.trimIndent()

    protected open fun deleteHomesSql(): String =
        """
        DELETE FROM $homeTableName
        WHERE player_uuid = ?
        """.trimIndent()

    protected open fun insertHomeSql(): String =
        """
        INSERT INTO $homeTableName (
            player_uuid, home_name,
            world_name, x, y, z, yaw, pitch, icon_material
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

    protected open fun deleteWarpSql(): String =
        """
        DELETE FROM $warpTableName
        WHERE warp_name = ?
        """.trimIndent()

    protected open fun selectWarpsSql(): String =
        """
        SELECT warp_name, scope, owner_uuid, owner_name,
               world_name, x, y, z, yaw, pitch,
               icon_material, title, description
        FROM $warpTableName
        ORDER BY CASE WHEN scope = 'SERVER' THEN 0 ELSE 1 END, warp_name ASC
        """.trimIndent()

    protected fun fillBaseColumns(statement: PreparedStatement, playerId: UUID, data: BackData) {
        statement.setString(1, playerId.toString())
        statement.setString(2, data.playerName)
        bindLocation(statement, 3, data.lastLocation)
        bindLong(statement, 9, data.lastLocationAtMillis)
        bindLocation(statement, 10, data.lastDeathLocation)
        bindLong(statement, 16, data.lastDeathAtMillis)
    }

    protected fun bindLocation(statement: PreparedStatement, startIndex: Int, location: StoredLocation?) {
        if (location == null) {
            statement.setNull(startIndex, Types.VARCHAR)
            statement.setNull(startIndex + 1, Types.DOUBLE)
            statement.setNull(startIndex + 2, Types.DOUBLE)
            statement.setNull(startIndex + 3, Types.DOUBLE)
            statement.setNull(startIndex + 4, Types.FLOAT)
            statement.setNull(startIndex + 5, Types.FLOAT)
            return
        }

        statement.setString(startIndex, location.worldName)
        statement.setDouble(startIndex + 1, location.x)
        statement.setDouble(startIndex + 2, location.y)
        statement.setDouble(startIndex + 3, location.z)
        statement.setFloat(startIndex + 4, location.yaw)
        statement.setFloat(startIndex + 5, location.pitch)
    }

    protected fun bindLong(statement: PreparedStatement, index: Int, value: Long?) {
        if (value == null) {
            statement.setNull(index, Types.BIGINT)
        } else {
            statement.setLong(index, value)
        }
    }

    private fun readLocation(
        world: String?,
        x: Double,
        y: Double,
        z: Double,
        yaw: Float,
        pitch: Float,
    ): StoredLocation? =
        if (world == null) null else StoredLocation(world, x, y, z, yaw, pitch)

    private fun ResultSet.getLongOrNull(column: String): Long? {
        val value = getLong(column)
        return if (wasNull()) null else value
    }

    private fun loadHomes(connection: java.sql.Connection, playerId: String): Map<String, HomeData> {
        connection.prepareStatement(selectHomesSql()).use { statement ->
            statement.setString(1, playerId)
            statement.executeQuery().use { result ->
                val homes = linkedMapOf<String, HomeData>()
                while (result.next()) {
                    val location = readLocation(
                        world = result.getString("world_name"),
                        x = result.getDouble("x"),
                        y = result.getDouble("y"),
                        z = result.getDouble("z"),
                        yaw = result.getFloat("yaw"),
                        pitch = result.getFloat("pitch"),
                    ) ?: continue
                    homes[result.getString("home_name")] = HomeData(
                        location = location,
                        iconMaterialKey = result.getString("icon_material"),
                    )
                }
                return homes.toSortedMap()
            }
        }
    }

    private fun selectHomesSql(): String =
        """
        SELECT home_name, world_name, x, y, z, yaw, pitch, icon_material
        FROM $homeTableName
        WHERE player_uuid = ?
        ORDER BY home_name ASC
        """.trimIndent()
}
