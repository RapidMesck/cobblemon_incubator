package com.nbp.cobblemon_incubator.item

import com.nbp.cobblemon_incubator.config.IncubatorConfig
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag

class AnalyseUpgradeItem(properties: Properties) : Item(properties) {
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
        if (!IncubatorConfig.analyseUpgradeEnabled) {
            tooltipComponents.add(
                Component.translatable("item.cobblemon_incubator.upgrade.disabled")
                    .withStyle(ChatFormatting.RED)
            )
            return
        }
        tooltipComponents.add(
            Component.translatable("item.cobblemon_incubator.analyse_upgrade.description")
                .withStyle(ChatFormatting.GRAY)
        )
    }
}
