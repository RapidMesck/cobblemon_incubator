package com.nbp.cobblemon_incubator.item

import com.nbp.cobblemon_incubator.config.IncubatorConfig
import com.nbp.cobblemon_incubator.registry.ModRegistries
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag

class StemCellSyringeItem(properties: Properties) : Item(properties) {

    companion object {
        fun getCharge(stack: ItemStack): Int {
            return stack.getOrDefault(ModRegistries.SYRINGE_CHARGE.get(), 0)
        }

        fun setCharge(stack: ItemStack, charge: Int) {
            stack.set(ModRegistries.SYRINGE_CHARGE.get(), charge.coerceIn(0, IncubatorConfig.geneFusionSyringeMaxCharge))
        }

        fun getMaxCharge(): Int = IncubatorConfig.geneFusionSyringeMaxCharge

        fun addCharge(stack: ItemStack, amount: Int): Int {
            val current = getCharge(stack)
            val max = getMaxCharge()
            val newCharge = (current + amount).coerceAtMost(max)
            setCharge(stack, newCharge)
            return newCharge - current
        }

        fun consumeCharge(stack: ItemStack, amount: Int): Boolean {
            val current = getCharge(stack)
            if (current < amount) return false
            setCharge(stack, current - amount)
            return true
        }
    }

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
        val charge = getCharge(stack)
        val max = getMaxCharge()
        val percent = if (max > 0) (charge * 100 / max) else 0
        val color = when {
            percent >= 80 -> ChatFormatting.GREEN
            percent >= 40 -> ChatFormatting.YELLOW
            else -> ChatFormatting.RED
        }
        tooltipComponents.add(
            Component.translatable(
                "item.cobblemon_incubator.stem_cell_syringe.charge",
                charge, max
            ).withStyle(color)
        )
        tooltipComponents.add(
            Component.translatable(
                "item.cobblemon_incubator.stem_cell_syringe.description"
            ).withStyle(ChatFormatting.GRAY)
        )
    }

    override fun isBarVisible(stack: ItemStack): Boolean {
        return getCharge(stack) > 0
    }

    override fun getBarWidth(stack: ItemStack): Int {
        val max = getMaxCharge()
        if (max <= 0) return 0
        return (13.0f * getCharge(stack) / max).toInt().coerceIn(0, 13)
    }

    override fun getBarColor(stack: ItemStack): Int {
        val percent = getCharge(stack).toFloat() / getMaxCharge().coerceAtLeast(1)
        return when {
            percent >= 0.8f -> 0x55FF55
            percent >= 0.4f -> 0xFFFF55
            else -> 0xFF5555
        }
    }
}
