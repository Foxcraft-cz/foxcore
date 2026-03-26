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
    reportTableName = buildReportTableName(config),
    reportActivityTableName = buildReportActivityTableName(config),
    rewardClaimTableName = buildRewardClaimTableName(config),
    rewardDailyStateTableName = buildRewardDailyStateTableName(config),
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

    override fun createReportTableSql(): String =
        """
        CREATE TABLE IF NOT EXISTS $reportTableName (
            id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
            type VARCHAR(16) NOT NULL,
            status VARCHAR(16) NOT NULL,
            reason TEXT NOT NULL,
            reason_normalized TEXT NOT NULL,
            created_at BIGINT NOT NULL,
            reported_uuid VARCHAR(36) NOT NULL,
            reported_name VARCHAR(16) NOT NULL,
            reported_world VARCHAR(255) NOT NULL,
            reported_x DOUBLE NOT NULL,
            reported_y DOUBLE NOT NULL,
            reported_z DOUBLE NOT NULL,
            reported_yaw FLOAT NOT NULL,
            reported_pitch FLOAT NOT NULL,
            reported_online TINYINT(1) NOT NULL,
            reported_gamemode VARCHAR(32) NOT NULL,
            reporter_uuid VARCHAR(36) NOT NULL,
            reporter_name VARCHAR(16) NOT NULL,
            reporter_world VARCHAR(255) NOT NULL,
            reporter_x DOUBLE NOT NULL,
            reporter_y DOUBLE NOT NULL,
            reporter_z DOUBLE NOT NULL,
            reporter_yaw FLOAT NOT NULL,
            reporter_pitch FLOAT NOT NULL,
            reporter_online TINYINT(1) NOT NULL,
            reporter_gamemode VARCHAR(32) NOT NULL,
            resolver_uuid VARCHAR(36) NULL,
            resolver_name VARCHAR(16) NULL,
            resolved_at BIGINT NULL,
            INDEX idx_${reportTableName}_type_status_created (type, status, created_at),
            INDEX idx_${reportTableName}_reported_uuid (reported_uuid),
            INDEX idx_${reportTableName}_duplicate_lookup (status, reporter_uuid, reported_uuid, created_at)
        )
        """.trimIndent()

    override fun createReportActivityTableSql(): String =
        """
        CREATE TABLE IF NOT EXISTS $reportActivityTableName (
            report_id BIGINT NOT NULL,
            entry_index INT NOT NULL,
            entry_type VARCHAR(16) NOT NULL,
            content TEXT NOT NULL,
            created_at BIGINT NOT NULL,
            PRIMARY KEY (report_id, entry_index),
            INDEX idx_${reportActivityTableName}_report_id (report_id)
        )
        """.trimIndent()

    override fun createRewardClaimTableSql(): String =
        """
        CREATE TABLE IF NOT EXISTS $rewardClaimTableName (
            player_uuid VARCHAR(36) NOT NULL,
            track_id VARCHAR(64) NOT NULL,
            reward_id VARCHAR(64) NOT NULL,
            cycle_key VARCHAR(255) NOT NULL,
            claimed_at BIGINT NOT NULL,
            PRIMARY KEY (player_uuid, track_id, reward_id, cycle_key),
            INDEX idx_${rewardClaimTableName}_player_track (player_uuid, track_id)
        )
        """.trimIndent()

    override fun createRewardDailyStateTableSql(): String =
        """
        CREATE TABLE IF NOT EXISTS $rewardDailyStateTableName (
            player_uuid VARCHAR(36) NOT NULL,
            track_id VARCHAR(64) NOT NULL,
            streak INT NOT NULL,
            last_join_date VARCHAR(32) NOT NULL,
            cycle INT NOT NULL,
            PRIMARY KEY (player_uuid, track_id)
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

    override fun insertRewardClaimSql(): String =
        """
        INSERT IGNORE INTO $rewardClaimTableName (
            player_uuid, track_id, reward_id, cycle_key, claimed_at
        ) VALUES (?, ?, ?, ?, ?)
        """.trimIndent()

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
        statement.runCatching {
            executeUpdate("ALTER TABLE $reportTableName ADD COLUMN resolver_uuid VARCHAR(36) NULL")
        }
        statement.runCatching {
            executeUpdate("ALTER TABLE $reportTableName ADD COLUMN resolver_name VARCHAR(16) NULL")
        }
        statement.runCatching {
            executeUpdate("ALTER TABLE $reportTableName ADD COLUMN resolved_at BIGINT NULL")
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

        private fun buildRewardClaimTableName(config: FileConfiguration): String {
            val prefix = config.getString("storage.mysql.table-prefix", "foxcore_").orEmpty()
            return "${prefix}reward_claim"
        }

        private fun buildRewardDailyStateTableName(config: FileConfiguration): String {
            val prefix = config.getString("storage.mysql.table-prefix", "foxcore_").orEmpty()
            return "${prefix}reward_daily_state"
        }

        private fun buildReportTableName(config: FileConfiguration): String {
            val prefix = config.getString("storage.mysql.table-prefix", "foxcore_").orEmpty()
            return "${prefix}report"
        }

        private fun buildReportActivityTableName(config: FileConfiguration): String {
            val prefix = config.getString("storage.mysql.table-prefix", "foxcore_").orEmpty()
            return "${prefix}report_activity"
        }
    }
}
