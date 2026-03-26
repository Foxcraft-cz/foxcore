package me.dragan.foxcore.back.storage

import com.zaxxer.hikari.HikariDataSource
import me.dragan.foxcore.back.BackData
import me.dragan.foxcore.back.StoredLocation
import me.dragan.foxcore.home.HomeData
import me.dragan.foxcore.reward.RewardClaimRecord
import me.dragan.foxcore.reward.RewardDailyStateRecord
import me.dragan.foxcore.report.NewReportRecord
import me.dragan.foxcore.report.ReportActivitySnapshot
import me.dragan.foxcore.report.ReportActivityType
import me.dragan.foxcore.report.ReportDetail
import me.dragan.foxcore.report.ReportListEntry
import me.dragan.foxcore.report.ReportLocationSnapshot
import me.dragan.foxcore.report.ReportStatus
import me.dragan.foxcore.report.ReportTargetSummary
import me.dragan.foxcore.report.ReportType
import me.dragan.foxcore.warp.WarpData
import me.dragan.foxcore.warp.WarpScope
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Types
import java.util.UUID

abstract class JdbcBackStorage(
    private val dataSource: HikariDataSource,
    protected val tableName: String,
    protected val homeTableName: String,
    protected val warpTableName: String,
    protected val reportTableName: String,
    protected val reportActivityTableName: String,
    protected val rewardClaimTableName: String,
    protected val rewardDailyStateTableName: String,
) : FoxCoreStorage {

    override fun initialize() {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(createTableSql())
                statement.executeUpdate(createHomeTableSql())
                statement.executeUpdate(createWarpTableSql())
                statement.executeUpdate(createReportTableSql())
                statement.executeUpdate(createReportActivityTableSql())
                statement.executeUpdate(createRewardClaimTableSql())
                statement.executeUpdate(createRewardDailyStateTableSql())
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

    override fun createReport(record: NewReportRecord): Long {
        dataSource.connection.use { connection ->
            val previousAutoCommit = connection.autoCommit
            connection.autoCommit = false
            try {
                val reportId = connection.prepareStatement(insertReportSql(), Statement.RETURN_GENERATED_KEYS).use { statement ->
                    fillReportStatement(statement, record)
                    statement.executeUpdate()
                    statement.generatedKeys.use { keys ->
                        if (!keys.next()) {
                            error("Failed to create report row: no generated key returned")
                        }
                        keys.getLong(1)
                    }
                }
                connection.prepareStatement(insertReportActivitySql()).use { statement ->
                    record.activities.forEachIndexed { index, activity ->
                        statement.setLong(1, reportId)
                        statement.setInt(2, index)
                        statement.setString(3, activity.type.name)
                        statement.setString(4, activity.content)
                        statement.setLong(5, activity.createdAtMillis)
                        statement.addBatch()
                    }
                    statement.executeBatch()
                }
                connection.commit()
                return reportId
            } catch (exception: Exception) {
                connection.rollback()
                throw exception
            } finally {
                connection.autoCommit = previousAutoCommit
            }
        }
    }

    override fun hasRecentOpenDuplicate(
        reporterId: String,
        reportedId: String,
        normalizedReason: String,
        createdAfterMillis: Long,
    ): Boolean {
        dataSource.connection.use { connection ->
            connection.prepareStatement(recentDuplicateSql()).use { statement ->
                statement.setString(1, ReportStatus.OPEN.name)
                statement.setString(2, reporterId)
                statement.setString(3, reportedId)
                statement.setString(4, normalizedReason)
                statement.setLong(5, createdAfterMillis)
                statement.executeQuery().use { result ->
                    return result.next()
                }
            }
        }
    }

    override fun listReportTargetSummaries(type: ReportType, resolved: Boolean): List<ReportTargetSummary> {
        dataSource.connection.use { connection ->
            connection.prepareStatement(reportTargetSummarySql(resolved)).use { statement ->
                statement.setString(1, type.name)
                statement.setString(2, ReportStatus.OPEN.name)
                statement.executeQuery().use { result ->
                    val summaries = linkedMapOf<String, ReportTargetSummary>()
                    while (result.next()) {
                        val targetId = result.getString("reported_uuid")
                        if (!summaries.containsKey(targetId)) {
                            summaries[targetId] = ReportTargetSummary(
                                type = type,
                                targetId = targetId,
                                targetName = result.getString("reported_name"),
                                count = 1,
                                latestCreatedAtMillis = result.getLong("created_at"),
                                latestReason = result.getString("reason"),
                            )
                        } else {
                            val existing = summaries.getValue(targetId)
                            summaries[targetId] = existing.copy(count = existing.count + 1)
                        }
                    }
                    return summaries.values.toList()
                }
            }
        }
    }

    override fun listReportsForTarget(type: ReportType, resolved: Boolean, targetId: String): List<ReportListEntry> {
        dataSource.connection.use { connection ->
            connection.prepareStatement(reportListSql(resolved)).use { statement ->
                statement.setString(1, type.name)
                statement.setString(2, targetId)
                statement.setString(3, ReportStatus.OPEN.name)
                statement.executeQuery().use { result ->
                    val reports = mutableListOf<ReportListEntry>()
                    while (result.next()) {
                        reports += ReportListEntry(
                            id = result.getLong("id"),
                            type = ReportType.valueOf(result.getString("type")),
                            status = ReportStatus.valueOf(result.getString("status")),
                            reason = result.getString("reason"),
                            createdAtMillis = result.getLong("created_at"),
                            reportedId = result.getString("reported_uuid"),
                            reportedName = result.getString("reported_name"),
                            reporterId = result.getString("reporter_uuid"),
                            reporterName = result.getString("reporter_name"),
                            resolverName = result.getString("resolver_name"),
                            resolvedAtMillis = result.getLongOrNull("resolved_at"),
                        )
                    }
                    return reports
                }
            }
        }
    }

    override fun findReportById(id: Long): ReportDetail? {
        dataSource.connection.use { connection ->
            connection.prepareStatement(reportDetailSql()).use { statement ->
                statement.setLong(1, id)
                statement.executeQuery().use { result ->
                    if (!result.next()) {
                        return null
                    }

                    return ReportDetail(
                        id = result.getLong("id"),
                        type = ReportType.valueOf(result.getString("type")),
                        status = ReportStatus.valueOf(result.getString("status")),
                        reason = result.getString("reason"),
                        createdAtMillis = result.getLong("created_at"),
                        reportedId = result.getString("reported_uuid"),
                        reportedName = result.getString("reported_name"),
                        reportedLocation = readReportLocation(result, "reported"),
                        reportedOnline = result.getBoolean("reported_online"),
                        reportedGameMode = result.getString("reported_gamemode"),
                        reporterId = result.getString("reporter_uuid"),
                        reporterName = result.getString("reporter_name"),
                        reporterLocation = readReportLocation(result, "reporter"),
                        reporterOnline = result.getBoolean("reporter_online"),
                        reporterGameMode = result.getString("reporter_gamemode"),
                        resolverId = result.getString("resolver_uuid"),
                        resolverName = result.getString("resolver_name"),
                        resolvedAtMillis = result.getLongOrNull("resolved_at"),
                        activities = loadReportActivities(connection, id),
                    )
                }
            }
        }
    }

    override fun resolveReport(
        id: Long,
        status: ReportStatus,
        resolverId: String,
        resolverName: String,
        resolvedAtMillis: Long,
    ): Boolean {
        dataSource.connection.use { connection ->
            connection.prepareStatement(resolveReportSql()).use { statement ->
                statement.setString(1, status.name)
                statement.setString(2, resolverId)
                statement.setString(3, resolverName)
                statement.setLong(4, resolvedAtMillis)
                statement.setLong(5, id)
                statement.setString(6, ReportStatus.OPEN.name)
                return statement.executeUpdate() > 0
            }
        }
    }

    override fun loadRewardClaims(playerId: UUID): List<RewardClaimRecord> {
        dataSource.connection.use { connection ->
            connection.prepareStatement(selectRewardClaimsSql()).use { statement ->
                statement.setString(1, playerId.toString())
                statement.executeQuery().use { result ->
                    val claims = mutableListOf<RewardClaimRecord>()
                    while (result.next()) {
                        claims += RewardClaimRecord(
                            trackId = result.getString("track_id"),
                            rewardId = result.getString("reward_id"),
                            cycleKey = result.getString("cycle_key"),
                            claimedAtMillis = result.getLong("claimed_at"),
                        )
                    }
                    return claims
                }
            }
        }
    }

    override fun loadRewardDailyStates(playerId: UUID): List<RewardDailyStateRecord> {
        dataSource.connection.use { connection ->
            connection.prepareStatement(selectRewardDailyStatesSql()).use { statement ->
                statement.setString(1, playerId.toString())
                statement.executeQuery().use { result ->
                    val states = mutableListOf<RewardDailyStateRecord>()
                    while (result.next()) {
                        states += RewardDailyStateRecord(
                            trackId = result.getString("track_id"),
                            streak = result.getInt("streak"),
                            lastJoinDate = result.getString("last_join_date"),
                            cycle = result.getInt("cycle"),
                        )
                    }
                    return states
                }
            }
        }
    }

    override fun saveRewardDailyState(playerId: UUID, state: RewardDailyStateRecord) {
        dataSource.connection.use { connection ->
            val previousAutoCommit = connection.autoCommit
            connection.autoCommit = false
            try {
                connection.prepareStatement(deleteRewardDailyStateSql()).use { statement ->
                    statement.setString(1, playerId.toString())
                    statement.setString(2, state.trackId)
                    statement.executeUpdate()
                }
                connection.prepareStatement(insertRewardDailyStateSql()).use { statement ->
                    statement.setString(1, playerId.toString())
                    statement.setString(2, state.trackId)
                    statement.setInt(3, state.streak)
                    statement.setString(4, state.lastJoinDate)
                    statement.setInt(5, state.cycle)
                    statement.executeUpdate()
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

    override fun claimReward(playerId: UUID, claim: RewardClaimRecord): Boolean {
        dataSource.connection.use { connection ->
            connection.prepareStatement(insertRewardClaimSql()).use { statement ->
                statement.setString(1, playerId.toString())
                statement.setString(2, claim.trackId)
                statement.setString(3, claim.rewardId)
                statement.setString(4, claim.cycleKey)
                statement.setLong(5, claim.claimedAtMillis)
                return statement.executeUpdate() > 0
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

    protected abstract fun createReportTableSql(): String

    protected abstract fun createReportActivityTableSql(): String

    protected abstract fun createRewardClaimTableSql(): String

    protected abstract fun createRewardDailyStateTableSql(): String

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

    protected open fun insertReportSql(): String =
        """
        INSERT INTO $reportTableName (
            type, status, reason, reason_normalized, created_at,
            reported_uuid, reported_name, reported_world, reported_x, reported_y, reported_z, reported_yaw, reported_pitch, reported_online, reported_gamemode,
            reporter_uuid, reporter_name, reporter_world, reporter_x, reporter_y, reporter_z, reporter_yaw, reporter_pitch, reporter_online, reporter_gamemode,
            resolver_uuid, resolver_name, resolved_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

    protected open fun insertReportActivitySql(): String =
        """
        INSERT INTO $reportActivityTableName (
            report_id, entry_index, entry_type, content, created_at
        ) VALUES (?, ?, ?, ?, ?)
        """.trimIndent()

    protected open fun insertRewardClaimSql(): String =
        """
        INSERT INTO $rewardClaimTableName (
            player_uuid, track_id, reward_id, cycle_key, claimed_at
        ) VALUES (?, ?, ?, ?, ?)
        """.trimIndent()

    protected open fun selectRewardClaimsSql(): String =
        """
        SELECT track_id, reward_id, cycle_key, claimed_at
        FROM $rewardClaimTableName
        WHERE player_uuid = ?
        ORDER BY claimed_at ASC
        """.trimIndent()

    protected open fun selectRewardDailyStatesSql(): String =
        """
        SELECT track_id, streak, last_join_date, cycle
        FROM $rewardDailyStateTableName
        WHERE player_uuid = ?
        ORDER BY track_id ASC
        """.trimIndent()

    protected open fun deleteRewardDailyStateSql(): String =
        """
        DELETE FROM $rewardDailyStateTableName
        WHERE player_uuid = ? AND track_id = ?
        """.trimIndent()

    protected open fun insertRewardDailyStateSql(): String =
        """
        INSERT INTO $rewardDailyStateTableName (
            player_uuid, track_id, streak, last_join_date, cycle
        ) VALUES (?, ?, ?, ?, ?)
        """.trimIndent()

    protected open fun recentDuplicateSql(): String =
        """
        SELECT 1
        FROM $reportTableName
        WHERE status = ?
          AND reporter_uuid = ?
          AND reported_uuid = ?
          AND reason_normalized = ?
          AND created_at >= ?
        LIMIT 1
        """.trimIndent()

    protected open fun reportTargetSummarySql(resolved: Boolean): String =
        """
        SELECT reported_uuid, reported_name, reason, created_at
        FROM $reportTableName
        WHERE type = ? AND status ${if (resolved) "<>" else "="} ?
        ORDER BY created_at DESC
        """.trimIndent()

    protected open fun reportListSql(resolved: Boolean): String =
        """
        SELECT id, type, status, reason, created_at,
               reported_uuid, reported_name,
               reporter_uuid, reporter_name,
               resolver_name, resolved_at
        FROM $reportTableName
        WHERE type = ?
          AND reported_uuid = ?
          AND status ${if (resolved) "<>" else "="} ?
        ORDER BY created_at DESC
        """.trimIndent()

    protected open fun reportDetailSql(): String =
        """
        SELECT id, type, status, reason, created_at,
               reported_uuid, reported_name, reported_world, reported_x, reported_y, reported_z, reported_yaw, reported_pitch, reported_online, reported_gamemode,
               reporter_uuid, reporter_name, reporter_world, reporter_x, reporter_y, reporter_z, reporter_yaw, reporter_pitch, reporter_online, reporter_gamemode,
               resolver_uuid, resolver_name, resolved_at
        FROM $reportTableName
        WHERE id = ?
        """.trimIndent()

    protected open fun reportActivitySql(): String =
        """
        SELECT entry_type, content, created_at
        FROM $reportActivityTableName
        WHERE report_id = ?
        ORDER BY entry_index ASC
        """.trimIndent()

    protected open fun resolveReportSql(): String =
        """
        UPDATE $reportTableName
        SET status = ?, resolver_uuid = ?, resolver_name = ?, resolved_at = ?
        WHERE id = ? AND status = ?
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

    protected fun fillReportStatement(statement: PreparedStatement, record: NewReportRecord) {
        statement.setString(1, record.type.name)
        statement.setString(2, record.status.name)
        statement.setString(3, record.reason)
        statement.setString(4, record.normalizedReason)
        statement.setLong(5, record.createdAtMillis)
        statement.setString(6, record.reportedId)
        statement.setString(7, record.reportedName)
        bindReportLocation(statement, 8, record.reportedLocation)
        statement.setBoolean(14, record.reportedOnline)
        statement.setString(15, record.reportedGameMode)
        statement.setString(16, record.reporterId)
        statement.setString(17, record.reporterName)
        bindReportLocation(statement, 18, record.reporterLocation)
        statement.setBoolean(24, record.reporterOnline)
        statement.setString(25, record.reporterGameMode)
        statement.setNull(26, Types.VARCHAR)
        statement.setNull(27, Types.VARCHAR)
        statement.setNull(28, Types.BIGINT)
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

    protected fun bindReportLocation(statement: PreparedStatement, startIndex: Int, location: ReportLocationSnapshot) {
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

    protected fun ResultSet.getLongOrNull(column: String): Long? {
        val value = getLong(column)
        return if (wasNull()) null else value
    }

    protected fun readReportLocation(result: ResultSet, prefix: String): ReportLocationSnapshot =
        ReportLocationSnapshot(
            worldName = result.getString("${prefix}_world"),
            x = result.getDouble("${prefix}_x"),
            y = result.getDouble("${prefix}_y"),
            z = result.getDouble("${prefix}_z"),
            yaw = result.getFloat("${prefix}_yaw"),
            pitch = result.getFloat("${prefix}_pitch"),
        )

    private fun readLocation(
        world: String?,
        x: Double,
        y: Double,
        z: Double,
        yaw: Float,
        pitch: Float,
    ): StoredLocation? =
        if (world == null) null else StoredLocation(world, x, y, z, yaw, pitch)

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

    private fun loadReportActivities(connection: java.sql.Connection, reportId: Long): List<ReportActivitySnapshot> {
        connection.prepareStatement(reportActivitySql()).use { statement ->
            statement.setLong(1, reportId)
            statement.executeQuery().use { result ->
                val activities = mutableListOf<ReportActivitySnapshot>()
                while (result.next()) {
                    activities += ReportActivitySnapshot(
                        type = ReportActivityType.valueOf(result.getString("entry_type")),
                        content = result.getString("content"),
                        createdAtMillis = result.getLong("created_at"),
                    )
                }
                return activities
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
