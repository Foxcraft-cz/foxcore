package me.dragan.foxcore.report

enum class ReportType {
    PLAYER,
    STAFF,
}

enum class ReportStatus {
    OPEN,
    CONFIRMED,
    REJECTED,
}

enum class ReportActivityType {
    CHAT,
    COMMAND,
}

data class ReportLocationSnapshot(
    val worldName: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float,
)

data class ReportActivitySnapshot(
    val type: ReportActivityType,
    val content: String,
    val createdAtMillis: Long,
)

data class NewReportRecord(
    val type: ReportType,
    val status: ReportStatus,
    val reason: String,
    val normalizedReason: String,
    val createdAtMillis: Long,
    val reportedId: String,
    val reportedName: String,
    val reportedLocation: ReportLocationSnapshot,
    val reportedOnline: Boolean,
    val reportedGameMode: String,
    val reporterId: String,
    val reporterName: String,
    val reporterLocation: ReportLocationSnapshot,
    val reporterOnline: Boolean,
    val reporterGameMode: String,
    val activities: List<ReportActivitySnapshot>,
)

data class ReportTargetSummary(
    val type: ReportType,
    val targetId: String,
    val targetName: String,
    val count: Int,
    val latestCreatedAtMillis: Long,
    val latestReason: String,
)

data class ReportListEntry(
    val id: Long,
    val type: ReportType,
    val status: ReportStatus,
    val reason: String,
    val createdAtMillis: Long,
    val reportedId: String,
    val reportedName: String,
    val reporterId: String,
    val reporterName: String,
    val resolverName: String?,
    val resolvedAtMillis: Long?,
)

data class ReportDetail(
    val id: Long,
    val type: ReportType,
    val status: ReportStatus,
    val reason: String,
    val createdAtMillis: Long,
    val reportedId: String,
    val reportedName: String,
    val reportedLocation: ReportLocationSnapshot,
    val reportedOnline: Boolean,
    val reportedGameMode: String,
    val reporterId: String,
    val reporterName: String,
    val reporterLocation: ReportLocationSnapshot,
    val reporterOnline: Boolean,
    val reporterGameMode: String,
    val resolverId: String?,
    val resolverName: String?,
    val resolvedAtMillis: Long?,
    val activities: List<ReportActivitySnapshot>,
)

sealed interface ReportCreateResult {
    data object Disabled : ReportCreateResult
    data object SelfReport : ReportCreateResult
    data class ReasonTooShort(val minLength: Int) : ReportCreateResult
    data class ReasonTooLong(val maxLength: Int) : ReportCreateResult
    data class Cooldown(val secondsRemaining: Long) : ReportCreateResult
    data object Duplicate : ReportCreateResult
    data class Success(val reportId: Long, val type: ReportType) : ReportCreateResult
}

sealed interface ReportResolveResult {
    data object NotFound : ReportResolveResult
    data object AlreadyResolved : ReportResolveResult
    data object Success : ReportResolveResult
}
