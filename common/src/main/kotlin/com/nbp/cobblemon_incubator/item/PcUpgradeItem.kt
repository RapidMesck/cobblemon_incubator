package com.nbp.cobblemon_incubator.item

import com.nbp.cobblemon_incubator.registry.ModRegistries
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level

class PcUpgradeItem(properties: Properties) : Item(properties) {
    override fun use(level: Level, player: Player, usedHand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(usedHand)
        if (!level.isClientSide) {
            stack.set(ModRegistries.PC_UPGRADE_OWNER_UUID.get(), player.uuid.toString())
            stack.set(ModRegistries.PC_UPGRADE_OWNER_NAME.get(), player.gameProfile.name)
            player.sendSystemMessage(Component.translatable("item.cobblemon_incubator.pc_upgrade.bound", player.gameProfile.name))
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
    }

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
        tooltipComponents.add(
            Component.translatable("item.cobblemon_incubator.pc_upgrade.description")
                .withStyle(ChatFormatting.GRAY)
        )
        val owner = stack.get(ModRegistries.PC_UPGRADE_OWNER_NAME.get())
        if (owner == null) {
            tooltipComponents.add(
                Component.translatable("item.cobblemon_incubator.pc_upgrade.unbound")
                    .withStyle(ChatFormatting.YELLOW)
            )
        } else {
            tooltipComponents.add(
                Component.translatable("item.cobblemon_incubator.pc_upgrade.owner", owner)
                    .withStyle(ChatFormatting.AQUA)
            )
        }
    }
}
