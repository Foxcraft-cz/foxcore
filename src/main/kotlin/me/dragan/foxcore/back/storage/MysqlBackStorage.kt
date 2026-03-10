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

    override fun bindUpsertTail(statement: PreparedStatement, playerId: UUID, data: BackData) {
        statement.setString(17, data.playerName)
        bindLocation(statement, 18, data.lastLocation)
        bindLong(statement, 24, data.lastLocationAtMillis)
        bindLocation(statement, 25, data.lastDeathLocation)
        bindLong(statement, 31, data.lastDeathAtMillis)
    }

    override fun migrateSchema(statement: java.sql.Statement) {
        statement.runCatching {
            executeUpdate("ALTER TABLE $tableName ADD COLUMN player_name VARCHAR(16) NULL")
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
    }
}
