package me.dragan.foxcore.back.storage

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import me.dragan.foxcore.back.BackData
import org.bukkit.configuration.file.FileConfiguration
import java.sql.PreparedStatement
import java.util.UUID

class MysqlBackStorage(
    config: FileConfiguration,
) : JdbcBackStorage(
    dataSource = createDataSource(config),
    tableName = buildTableName(config),
    homeTableName = buildHomeTableName(config),
    warpTableName = buildWarpTableName(config),
) {
    override fun createTableSql(): String =
        """
        CREATE TABLE IF NOT EXISTS $tableName (
            player_uuid VARCHAR(36) NOT NULL PRIMARY KEY,
            player_name VARCHAR(16) NULL,
            last_world VARCHAR(255) NULL,
            last_x DOUBLE NULL,
            last_y DOUBLE NULL,
            last_z DOUBLE NULL,
            last_yaw FLOAT NULL,
            last_pitch FLOAT NULL,
            last_location_at BIGINT NULL,
            death_world VARCHAR(255) NULL,
            death_x DOUBLE NULL,
            death_y DOUBLE NULL,
            death_z DOUBLE NULL,
            death_yaw FLOAT NULL,
            death_pitch FLOAT NULL,
            death_location_at BIGINT NULL
        )
        """.trimIndent()

    override fun upsertSql(): String =
        """
        INSERT INTO $tableName (
            player_uuid, player_name,
            last_world, last_x, last_y, last_z, last_yaw, last_pitch, last_location_at,
            death_world, death_x, death_y, death_z, death_yaw, death_pitch, death_location_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
            player_name = ?,
            last_world = ?,
            last_x = ?,
            last_y = ?,
            last_z = ?,
            last_yaw = ?,
            last_pitch = ?,
            last_location_at = ?,
            death_world = ?,
            death_x = ?,
            death_y = ?,
            death_z = ?,
            death_yaw = ?,
            death_pitch = ?,
            death_location_at = ?
        """.trimIndent()

    override fun createHomeTableSql(): String =
        """
        CREATE TABLE IF NOT EXISTS $homeTableName (
            player_uuid VARCHAR(36) NOT NULL,
            home_name VARCHAR(32) NOT NULL,
            world_name VARCHAR(255) NOT NULL,
            x DOUBLE NOT NULL,
            y DOUBLE NOT NULL,
            z DOUBLE NOT NULL,
            yaw FLOAT NOT NULL,
            pitch FLOAT NOT NULL,
            icon_material VARCHAR(255) NULL,
            PRIMARY KEY (player_uuid, home_name)
        )
        """.trimIndent()

    override fun createWarpTableSql(): String =
        """
        CREATE TABLE IF NOT EXISTS $warpTableName (
            warp_name VARCHAR(32) NOT NULL PRIMARY KEY,
            scope VARCHAR(16) NOT NULL,
            owner_uuid VARCHAR(36) NULL,
            owner_name VARCHAR(16) NULL,
            world_name VARCHAR(255) NOT NULL,
            x DOUBLE NOT NULL,
            y DOUBLE NOT NULL,
            z DOUBLE NOT NULL,
            yaw FLOAT NOT NULL,
            pitch FLOAT NOT NULL,
            icon_material VARCHAR(255) NULL,
            title VARCHAR(255) NULL,
            description TEXT NULL,
            INDEX idx_${warpTableName}_owner_uuid (owner_uuid)
        )
        """.trimIndent()

    override fun bindUpsertTail(statement: PreparedStatement, playerId: UUID, data: BackData) {
        statement.setString(17, data.playerName)
        bindLocation(statement, 18, data.lastLocation)
        bindLong(statement, 24, data.lastLocationAtMillis)
        bindLocation(statement, 25, data.lastDeathLocation)
        bindLong(statement, 31, data.lastDeathAtMillis)
    }

    override fun upsertWarpSql(): String =
        """
        INSERT INTO $warpTableName (
            warp_name, scope, owner_uuid, owner_name,
            world_name, x, y, z, yaw, pitch,
            icon_material, title, description
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
            scope = ?,
            owner_uuid = ?,
            owner_name = ?,
            world_name = ?,
            x = ?,
            y = ?,
            z = ?,
            yaw = ?,
            pitch = ?,
            icon_material = ?,
            title = ?,
            description = ?
        """.trimIndent()

    override fun bindWarpUpsertTail(statement: PreparedStatement, name: String, data: me.dragan.foxcore.warp.WarpData) {
        statement.setString(14, data.scope.name)
        statement.setString(15, data.ownerId?.toString())
        statement.setString(16, data.ownerName)
        bindLocation(statement, 17, data.location)
        statement.setString(23, data.iconMaterialKey)
        statement.setString(24, data.title)
        statement.setString(25, data.description)
    }

    override fun renameWarpSql(): String =
        """
        UPDATE $warpTableName
        SET warp_name = ?
        WHERE warp_name = ?
        """.trimIndent()

    override fun migrateSchema(statement: java.sql.Statement) {
        statement.runCatching {
            executeUpdate("ALTER TABLE $tableName ADD COLUMN player_name VARCHAR(16) NULL")
        }
        statement.runCatching {
            executeUpdate("ALTER TABLE $homeTableName ADD COLUMN icon_material VARCHAR(255) NULL")
        }
        statement.runCatching {
            executeUpdate("ALTER TABLE $warpTableName ADD COLUMN icon_material VARCHAR(255) NULL")
        }
        statement.runCatching {
            executeUpdate("ALTER TABLE $warpTableName ADD COLUMN title VARCHAR(255) NULL")
        }
        statement.runCatching {
            executeUpdate("ALTER TABLE $warpTableName ADD COLUMN description TEXT NULL")
        }
    }

    companion object {
        private fun createDataSource(config: FileConfiguration): HikariDataSource {
            val section = requireNotNull(config.getConfigurationSection("storage.mysql")) {
                "Missing storage.mysql configuration section"
            }

            val hikari = HikariConfig().apply {
                jdbcUrl = "jdbc:mysql://${section.getString("host", "127.0.0.1")}:${section.getInt("port", 3306)}/${section.getString("database", "foxcore")}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
                username = section.getString("username", "root")
                password = section.getString("password", "")
                maximumPoolSize = section.getInt("pool-size", 5).coerceAtLeast(1)
                driverClassName = "com.mysql.cj.jdbc.Driver"
                poolName = "FoxCore-MySQL"
            }

            return HikariDataSource(hikari)
        }

        private fun buildTableName(config: FileConfiguration): String {
            val prefix = config.getString("storage.mysql.table-prefix", "foxcore_").orEmpty()
            return "${prefix}player_back"
        }

        private fun buildHomeTableName(config: FileConfiguration): String {
            val prefix = config.getString("storage.mysql.table-prefix", "foxcore_").orEmpty()
            return "${prefix}player_home"
        }

        private fun buildWarpTableName(config: FileConfiguration): String {
            val prefix = config.getString("storage.mysql.table-prefix", "foxcore_").orEmpty()
            return "${prefix}warp"
        }
    }
}
