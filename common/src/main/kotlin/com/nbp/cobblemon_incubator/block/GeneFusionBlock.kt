package com.nbp.cobblemon_incubator.block

import com.cobblemon.mod.common.CobblemonSounds
import com.mojang.serialization.MapCodec
import com.nbp.cobblemon_incubator.blockentity.GeneFusionBlockEntity
import com.nbp.cobblemon_incubator.config.IncubatorConfig
import com.nbp.cobblemon_incubator.registry.ModRegistries
import dev.architectury.registry.menu.MenuRegistry
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.phys.BlockHitResult

class GeneFusionBlock(properties: Properties) : BaseEntityBlock(properties) {
    companion object {
        val FACING: DirectionProperty = BlockStateProperties.HORIZONTAL_FACING
        val OPEN: BooleanProperty = BooleanProperty.create("open")
        val HAS_EGG: BooleanProperty = BooleanProperty.create("has_egg")
        val CODEC: MapCodec<GeneFusionBlock> = simpleCodec(::GeneFusionBlock)
    }

    init {
        registerDefaultState(
            stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(OPEN, false)
                .setValue(HAS_EGG, false)
        )
    }

    override fun codec(): MapCodec<out BaseEntityBlock> = CODEC

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = GeneFusionBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        blockEntityType: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        if (level.isClientSide) return null
        return createTickerHelper(
            blockEntityType,
            ModRegistries.GENE_FUSION_BLOCK_ENTITY.get(),
            GeneFusionBlockEntity::serverTick
        )
    }

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return defaultBlockState().setValue(FACING, context.horizontalDirection.opposite)
    }

    override fun rotate(state: BlockState, rotation: Rotation): BlockState {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)))
    }

    override fun mirror(state: BlockState, mirror: Mirror): BlockState {
        return state.rotate(mirror.getRotation(state.getValue(FACING)))
    }

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

    override fun onRemove(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        newState: BlockState,
        movedByPiston: Boolean
    ) {
        if (!state.`is`(newState.block)) {
            val blockEntity = level.getBlockEntity(pos)
            if (blockEntity is GeneFusionBlockEntity) {
                net.minecraft.world.Containers.dropContents(level, pos, blockEntity)
                level.updateNeighbourForOutputSignal(pos, this)
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston)
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING, OPEN, HAS_EGG)
    }

    private fun openMenu(level: Level, pos: BlockPos, player: Player) {
        if (level.isClientSide || player !is ServerPlayer) return
        if (!IncubatorConfig.geneFusionEnabled) {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("item.cobblemon_incubator.gene_fusion.disabled"),
                true
            )
            return
        }
        val blockEntity = level.getBlockEntity(pos)
        if (blockEntity is GeneFusionBlockEntity) {
            MenuRegistry.openExtendedMenu(player, blockEntity) { buffer -> buffer.writeBlockPos(pos) }
            level.playSound(null, pos, CobblemonSounds.PC_ON, SoundSource.BLOCKS, 0.5F, 1F)
            level.gameEvent(player, GameEvent.BLOCK_OPEN, pos)
        }
    }
}
