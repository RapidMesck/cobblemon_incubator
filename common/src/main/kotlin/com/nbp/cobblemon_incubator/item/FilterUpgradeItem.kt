package com.nbp.cobblemon_incubator.item

import com.nbp.cobblemon_incubator.util.FilterConfig
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag

class FilterUpgradeItem(properties: Properties) : Item(properties) {
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
        tooltipComponents.add(
            Component.translatable("item.cobblemon_incubator.filter_upgrade.description")
                .withStyle(ChatFormatting.GRAY)
        )
        val config = FilterConfig.fromStack(stack)
        if (!config.hasCriteria()) {
            tooltipComponents.add(
                Component.translatable("item.cobblemon_incubator.filter_upgrade.empty")
                    .withStyle(ChatFormatting.YELLOW)
            )
            return
        }
        tooltipComponents.add(Component.literal(config.summary()).withStyle(ChatFormatting.AQUA))
        tooltipComponents.add(
            Component.translatable(
                "item.cobblemon_incubator.filter_upgrade.reject",
                config.rejectAction.label
            ).withStyle(ChatFormatting.DARK_GRAY)
        )
    }
}
