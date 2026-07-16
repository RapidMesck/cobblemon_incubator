package com.nbp.cobblemon_incubator.util

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

interface ItemTransferHelper {
    fun insertItem(level: Level, pos: BlockPos, side: Direction, stack: ItemStack): Boolean

    fun extractItem(level: Level, pos: BlockPos, side: Direction, slot: Int, amount: Int): ItemStack
}

object PlatformHelper {
    var transfer: ItemTransferHelper? = null
}
