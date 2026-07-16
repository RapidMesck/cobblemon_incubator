package com.nbp.cobblemon_incubator.neoforge

import com.nbp.cobblemon_incubator.util.ItemTransferHelper
import com.nbp.cobblemon_incubator.util.PlatformHelper
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.items.IItemHandler
import net.neoforged.neoforge.items.ItemHandlerHelper

object NeoForgeItemTransferHelper : ItemTransferHelper {
    override fun insertItem(level: Level, pos: BlockPos, side: Direction, stack: ItemStack): Boolean {
        val handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side) ?: return false
        val countBefore = stack.count
        val remainder = ItemHandlerHelper.insertItem(handler, stack.copy(), false)
        val inserted = countBefore - remainder.count
        if (inserted > 0) {
            stack.shrink(inserted)
            return true
        }
        return false
    }

    override fun extractItem(level: Level, pos: BlockPos, side: Direction, slot: Int, amount: Int): ItemStack {
        val handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side) ?: return ItemStack.EMPTY
        return handler.extractItem(slot, amount, false)
    }

    fun register() {
        PlatformHelper.transfer = this
    }
}
