package me.dragan.foxcore.back.storage

import me.dragan.foxcore.reward.RewardClaimRecord
import me.dragan.foxcore.reward.RewardDailyStateRecord
import me.dragan.foxcore.report.NewReportRecord
import me.dragan.foxcore.report.ReportDetail
import me.dragan.foxcore.report.ReportListEntry
import me.dragan.foxcore.report.ReportStatus
import me.dragan.foxcore.report.ReportTargetSummary
import me.dragan.foxcore.report.ReportType
import java.util.UUID

interface FoxCoreStorage : BackStorage {
    fun createReport(record: NewReportRecord): Long
    fun hasRecentOpenDuplicate(
        reporterId: String,
        reportedId: String,
        normalizedReason: String,
        createdAfterMillis: Long,
    ): Boolean

    fun listReportTargetSummaries(type: ReportType, resolved: Boolean): List<ReportTargetSummary>

    fun listReportsForTarget(
        type: ReportType,
        resolved: Boolean,
        targetId: String,
    ): List<ReportListEntry>

    fun findReportById(id: Long): ReportDetail?

    fun resolveReport(
        id: Long,
        status: ReportStatus,
        resolverId: String,
        resolverName: String,
        resolvedAtMillis: Long,
    ): Boolean

    fun loadRewardClaims(playerId: UUID): List<RewardClaimRecord>

    fun loadRewardDailyStates(playerId: UUID): List<RewardDailyStateRecord>

    fun saveRewardDailyState(playerId: UUID, state: RewardDailyStateRecord)

    fun claimReward(playerId: UUID, claim: RewardClaimRecord): Boolean
}
