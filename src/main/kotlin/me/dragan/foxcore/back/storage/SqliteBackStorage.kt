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
    homeTableName = "player_home",
    warpTableName = "warp",
) {
    override fun createTableSql(): String =
        """
        CREATE TABLE IF NOT EXISTS $tableName (
            player_uuid TEXT NOT NULL PRIMARY KEY,
            player_name TEXT NULL,
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
            player_uuid, player_name,
            last_world, last_x, last_y, last_z, last_yaw, last_pitch, last_location_at,
            death_world, death_x, death_y, death_z, death_yaw, death_pitch, death_location_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(player_uuid) DO UPDATE SET
            player_name = excluded.player_name,
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

    override fun createHomeTableSql(): String =
        """
        CREATE TABLE IF NOT EXISTS $homeTableName (
            player_uuid TEXT NOT NULL,
            home_name TEXT NOT NULL,
            world_name TEXT NOT NULL,
            x REAL NOT NULL,
            y REAL NOT NULL,
            z REAL NOT NULL,
            yaw REAL NOT NULL,
            pitch REAL NOT NULL,
            icon_material TEXT NULL,
            PRIMARY KEY (player_uuid, home_name)
        )
        """.trimIndent()

    override fun createWarpTableSql(): String =
        """
        CREATE TABLE IF NOT EXISTS $warpTableName (
            warp_name TEXT NOT NULL PRIMARY KEY,
            scope TEXT NOT NULL,
            owner_uuid TEXT NULL,
            owner_name TEXT NULL,
            world_name TEXT NOT NULL,
            x REAL NOT NULL,
            y REAL NOT NULL,
            z REAL NOT NULL,
            yaw REAL NOT NULL,
            pitch REAL NOT NULL,
            icon_material TEXT NULL,
            title TEXT NULL,
            description TEXT NULL
        )
        """.trimIndent()

    override fun bindUpsertTail(statement: PreparedStatement, playerId: UUID, data: BackData) = Unit

    override fun upsertWarpSql(): String =
        """
        INSERT INTO $warpTableName (
            warp_name, scope, owner_uuid, owner_name,
            world_name, x, y, z, yaw, pitch,
            icon_material, title, description
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(warp_name) DO UPDATE SET
            scope = excluded.scope,
            owner_uuid = excluded.owner_uuid,
            owner_name = excluded.owner_name,
            world_name = excluded.world_name,
            x = excluded.x,
            y = excluded.y,
            z = excluded.z,
            yaw = excluded.yaw,
            pitch = excluded.pitch,
            icon_material = excluded.icon_material,
            title = excluded.title,
            description = excluded.description
        """.trimIndent()

    override fun bindWarpUpsertTail(statement: PreparedStatement, name: String, data: me.dragan.foxcore.warp.WarpData) = Unit

    override fun renameWarpSql(): String =
        """
        UPDATE $warpTableName
        SET warp_name = ?
        WHERE warp_name = ?
        """.trimIndent()

    override fun migrateSchema(statement: java.sql.Statement) {
        statement.runCatching {
            executeUpdate("ALTER TABLE $tableName ADD COLUMN player_name TEXT NULL")
        }
        statement.runCatching {
            executeUpdate("ALTER TABLE $homeTableName ADD COLUMN icon_material TEXT NULL")
        }
        statement.runCatching {
            executeUpdate("ALTER TABLE $warpTableName ADD COLUMN icon_material TEXT NULL")
        }
        statement.runCatching {
            executeUpdate("ALTER TABLE $warpTableName ADD COLUMN title TEXT NULL")
        }
        statement.runCatching {
            executeUpdate("ALTER TABLE $warpTableName ADD COLUMN description TEXT NULL")
        }
    }

    companion object {
        private fun createDataSource(plugin: JavaPlugin, config: FileConfiguration): HikariDataSource {
            val fileName = config.getString("storage.sqlite.file", "storage/foxcore.db").orEmpty().ifBlank {
                "storage/foxcore.db"
            }
            val file = File(plugin.dataFolder, fileName)
            file.parentFile?.mkdirs()

            val hikari = HikariConfig().apply {
                jdbcUrl = "jdbc:sqlite:${file.absolutePath}"
                driverClassName = "org.sqlite.JDBC"
                maximumPoolSize = 1
                poolName = "FoxCore-SQLite"
            }

            return HikariDataSource(hikari)
        }
    }
}
