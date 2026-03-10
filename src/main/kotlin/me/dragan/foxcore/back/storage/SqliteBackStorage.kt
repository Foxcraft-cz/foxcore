package me.dragan.foxcore.back.storage

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import me.dragan.foxcore.back.BackData
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.sql.PreparedStatement
import java.util.UUID

class SqliteBackStorage(
    plugin: JavaPlugin,
    config: FileConfiguration,
) : JdbcBackStorage(
    dataSource = createDataSource(plugin, config),
    tableName = "player_back",
) {
    override fun createTableSql(): String =
        """
        CREATE TABLE IF NOT EXISTS $tableName (
            player_uuid TEXT NOT NULL PRIMARY KEY,
            last_world TEXT NULL,
            last_x REAL NULL,
            last_y REAL NULL,
            last_z REAL NULL,
            last_yaw REAL NULL,
            last_pitch REAL NULL,
            last_location_at INTEGER NULL,
            death_world TEXT NULL,
            death_x REAL NULL,
            death_y REAL NULL,
            death_z REAL NULL,
            death_yaw REAL NULL,
            death_pitch REAL NULL,
            death_location_at INTEGER NULL
        )
        """.trimIndent()

    override fun upsertSql(): String =
        """
        INSERT INTO $tableName (
            player_uuid,
            last_world, last_x, last_y, last_z, last_yaw, last_pitch, last_location_at,
            death_world, death_x, death_y, death_z, death_yaw, death_pitch, death_location_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(player_uuid) DO UPDATE SET
            last_world = excluded.last_world,
            last_x = excluded.last_x,
            last_y = excluded.last_y,
            last_z = excluded.last_z,
            last_yaw = excluded.last_yaw,
            last_pitch = excluded.last_pitch,
            last_location_at = excluded.last_location_at,
            death_world = excluded.death_world,
            death_x = excluded.death_x,
            death_y = excluded.death_y,
            death_z = excluded.death_z,
            death_yaw = excluded.death_yaw,
            death_pitch = excluded.death_pitch,
            death_location_at = excluded.death_location_at
        """.trimIndent()

    override fun bindUpsertTail(statement: PreparedStatement, playerId: UUID, data: BackData) = Unit

    companion object {
        private fun createDataSource(plugin: JavaPlugin, config: FileConfiguration): HikariDataSource {
            val fileName = config.getString("storage.sqlite.file", "storage/foxcraft.db").orEmpty().ifBlank {
                "storage/foxcraft.db"
            }
            val file = File(plugin.dataFolder, fileName)
            file.parentFile?.mkdirs()

            val hikari = HikariConfig().apply {
                jdbcUrl = "jdbc:sqlite:${file.absolutePath}"
                driverClassName = "org.sqlite.JDBC"
                maximumPoolSize = 1
                poolName = "FoxCraft-SQLite"
            }

            return HikariDataSource(hikari)
        }
    }
}

