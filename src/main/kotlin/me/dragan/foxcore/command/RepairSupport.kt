package me.dragan.foxcore.command

import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable

object RepairSupport {
    fun repair(item: ItemStack?): RepairResult {
        if (item == null || item.type.isAir) {
            return RepairResult.INVALID_ITEM
        }

        if (item.type.maxDurability <= 0) {
            return RepairResult.NOT_REPAIRABLE
        }

        val meta = item.itemMeta as? Damageable ?: return RepairResult.NOT_REPAIRABLE
        if (meta.damage <= 0) {
            return RepairResult.ALREADY_REPAIRED
        }

        meta.damage = 0
        item.itemMeta = meta
        return RepairResult.REPAIRED
    }
}

enum class RepairResult {
    INVALID_ITEM,
    NOT_REPAIRABLE,
    ALREADY_REPAIRED,
    REPAIRED,
}
