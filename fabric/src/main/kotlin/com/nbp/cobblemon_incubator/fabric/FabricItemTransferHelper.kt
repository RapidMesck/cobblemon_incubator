package com.nbp.cobblemon_incubator.fabric

import com.nbp.cobblemon_incubator.util.ItemTransferHelper
import com.nbp.cobblemon_incubator.util.PlatformHelper
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

object FabricItemTransferHelper : ItemTransferHelper {
    override fun insertItem(level: Level, pos: BlockPos, side: Direction, stack: ItemStack): Boolean {
        val storage = ItemStorage.SIDED.find(level, pos, side) ?: return false
        val countBefore = stack.count
        Transaction.openOuter().use { tx ->
            val inserted = storage.insert(ItemVariant.of(stack), stack.count.toLong(), tx)
            if (inserted > 0) {
                stack.shrink(inserted.toInt())
                tx.commit()
            }
        }
        return stack.count < countBefore
    }

    override fun extractItem(level: Level, pos: BlockPos, side: Direction, slot: Int, amount: Int): ItemStack {
        val storage = ItemStorage.SIDED.find(level, pos, side) ?: return ItemStack.EMPTY
        Transaction.openOuter().use { tx ->
            for (view in storage) {
                if (!view.isResourceBlank && amount > 0) {
                    val extracted = storage.extract(view.resource, amount.toLong(), tx)
                    if (extracted > 0) {
                        tx.commit()
                        return view.resource.toStack(extracted.toInt())
                    }
                }
            }
        }
        return ItemStack.EMPTY
    }

    fun register() {
        PlatformHelper.transfer = this
    }
}
