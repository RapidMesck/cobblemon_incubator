package com.nbp.cobblemon_incubator.item

import com.nbp.cobblemon_incubator.config.IncubatorConfig
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag

class SpeedUpgradeItem(properties: Properties) : Item(properties) {
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
        tooltipComponents.add(
            Component.translatable(
                "item.cobblemon_incubator.speed_upgrade.description",
                formatMultiplier(IncubatorConfig.speedUpgradeMultiplier)
            ).withStyle(ChatFormatting.GRAY)
        )
    }

    private fun formatMultiplier(multiplier: Double): String {
        return if (multiplier % 1.0 == 0.0) {
            multiplier.toInt().toString()
        } else {
            multiplier.toString()
        }
    }
}
