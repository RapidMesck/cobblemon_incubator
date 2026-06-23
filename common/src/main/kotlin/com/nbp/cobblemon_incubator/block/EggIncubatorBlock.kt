package com.nbp.cobblemon_incubator.block

import com.mojang.serialization.MapCodec
import com.nbp.cobblemon_incubator.blockentity.EggIncubatorBlockEntity
import com.nbp.cobblemon_incubator.registry.ModRegistries
import dev.architectury.registry.menu.MenuRegistry
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.phys.BlockHitResult

class EggIncubatorBlock(properties: Properties) : BaseEntityBlock(properties) {
    companion object {
        val HAS_EGG: BooleanProperty = BooleanProperty.create("has_egg")
        val CODEC: MapCodec<EggIncubatorBlock> = simpleCodec(::EggIncubatorBlock)
    }

    init {
        registerDefaultState(stateDefinition.any().setValue(HAS_EGG, false))
    }

    override fun codec(): MapCodec<out BaseEntityBlock> = CODEC

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = EggIncubatorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        blockEntityType: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        if (level.isClientSide) return null
        return createTickerHelper(
            blockEntityType,
            ModRegistries.EGG_INCUBATOR_BLOCK_ENTITY.get(),
            EggIncubatorBlockEntity::serverTick
        )
    }

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hitResult: BlockHitResult
    ): InteractionResult {
        openMenu(level, pos, player)
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    override fun useItemOn(
        stack: ItemStack,
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: net.minecraft.world.InteractionHand,
        hitResult: BlockHitResult
    ): ItemInteractionResult {
        openMenu(level, pos, player)
        return ItemInteractionResult.sidedSuccess(level.isClientSide)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, movedByPiston: Boolean) {
        if (!state.`is`(newState.block)) {
            val blockEntity = level.getBlockEntity(pos)
            if (blockEntity is EggIncubatorBlockEntity) {
                net.minecraft.world.Containers.dropContents(level, pos, blockEntity)
                level.updateNeighbourForOutputSignal(pos, this)
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston)
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(HAS_EGG)
    }

    private fun openMenu(level: Level, pos: BlockPos, player: Player) {
        if (level.isClientSide || player !is ServerPlayer) return
        val blockEntity = level.getBlockEntity(pos)
        if (blockEntity is EggIncubatorBlockEntity) {
            MenuRegistry.openExtendedMenu(player, blockEntity) { buffer -> buffer.writeBlockPos(pos) }
        }
    }
}
