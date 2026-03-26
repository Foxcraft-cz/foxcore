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
    reportTableName = "report",
    reportActivityTableName = "report_activity",
    rewardClaimTableName = "reward_claim",
    rewardDailyStateTableName = "reward_daily_state",
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

    override fun createReportTableSql(): String =
        """
        CREATE TABLE IF NOT EXISTS $reportTableName (
            id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
            type TEXT NOT NULL,
            status TEXT NOT NULL,
            reason TEXT NOT NULL,
            reason_normalized TEXT NOT NULL,
            created_at INTEGER NOT NULL,
            reported_uuid TEXT NOT NULL,
            reported_name TEXT NOT NULL,
            reported_world TEXT NOT NULL,
            reported_x REAL NOT NULL,
            reported_y REAL NOT NULL,
            reported_z REAL NOT NULL,
            reported_yaw REAL NOT NULL,
            reported_pitch REAL NOT NULL,
            reported_online INTEGER NOT NULL,
            reported_gamemode TEXT NOT NULL,
            reporter_uuid TEXT NOT NULL,
            reporter_name TEXT NOT NULL,
            reporter_world TEXT NOT NULL,
            reporter_x REAL NOT NULL,
            reporter_y REAL NOT NULL,
            reporter_z REAL NOT NULL,
            reporter_yaw REAL NOT NULL,
            reporter_pitch REAL NOT NULL,
            reporter_online INTEGER NOT NULL,
            reporter_gamemode TEXT NOT NULL,
            resolver_uuid TEXT NULL,
            resolver_name TEXT NULL,
            resolved_at INTEGER NULL
        )
        """.trimIndent()

    override fun createReportActivityTableSql(): String =
        """
        CREATE TABLE IF NOT EXISTS $reportActivityTableName (
            report_id INTEGER NOT NULL,
            entry_index INTEGER NOT NULL,
            entry_type TEXT NOT NULL,
            content TEXT NOT NULL,
            created_at INTEGER NOT NULL,
            PRIMARY KEY (report_id, entry_index)
        )
        """.trimIndent()

    override fun createRewardClaimTableSql(): String =
        """
        CREATE TABLE IF NOT EXISTS $rewardClaimTableName (
            player_uuid TEXT NOT NULL,
            track_id TEXT NOT NULL,
            reward_id TEXT NOT NULL,
            cycle_key TEXT NOT NULL,
            claimed_at INTEGER NOT NULL,
            PRIMARY KEY (player_uuid, track_id, reward_id, cycle_key)
        )
        """.trimIndent()

    override fun createRewardDailyStateTableSql(): String =
        """
        CREATE TABLE IF NOT EXISTS $rewardDailyStateTableName (
            player_uuid TEXT NOT NULL,
            track_id TEXT NOT NULL,
            streak INTEGER NOT NULL,
            last_join_date TEXT NOT NULL,
            cycle INTEGER NOT NULL,
            PRIMARY KEY (player_uuid, track_id)
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

    override fun insertRewardClaimSql(): String =
        """
        INSERT OR IGNORE INTO $rewardClaimTableName (
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
        statement.runCatching {
            executeUpdate("CREATE INDEX idx_${reportTableName}_type_status_created ON $reportTableName (type, status, created_at)")
        }
        statement.runCatching {
            executeUpdate("CREATE INDEX idx_${reportTableName}_reported_uuid ON $reportTableName (reported_uuid)")
        }
        statement.runCatching {
            executeUpdate("CREATE INDEX idx_${reportTableName}_duplicate_lookup ON $reportTableName (status, reporter_uuid, reported_uuid, reason_normalized, created_at)")
        }
        statement.runCatching {
            executeUpdate("CREATE INDEX idx_${rewardClaimTableName}_player_track ON $rewardClaimTableName (player_uuid, track_id)")
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
