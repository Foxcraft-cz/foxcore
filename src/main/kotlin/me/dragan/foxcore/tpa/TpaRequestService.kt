package me.dragan.foxcore.tpa

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.ArrayDeque
import java.util.UUID

class TpaRequestService {
    private val requestsByTarget = mutableMapOf<UUID, ArrayDeque<TpaRequest>>()

    fun createRequest(requester: Player, target: Player, type: TpaRequestType, expirySeconds: Long) {
        cleanupTarget(target.uniqueId)
        val queue = requestsByTarget.getOrPut(target.uniqueId) { ArrayDeque() }
        queue.removeIf { it.requesterId == requester.uniqueId && it.type == type }
        queue.addLast(
            TpaRequest(
                requesterId = requester.uniqueId,
                requesterName = requester.name,
                targetId = target.uniqueId,
                type = type,
                expiresAtMillis = System.currentTimeMillis() + (expirySeconds * 1000L),
            ),
        )
    }

    fun acceptLatest(target: Player): TpaResolveResult =
        resolveLatest(target, remove = true)

    fun acceptFrom(target: Player, requesterName: String): TpaResolveResult =
        resolveFrom(target, requesterName, remove = true)

    fun denyLatest(target: Player): TpaResolveResult =
        resolveLatest(target, remove = true)

    fun denyFrom(target: Player, requesterName: String): TpaResolveResult =
        resolveFrom(target, requesterName, remove = true)

    fun pendingRequesterNames(target: Player): List<String> {
        cleanupTarget(target.uniqueId)
        return requestsByTarget[target.uniqueId]
            ?.map(TpaRequest::requesterName)
            ?.distinct()
            ?: emptyList()
    }

    fun removeAllFor(playerId: UUID) {
        requestsByTarget.remove(playerId)
        requestsByTarget.values.removeIf { queue ->
            queue.removeIf { it.requesterId == playerId }
            queue.isEmpty()
        }
    }

    private fun resolveLatest(target: Player, remove: Boolean): TpaResolveResult {
        cleanupTarget(target.uniqueId)
        val queue = requestsByTarget[target.uniqueId] ?: return TpaResolveResult(TpaResolveStatus.NO_PENDING)
        if (queue.isEmpty()) {
            return TpaResolveResult(TpaResolveStatus.NO_PENDING)
        }

        val request = queue.removeLast()
        if (!remove) {
            queue.addLast(request)
        }
        pruneEmptyQueue(target.uniqueId)
        return resolveRequester(request)
    }

    private fun resolveFrom(target: Player, requesterName: String, remove: Boolean): TpaResolveResult {
        cleanupTarget(target.uniqueId)
        val queue = requestsByTarget[target.uniqueId] ?: return TpaResolveResult(TpaResolveStatus.NO_PENDING)
        val request = queue.lastOrNull { it.requesterName.equals(requesterName, ignoreCase = true) }
            ?: return TpaResolveResult(TpaResolveStatus.NOT_FOUND_FOR_REQUESTER)
        if (remove) {
            queue.remove(request)
        }
        pruneEmptyQueue(target.uniqueId)
        return resolveRequester(request)
    }

    private fun resolveRequester(request: TpaRequest): TpaResolveResult {
        val requester = Bukkit.getPlayer(request.requesterId)
        return if (requester == null || !requester.isOnline) {
            TpaResolveResult(TpaResolveStatus.REQUESTER_OFFLINE, request = request)
        } else {
            TpaResolveResult(TpaResolveStatus.SUCCESS, request = request, requester = requester)
        }
    }

    private fun cleanupTarget(targetId: UUID) {
        val queue = requestsByTarget[targetId] ?: return
        val now = System.currentTimeMillis()
        queue.removeIf { it.expiresAtMillis <= now }
        pruneEmptyQueue(targetId)
    }

    private fun pruneEmptyQueue(targetId: UUID) {
        if (requestsByTarget[targetId]?.isEmpty() == true) {
            requestsByTarget.remove(targetId)
        }
    }
}
