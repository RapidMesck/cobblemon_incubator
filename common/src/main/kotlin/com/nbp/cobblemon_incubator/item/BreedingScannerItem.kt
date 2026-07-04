package com.nbp.cobblemon_incubator.item

import com.cobblemon.mod.common.block.PastureBlock
import com.cobblemon.mod.common.block.entity.PokemonPastureBlockEntity
import com.nbp.cobblemon_incubator.util.CobbreedingPreviewCompat
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.UseOnContext

class BreedingScannerItem(properties: Properties) : Item(properties) {
    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level
        val player = context.player
        if (level.isClientSide) return InteractionResult.SUCCESS
        if (player !is ServerPlayer) return InteractionResult.PASS

        val state = level.getBlockState(context.clickedPos)
        val pastureBlock = state.block as? PastureBlock
        if (pastureBlock == null) {
            player.sendSystemMessage(
                Component.translatable("message.cobblemon_incubator.breeding_scanner.use_on_pasture")
                    .withStyle(ChatFormatting.YELLOW)
            )
            return InteractionResult.FAIL
        }

        val pasturePos = pastureBlock.getBasePosition(state, context.clickedPos)
        val pastureEntity = level.getBlockEntity(pasturePos) as? PokemonPastureBlockEntity
        if (pastureEntity == null) {
            player.sendSystemMessage(
                Component.translatable("message.cobblemon_incubator.breeding_scanner.no_pasture")
                    .withStyle(ChatFormatting.RED)
            )
            return InteractionResult.FAIL
        }

        val pokemon = pastureEntity.tetheredPokemon.mapNotNull { it.getPokemon() }
        CobbreedingPreviewCompat.buildReport(pokemon).forEach(player::sendSystemMessage)
        return InteractionResult.SUCCESS
    }

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
        tooltipComponents.add(
            Component.translatable("item.cobblemon_incubator.breeding_scanner.description")
                .withStyle(ChatFormatting.GRAY)
        )
    }
}
