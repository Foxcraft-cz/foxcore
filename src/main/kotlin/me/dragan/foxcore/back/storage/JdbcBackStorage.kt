package me.dragan.foxcore.back.storage

import com.zaxxer.hikari.HikariDataSource
import me.dragan.foxcore.back.BackData
import me.dragan.foxcore.back.StoredLocation
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.util.UUID

abstract class JdbcBackStorage(
    private val dataSource: HikariDataSource,
    protected val tableName: String,
) : BackStorage {

    override fun initialize() {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(createTableSql())
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

                    return BackData(
                        playerId = result.getString("player_uuid"),
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

                    return BackData(
                        playerId = result.getString("player_uuid"),
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
                    )
                }
            }
        }
    }

    override fun save(playerId: UUID, data: BackData) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(upsertSql()).use { statement ->
                fillBaseColumns(statement, playerId, data)
                bindUpsertTail(statement, playerId, data)
                statement.executeUpdate()
            }
        }
    }

    override fun close() {
        dataSource.close()
    }

    protected abstract fun createTableSql(): String

    protected abstract fun upsertSql(): String

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
}
