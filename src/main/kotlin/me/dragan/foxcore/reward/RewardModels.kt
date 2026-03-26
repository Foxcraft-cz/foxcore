package me.dragan.foxcore.reward

import org.bukkit.Material

data class RewardTrack(
    val id: String,
    val order: Int,
    val title: String,
    val iconMaterialKey: String?,
    val oraxenItemId: String?,
    val summary: List<String>,
    val viewPermission: String,
    val claimPermission: String,
    val progress: RewardProgressDefinition,
    val rewards: List<RewardEntry>,
) {
    fun iconMaterial(): Material =
        iconMaterialKey
            ?.let(Material::matchMaterial)
            ?.takeIf(Material::isItem)
            ?: Material.CHEST
}

data class RewardEntry(
    val id: String,
    val requiredProgress: Int,
    val iconMaterialKey: String?,
    val oraxenItemId: String?,
    val title: String?,
    val description: List<String>,
    val commands: List<String>,
) {
    fun iconMaterial(): Material =
        iconMaterialKey
            ?.let(Material::matchMaterial)
            ?.takeIf(Material::isItem)
            ?: Material.CHEST
}

sealed interface RewardProgressDefinition {
    val type: String
}

data class PlaceholderRewardProgressDefinition(
    val valuePlaceholder: String,
    val cyclePlaceholder: String?,
    val cycleKey: String,
) : RewardProgressDefinition {
    override val type: String = "placeholder"
}

data class DailyStreakRewardProgressDefinition(
    val timezone: String,
) : RewardProgressDefinition {
    override val type: String = "daily_streak"
}

data class RewardClaimRecord(
    val trackId: String,
    val rewardId: String,
    val cycleKey: String,
    val claimedAtMillis: Long,
)

data class RewardDailyStateRecord(
    val trackId: String,
    val streak: Int,
    val lastJoinDate: String,
    val cycle: Int,
)

data class RewardPlayerStorageSnapshot(
    val claims: List<RewardClaimRecord>,
    val dailyStates: Map<String, RewardDailyStateRecord>,
)

enum class RewardClaimStatus {
    CLAIMED,
    CLAIMABLE,
    LOCKED,
}

data class RewardEntryView(
    val reward: RewardEntry,
    val status: RewardClaimStatus,
)

data class RewardTrackView(
    val track: RewardTrack,
    val progressValue: Int,
    val cycleKey: String,
    val canClaim: Boolean,
    val claimableCount: Int,
    val rewardViews: List<RewardEntryView>,
) {
    val nextRequiredProgress: Int? =
        rewardViews.firstOrNull { it.status != RewardClaimStatus.CLAIMED }?.reward?.requiredProgress

    val focusRewardIndex: Int =
        run {
            val nextLockedIndex = rewardViews.indexOfFirst { it.reward.requiredProgress > progressValue }
            if (nextLockedIndex >= 0) {
                val currentProgressRequired = rewardViews
                    .getOrNull(nextLockedIndex - 1)
                    ?.reward
                    ?.requiredProgress
                rewardViews.indexOfLast { rewardView ->
                    rewardView.status == RewardClaimStatus.CLAIMABLE &&
                        rewardView.reward.requiredProgress == currentProgressRequired
                }.takeIf { it >= 0 } ?: nextLockedIndex
            } else {
                val lastRequired = rewardViews.lastOrNull()?.reward?.requiredProgress
                rewardViews.indexOfLast { rewardView ->
                    rewardView.status == RewardClaimStatus.CLAIMABLE &&
                        rewardView.reward.requiredProgress == lastRequired
                }.takeIf { it >= 0 } ?: rewardViews.lastIndex.coerceAtLeast(0)
            }
        }

    fun defaultPage(pageSize: Int): Int =
        if (pageSize <= 0) 0 else focusRewardIndex / pageSize
}
