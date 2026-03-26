package me.dragan.foxcore.vote

import me.dragan.foxcore.FoxCorePlugin
import me.dragan.foxcore.feedback.PlayerFeedback
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.util.Locale
import java.util.UUID

class VoteService(
    private val plugin: FoxCorePlugin,
) {
    private val plainText = PlainTextComponentSerializer.plainText()
    private var activeVote: ActiveVote? = null
    private val cooldowns = mutableMapOf<VoteAction, Long>()
    private var tickerTask: BukkitTask? = null

    fun reload() {
        cancelActiveVote(CancelReason.RELOAD)
    }

    fun shutdown() {
        cancelActiveVote(null)
        cooldowns.clear()
    }

    fun startVote(player: Player, action: VoteAction): VoteStartResult {
        if (!plugin.config.getBoolean("votes.enabled", true)) {
            return VoteStartResult.Disabled
        }

        activeVote?.let { vote ->
            return VoteStartResult.Active(vote.action)
        }

        val now = System.currentTimeMillis()
        val cooldownEndsAt = cooldowns[action]
        if (cooldownEndsAt != null && cooldownEndsAt > now) {
            return VoteStartResult.Cooldown(secondsRemaining(cooldownEndsAt - now))
        }

        val durationSeconds = voteDurationSeconds()
        val vote = ActiveVote(
            action = action,
            world = player.world,
            durationSeconds = durationSeconds,
            endsAtMillis = now + (durationSeconds * 1000L),
            bossBar = BossBar.bossBar(
                Component.empty(),
                1.0f,
                action.bossBarColor,
                BossBar.Overlay.PROGRESS,
            ),
        )

        activeVote = vote
        vote.voters[player.uniqueId] = VoteChoice.YES
        vote.yesVotes = 1

        val cooldownSeconds = cooldownSeconds(action)
        if (cooldownSeconds > 0L) {
            cooldowns[action] = now + (cooldownSeconds * 1000L)
        }

        updateBossBar(vote)
        plugin.server.onlinePlayers.forEach { it.showBossBar(vote.bossBar) }
        broadcast(
            plugin.messages.text(
                "command.vote.started",
                "player" to player.name,
                "action" to actionLabel(action),
                "world" to vote.world.name,
                "seconds" to durationSeconds.toString(),
                "command" to action.commandLabel,
            ),
        )
        broadcast(buildVoteButtons(vote))
        plugin.server.onlinePlayers.forEach(PlayerFeedback::voteStarted)
        ensureTicker()
        return VoteStartResult.Started(durationSeconds)
    }

    fun forceVote(player: Player, action: VoteAction) {
        cancelActiveVote(CancelReason.FORCED(player.name))

        action.apply(player.world)

        val cooldownSeconds = cooldownSeconds(action)
        if (cooldownSeconds > 0L) {
            cooldowns[action] = System.currentTimeMillis() + (cooldownSeconds * 1000L)
        }

        broadcast(
            plugin.messages.text(
                "command.vote.force-success",
                "player" to player.name,
                "action" to actionLabel(action),
                "world" to player.world.name,
            ),
        )
        plugin.server.onlinePlayers.forEach(PlayerFeedback::votePassed)
    }

    fun castVote(player: Player, choice: VoteChoice): VoteCastResult {
        val vote = activeVote ?: return VoteCastResult.NoActiveVote

        val previous = vote.voters[player.uniqueId]
        if (previous == choice) {
            return VoteCastResult.AlreadyVoted(choice)
        }

        if (previous != null) {
            decrementChoice(vote, previous)
        }

        vote.voters[player.uniqueId] = choice
        incrementChoice(vote, choice)
        return if (previous == null) {
            VoteCastResult.Recorded(choice)
        } else {
            VoteCastResult.Changed(choice)
        }
    }

    private fun ensureTicker() {
        if (tickerTask != null) {
            return
        }

        tickerTask = plugin.server.scheduler.runTaskTimer(
            plugin,
            Runnable { tick() },
            20L,
            20L,
        )
    }

    private fun stopTicker() {
        tickerTask?.cancel()
        tickerTask = null
    }

    private fun tick() {
        val vote = activeVote ?: run {
            stopTicker()
            return
        }

        if (System.currentTimeMillis() >= vote.endsAtMillis) {
            finishVote(vote)
            return
        }

        maybeBroadcastMilestones(vote)
        plugin.server.onlinePlayers.forEach { it.showBossBar(vote.bossBar) }
        updateBossBar(vote)
    }

    private fun finishVote(vote: ActiveVote) {
        hideBossBar(vote)
        activeVote = null
        stopTicker()

        val totalVotes = vote.yesVotes + vote.noVotes
        val minimumParticipants = plugin.config.getInt("votes.minimum-participants", 1).coerceAtLeast(1)
        val requiredRatio = plugin.config.getDouble("votes.required-yes-ratio", 0.60).coerceIn(0.0, 1.0)
        val yesRatio = if (totalVotes == 0) 0.0 else vote.yesVotes.toDouble() / totalVotes.toDouble()

        if (totalVotes < minimumParticipants) {
            broadcast(
                plugin.messages.text(
                    "command.vote.failed-minimum",
                    "action" to actionLabel(vote.action),
                    "world" to vote.world.name,
                    "yes" to vote.yesVotes.toString(),
                    "no" to vote.noVotes.toString(),
                    "required" to minimumParticipants.toString(),
                ),
            )
            plugin.server.onlinePlayers.forEach(PlayerFeedback::voteFailed)
            return
        }

        if (yesRatio >= requiredRatio) {
            vote.action.apply(vote.world)
            broadcast(
                plugin.messages.text(
                    "command.vote.passed",
                    "action" to actionLabel(vote.action),
                    "world" to vote.world.name,
                    "yes" to vote.yesVotes.toString(),
                    "no" to vote.noVotes.toString(),
                    "percent" to formatPercent(yesRatio),
                ),
            )
            plugin.server.onlinePlayers.forEach(PlayerFeedback::votePassed)
            return
        }

        broadcast(
            plugin.messages.text(
                "command.vote.failed",
                "action" to actionLabel(vote.action),
                "world" to vote.world.name,
                "yes" to vote.yesVotes.toString(),
                "no" to vote.noVotes.toString(),
                "percent" to formatPercent(yesRatio),
                "required" to formatPercent(requiredRatio),
            ),
        )
        plugin.server.onlinePlayers.forEach(PlayerFeedback::voteFailed)
    }

    private fun cancelActiveVote(reason: CancelReason?) {
        val vote = activeVote ?: return

        hideBossBar(vote)
        activeVote = null
        stopTicker()

        when (reason) {
            null -> Unit
            CancelReason.RELOAD -> broadcast(
                plugin.messages.text(
                    "command.vote.cancelled-reload",
                    "action" to actionLabel(vote.action),
                    "world" to vote.world.name,
                ),
            )
            is CancelReason.FORCED -> broadcast(
                plugin.messages.text(
                    "command.vote.cancelled-force",
                    "action" to actionLabel(vote.action),
                    "world" to vote.world.name,
                    "player" to reason.playerName,
                ),
            )
        }
    }

    private fun updateBossBar(vote: ActiveVote) {
        val remainingSeconds = secondsRemaining(vote.endsAtMillis - System.currentTimeMillis())
        vote.bossBar.name(
            plugin.messages.text(
                "command.vote.bossbar",
                "action" to actionLabel(vote.action),
                "world" to vote.world.name,
                "seconds" to remainingSeconds.toString(),
                "yes" to vote.yesVotes.toString(),
                "no" to vote.noVotes.toString(),
            ),
        )
        vote.bossBar.progress((remainingSeconds.toFloat() / vote.durationSeconds.toFloat()).coerceIn(0.0f, 1.0f))
    }

    private fun maybeBroadcastMilestones(vote: ActiveVote) {
        val remainingSeconds = secondsRemaining(vote.endsAtMillis - System.currentTimeMillis())
        val halfwaySeconds = ((vote.durationSeconds + 1L) / 2L).coerceAtLeast(1L)
        if (!vote.halfwayAnnounced && remainingSeconds <= halfwaySeconds) {
            vote.halfwayAnnounced = true
            broadcast(
                plugin.messages.text(
                    "command.vote.halfway",
                    "action" to actionLabel(vote.action),
                    "world" to vote.world.name,
                    "seconds" to remainingSeconds.toString(),
                    "yes" to vote.yesVotes.toString(),
                    "no" to vote.noVotes.toString(),
                ),
            )
            broadcast(buildVoteButtons(vote))
        }

        val endingSoonSeconds = endingSoonSeconds().coerceAtMost(vote.durationSeconds.coerceAtLeast(1L))
        if (!vote.endingSoonAnnounced && remainingSeconds <= endingSoonSeconds) {
            vote.endingSoonAnnounced = true
            broadcast(
                plugin.messages.text(
                    "command.vote.ending-soon",
                    "action" to actionLabel(vote.action),
                    "world" to vote.world.name,
                    "seconds" to remainingSeconds.toString(),
                    "yes" to vote.yesVotes.toString(),
                    "no" to vote.noVotes.toString(),
                ),
            )
            broadcast(buildVoteButtons(vote))
        }
    }

    private fun buildVoteButtons(vote: ActiveVote): Component {
        val yesCommand = "/voteyes"
        val noCommand = "/voteno"

        val yesButton = plugin.messages.text("command.vote.buttons.yes")
            .clickEvent(ClickEvent.runCommand(yesCommand))
            .hoverEvent(HoverEvent.showText(plugin.messages.text("command.vote.buttons.yes-hover", "command" to yesCommand)))

        val noButton = plugin.messages.text("command.vote.buttons.no")
            .clickEvent(ClickEvent.runCommand(noCommand))
            .hoverEvent(HoverEvent.showText(plugin.messages.text("command.vote.buttons.no-hover", "command" to noCommand)))

        return Component.empty()
            .append(plugin.messages.text("command.vote.buttons.prefix"))
            .appendSpace()
            .append(yesButton)
            .appendSpace()
            .append(noButton)
    }

    private fun broadcast(component: Component) {
        plugin.server.onlinePlayers.forEach { it.sendMessage(component) }
        plugin.server.consoleSender.sendMessage(component)
    }

    private fun hideBossBar(vote: ActiveVote) {
        plugin.server.onlinePlayers.forEach { it.hideBossBar(vote.bossBar) }
    }

    fun actionLabel(action: VoteAction): String =
        plainText.serialize(plugin.messages.text(action.translationKey))

    private fun voteDurationSeconds(): Long =
        plugin.config.getLong("votes.duration-seconds", 30L).coerceAtLeast(5L)

    private fun cooldownSeconds(action: VoteAction): Long =
        plugin.config.getLong("votes.commands.${action.commandLabel}.cooldown-seconds", 300L).coerceAtLeast(0L)

    private fun endingSoonSeconds(): Long =
        plugin.config.getLong("votes.ending-soon-seconds", 5L).coerceAtLeast(1L)

    private fun secondsRemaining(millis: Long): Long =
        ((millis.coerceAtLeast(0L) + 999L) / 1000L).coerceAtLeast(0L)

    private fun formatPercent(value: Double): String =
        String.format(Locale.US, "%.0f", value * 100.0)

    private fun incrementChoice(vote: ActiveVote, choice: VoteChoice) {
        when (choice) {
            VoteChoice.YES -> vote.yesVotes += 1
            VoteChoice.NO -> vote.noVotes += 1
        }
    }

    private fun decrementChoice(vote: ActiveVote, choice: VoteChoice) {
        when (choice) {
            VoteChoice.YES -> vote.yesVotes = (vote.yesVotes - 1).coerceAtLeast(0)
            VoteChoice.NO -> vote.noVotes = (vote.noVotes - 1).coerceAtLeast(0)
        }
    }

    private data class ActiveVote(
        val action: VoteAction,
        val world: World,
        val durationSeconds: Long,
        val endsAtMillis: Long,
        val bossBar: BossBar,
        val voters: MutableMap<UUID, VoteChoice> = mutableMapOf(),
        var yesVotes: Int = 0,
        var noVotes: Int = 0,
        var halfwayAnnounced: Boolean = false,
        var endingSoonAnnounced: Boolean = false,
    )

    private sealed interface CancelReason {
        data object RELOAD : CancelReason
        data class FORCED(val playerName: String) : CancelReason
    }
}

enum class VoteAction(
    val commandLabel: String,
    val startPermission: String,
    val forcePermission: String,
    val translationKey: String,
    val bossBarColor: BossBar.Color,
    val apply: (World) -> Unit,
) {
    DAY(
        commandLabel = "voteday",
        startPermission = "foxcore.voteday",
        forcePermission = "foxcore.voteday.force",
        translationKey = "command.vote.action.day",
        bossBarColor = BossBar.Color.YELLOW,
        apply = { world -> world.time = 1000L },
    ),
    NIGHT(
        commandLabel = "votenight",
        startPermission = "foxcore.votenight",
        forcePermission = "foxcore.votenight.force",
        translationKey = "command.vote.action.night",
        bossBarColor = BossBar.Color.PURPLE,
        apply = { world -> world.time = 13000L },
    ),
    SUN(
        commandLabel = "votesun",
        startPermission = "foxcore.votesun",
        forcePermission = "foxcore.votesun.force",
        translationKey = "command.vote.action.sun",
        bossBarColor = BossBar.Color.GREEN,
        apply = { world ->
            world.setStorm(false)
            world.isThundering = false
            world.weatherDuration = 6000
            world.thunderDuration = 0
        },
    ),
    RAIN(
        commandLabel = "voterain",
        startPermission = "foxcore.voterain",
        forcePermission = "foxcore.voterain.force",
        translationKey = "command.vote.action.rain",
        bossBarColor = BossBar.Color.BLUE,
        apply = { world ->
            world.setStorm(true)
            world.isThundering = false
        },
    ),
}

enum class VoteChoice {
    YES,
    NO,
}

sealed interface VoteStartResult {
    data object Disabled : VoteStartResult
    data class Cooldown(val secondsRemaining: Long) : VoteStartResult
    data class Active(val action: VoteAction) : VoteStartResult
    data class Started(val durationSeconds: Long) : VoteStartResult
}

sealed interface VoteCastResult {
    data object NoActiveVote : VoteCastResult
    data class Recorded(val choice: VoteChoice) : VoteCastResult
    data class Changed(val choice: VoteChoice) : VoteCastResult
    data class AlreadyVoted(val choice: VoteChoice) : VoteCastResult
}
