package com.nbp.cobblemon_incubator.blockentity

import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import com.cobblemon.mod.common.block.PastureBlock
import com.cobblemon.mod.common.block.entity.PokemonPastureBlockEntity
import com.nbp.cobblemon_incubator.block.EggIncubatorBlock
import com.nbp.cobblemon_incubator.blockentity.GeneFusionBlockEntity
import com.nbp.cobblemon_incubator.config.IncubatorConfig
import com.nbp.cobblemon_incubator.menu.EggIncubatorMenu
import com.nbp.cobblemon_incubator.registry.ModRegistries
import com.nbp.cobblemon_incubator.util.CobbreedingCompat
import com.nbp.cobblemon_incubator.util.FilterConfig
import com.nbp.cobblemon_incubator.util.PlatformHelper
import com.nbp.cobblemon_incubator.util.RejectAction
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.ContainerHelper
import net.minecraft.world.WorldlyContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity
import net.minecraft.world.level.block.state.BlockState
import java.util.UUID

class EggIncubatorBlockEntity(pos: BlockPos, state: BlockState) :
    BaseContainerBlockEntity(ModRegistries.EGG_INCUBATOR_BLOCK_ENTITY.get(), pos, state) {

    companion object {
        const val SLOT_INPUT = 0
        const val SLOT_OUTPUT = 1
        const val SLOT_UPGRADE_START = 2
        const val SLOT_UPGRADE_END = 4
        const val CONTAINER_SIZE = 5
        const val DATA_COUNT = 8

        fun serverTick(level: Level, pos: BlockPos, state: BlockState, blockEntity: EggIncubatorBlockEntity) {
            blockEntity.tickServer(level, pos, state)
        }
    }

    private var items: NonNullList<ItemStack> = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY)
    private var cachedTimer = 0
    private var cachedMaxTimer = 0
    private var syncedEggProperties: String = ""
    private var openers = 0

    private val dataAccess = object : ContainerData {
        override fun get(index: Int): Int {
            return when (index) {
                0 -> cachedTimer
                1 -> cachedMaxTimer
                2 -> incubationSpeed()
                3 -> if (pcUpgradeOwner() != null) 1 else 0
                4 -> IncubatorConfig.syncedDisplayMask
                5 -> IncubatorConfig.syncedUpgradeMask
                6 -> IncubatorConfig.syncedFilterMask
                7 -> IncubatorConfig.syncedAutomationMask
                else -> 0
            }
        }

        override fun set(index: Int, value: Int) {
            when (index) {
                0 -> cachedTimer = value
                1 -> cachedMaxTimer = value
            }
        }

        override fun getCount(): Int = DATA_COUNT
    }

    override fun getContainerSize(): Int = CONTAINER_SIZE

    override fun getItems(): NonNullList<ItemStack> = items

    override fun setItems(items: NonNullList<ItemStack>) {
        this.items = items
    }

    override fun getDefaultName(): Component = Component.translatable("container.cobblemon_incubator.egg_incubator")

    override fun createMenu(containerId: Int, inventory: Inventory): AbstractContainerMenu {
        return EggIncubatorMenu(containerId, inventory, this, dataAccess)
    }

    override fun startOpen(player: Player) {
        if (player.isSpectator) return
        openers++
        updateBlockState()
    }

    override fun stopOpen(player: Player) {
        if (player.isSpectator) return
        openers = (openers - 1).coerceAtLeast(0)
        updateBlockState()
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        items = NonNullList.withSize(containerSize, ItemStack.EMPTY)
        ContainerHelper.loadAllItems(tag, items, registries)
        cachedTimer = tag.getInt("CachedTimer")
        cachedMaxTimer = tag.getInt("CachedMaxTimer")
        syncedEggProperties = tag.getString("SyncedEggProperties")
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        ContainerHelper.saveAllItems(tag, items, registries)
        tag.putInt("CachedTimer", cachedTimer)
        tag.putInt("CachedMaxTimer", cachedMaxTimer)
        tag.putString("SyncedEggProperties", syncedEggProperties)
    }

    override fun setItem(slot: Int, stack: ItemStack) {
        super.setItem(slot, stack)
        if (slot == SLOT_INPUT) {
            cachedMaxTimer = CobbreedingCompat.getTimer(stack) ?: cachedMaxTimer
            refreshSyncedEggProperties()
            updateBlockState()
        }
    }

    override fun canPlaceItem(slot: Int, stack: ItemStack): Boolean {
        return when (slot) {
            SLOT_INPUT -> CobbreedingCompat.isEgg(stack)
            SLOT_OUTPUT -> false
            in SLOT_UPGRADE_START..SLOT_UPGRADE_END -> {
                if (!isUpgrade(stack)) return false
                if (stack.`is`(ModRegistries.SPEED_UPGRADE.get())) {
                    (SLOT_UPGRADE_START..SLOT_UPGRADE_END).none { i ->
                        i != slot && items[i].`is`(ModRegistries.SPEED_UPGRADE.get())
                    }
                } else true
            }

            else -> false
        }
    }

    private fun tickServer(level: Level, pos: BlockPos, state: BlockState) {
        refreshSyncedEggProperties()
        updateBlockState()
        if (IncubatorConfig.autoOutputToInventories) {
            pushOutputToAdjacentInventory(level, pos)
        }

        if (IncubatorConfig.autoInputFromPastures && items[SLOT_INPUT].isEmpty) {
            pullEggFromAdjacentPasture(level, pos)
        }

        val egg = items[SLOT_INPUT]
        if (!CobbreedingCompat.isEgg(egg)) {
            cachedTimer = 0
            cachedMaxTimer = 0
            updateBlockState()
            return
        }

        val timer = CobbreedingCompat.getTimer(egg) ?: return
        if (cachedMaxTimer <= 0 || timer > cachedMaxTimer) cachedMaxTimer = timer
        cachedTimer = timer
        updateBlockState()

        // Handle filter: reject eggs that don't match
        if (!passesFilter(egg)) {
            if (IncubatorConfig.ignoreFilterOnShiny && isEggShiny(egg)) {
                // Shiny eggs bypass the filter
            } else {
                rejectEgg(level)
                return
            }
        }

        // Analyse upgrade: disables incubation, turns incubator into analyzer
        if (hasAnalyseUpgrade()) {
            // If filter upgrade is also present, route passed eggs to adjacent gene fusion
            if (hasUpgrade(ModRegistries.FILTER_UPGRADE.get())) {
                pushEggToAdjacentGeneFusion(level, pos)
            }
            return
        }

        if (timer <= 0) {
            finishEgg(level)
            return
        }

        val nextTimer = timer - incubationSpeed()
        CobbreedingCompat.setTimer(egg, nextTimer)
        cachedTimer = nextTimer.coerceAtLeast(0)
        setChanged(level, pos, state)

        if (nextTimer <= 0) {
            finishEgg(level)
        }
    }

    private fun pushOutputToAdjacentInventory(level: Level, pos: BlockPos): Boolean {
        val output = items[SLOT_OUTPUT]
        if (output.isEmpty) return false

        for (direction in Direction.entries) {
            val adjacentPos = pos.relative(direction)
            if (level.getBlockState(adjacentPos).block is PastureBlock) continue

            val destination = level.getBlockEntity(adjacentPos) as? Container
            if (destination == null) {
                val transfer = PlatformHelper.transfer
                if (transfer != null && transfer.insertItem(level, adjacentPos, direction.opposite, output)) {
                    if (output.isEmpty) items[SLOT_OUTPUT] = ItemStack.EMPTY
                    setChanged()
                    level.sendBlockUpdated(
                        adjacentPos,
                        level.getBlockState(adjacentPos),
                        level.getBlockState(adjacentPos),
                        3
                    )
                    return true
                }
                continue
            }
            val insertionFace = direction.opposite
            val slots = if (destination is WorldlyContainer) {
                destination.getSlotsForFace(insertionFace)
            } else {
                IntArray(destination.containerSize) { it }
            }

            for (slot in slots) {
                if (!canInsertInto(destination, slot, output, insertionFace)) continue

                val target = destination.getItem(slot)
                val maxStackSize = minOf(destination.maxStackSize, output.maxStackSize)
                val moved = if (target.isEmpty) {
                    val amount = minOf(output.count, maxStackSize)
                    val inserted = output.copy()
                    inserted.count = amount
                    destination.setItem(slot, inserted)
                    amount
                } else if (ItemStack.isSameItemSameComponents(target, output)) {
                    val amount = minOf(output.count, maxStackSize - target.count)
                    if (amount <= 0) continue
                    target.grow(amount)
                    destination.setItem(slot, target)
                    amount
                } else {
                    continue
                }

                output.shrink(moved)
                if (output.isEmpty) items[SLOT_OUTPUT] = ItemStack.EMPTY
                destination.setChanged()
                setChanged()
                level.sendBlockUpdated(
                    adjacentPos,
                    level.getBlockState(adjacentPos),
                    level.getBlockState(adjacentPos),
                    3
                )
                return true
            }
        }

        return false
    }

    private fun canInsertInto(
        destination: Container,
        slot: Int,
        stack: ItemStack,
        insertionFace: Direction
    ): Boolean {
        if (!destination.canPlaceItem(slot, stack)) return false
        return destination !is WorldlyContainer ||
                destination.canPlaceItemThroughFace(slot, stack, insertionFace)
    }

    private fun pullEggFromAdjacentPasture(level: Level, pos: BlockPos): Boolean {
        val visitedPastures = mutableSetOf<BlockPos>()

        for (direction in Direction.entries) {
            val adjacentPos = pos.relative(direction)
            val adjacentState = level.getBlockState(adjacentPos)
            val pastureBlock = adjacentState.block as? PastureBlock ?: continue
            val pasturePos = pastureBlock.getBasePosition(adjacentState, adjacentPos)
            if (!visitedPastures.add(pasturePos)) continue

            val pastureEntity = level.getBlockEntity(pasturePos) as? PokemonPastureBlockEntity ?: continue
            val pastureInventory = pastureEntity as? Container ?: continue

            for (slot in 0 until pastureInventory.containerSize) {
                val candidate = pastureInventory.getItem(slot)
                if (!CobbreedingCompat.isEgg(candidate)) continue

                val extracted = pastureInventory.removeItem(slot, 1)
                if (extracted.isEmpty) continue

                setItem(SLOT_INPUT, extracted)
                pastureEntity.setChanged()
                level.sendBlockUpdated(
                    pasturePos,
                    level.getBlockState(pasturePos),
                    level.getBlockState(pasturePos),
                    3
                )
                return true
            }
        }

        return false
    }

    private fun finishEgg(level: Level) {
        val egg = items[SLOT_INPUT]
        if (egg.isEmpty) return

        val owner = pcUpgradeOwner()
        val player = owner?.let { level.server?.playerList?.getPlayer(it) }
        if (player is ServerPlayer && CobbreedingCompat.hatchToPc(player, owner, egg)) {
            val pokemonName = CobbreedingCompat.getSpeciesDisplayName(egg)
            player.sendSystemMessage(
                Component.translatable(
                    "message.cobblemon_incubator.sent_to_pc",
                    pokemonName
                )
            )
            items[SLOT_INPUT] = ItemStack.EMPTY
            updateBlockState()
            setChanged()
            return
        }

        if (items[SLOT_OUTPUT].isEmpty) {
            items[SLOT_OUTPUT] = egg.copy()
            items[SLOT_INPUT] = ItemStack.EMPTY
            updateBlockState()
            setChanged()
        }
    }

    fun incubationSpeed(): Int {
        return if (IncubatorConfig.speedUpgradeEnabled && hasUpgrade(ModRegistries.SPEED_UPGRADE.get())) {
            IncubatorConfig.upgradedSpeed
        } else {
            IncubatorConfig.baseSpeed
        }
    }

    fun filterConfig(): FilterConfig? {
        if (!IncubatorConfig.filterUpgradeEnabled) return null
        for (slot in SLOT_UPGRADE_START..SLOT_UPGRADE_END) {
            val stack = items[slot]
            if (stack.`is`(ModRegistries.FILTER_UPGRADE.get())) {
                return FilterConfig.fromStack(stack).enabledOnly()
            }
        }
        return null
    }

    fun pcUpgradeOwner(): UUID? {
        if (!IncubatorConfig.pcUpgradeEnabled) return null
        for (slot in SLOT_UPGRADE_START..SLOT_UPGRADE_END) {
            val stack = items[slot]
            if (stack.`is`(ModRegistries.PC_UPGRADE.get())) {
                val uuid = stack.get(ModRegistries.PC_UPGRADE_OWNER_UUID.get()) ?: return null
                return runCatching { UUID.fromString(uuid) }.getOrNull()
            }
        }
        return null
    }

    private fun hasUpgrade(item: net.minecraft.world.item.Item): Boolean {
        for (slot in SLOT_UPGRADE_START..SLOT_UPGRADE_END) {
            if (items[slot].`is`(item)) return true
        }
        return false
    }

    private fun hasAnalyseUpgrade(): Boolean {
        return IncubatorConfig.analyseUpgradeEnabled && hasUpgrade(ModRegistries.ANALYSE_UPGRADE.get())
    }

    private fun pushEggToAdjacentGeneFusion(level: Level, pos: BlockPos) {
        val egg = items[SLOT_INPUT]
        if (egg.isEmpty) return

        for (direction in Direction.entries) {
            val adjacentPos = pos.relative(direction)
            val geneFusion = level.getBlockEntity(adjacentPos) as? GeneFusionBlockEntity ?: continue

            val targetSlot = (GeneFusionBlockEntity.SLOT_EGG_START..GeneFusionBlockEntity.SLOT_EGG_END)
                .firstOrNull { geneFusion.getItem(it).isEmpty }
                ?: continue

            geneFusion.setItem(targetSlot, egg.copy())
            items[SLOT_INPUT] = ItemStack.EMPTY
            cachedTimer = 0
            cachedMaxTimer = 0
            updateBlockState()
            setChanged()
            geneFusion.setChanged()
            return
        }
    }

    private fun isUpgrade(stack: ItemStack): Boolean {
        return (IncubatorConfig.speedUpgradeEnabled && stack.`is`(ModRegistries.SPEED_UPGRADE.get())) ||
                (IncubatorConfig.pcUpgradeEnabled && stack.`is`(ModRegistries.PC_UPGRADE.get())) ||
                (IncubatorConfig.filterUpgradeEnabled && stack.`is`(ModRegistries.FILTER_UPGRADE.get())) ||
                (IncubatorConfig.analyseUpgradeEnabled && stack.`is`(ModRegistries.ANALYSE_UPGRADE.get()))
    }

    private fun isEggShiny(egg: ItemStack): Boolean {
        return CobbreedingCompat.extractProperties(egg)?.shiny == true
    }

    private fun passesFilter(egg: ItemStack): Boolean {
        val config = filterConfig() ?: return true
        if (!config.hasCriteria()) return true
        val properties = CobbreedingCompat.extractProperties(egg) ?: return false
        return config.matches(properties)
    }

    private fun rejectEgg(level: Level) {
        val config = filterConfig() ?: return
        val egg = items[SLOT_INPUT]
        if (egg.isEmpty) return

        when (config.rejectAction) {
            RejectAction.DELETE -> {
                items[SLOT_INPUT] = ItemStack.EMPTY
                cachedTimer = 0
                cachedMaxTimer = 0
                updateBlockState()
                setChanged()
            }

            RejectAction.OUTPUT -> {
                if (items[SLOT_OUTPUT].isEmpty) {
                    items[SLOT_OUTPUT] = egg.copy()
                    items[SLOT_INPUT] = ItemStack.EMPTY
                    cachedTimer = 0
                    cachedMaxTimer = 0
                    updateBlockState()
                    setChanged()
                }
            }
        }
        level.updateNeighbourForOutputSignal(worldPosition, blockState.block)
    }

    private fun refreshSyncedEggProperties() {
        val level = level ?: return
        if (level.isClientSide) return

        val resolved = CobbreedingCompat.extractProperties(items[SLOT_INPUT])?.asString(" ") ?: ""
        if (resolved != syncedEggProperties) {
            syncedEggProperties = resolved
            level.sendBlockUpdated(worldPosition, blockState, blockState, 2)
        }
    }

    /** Usado apenas no lado cliente, populado via getUpdateTag/loadAdditional. */
    fun clientEggProperties(): PokemonProperties? {
        if (syncedEggProperties.isBlank()) return null
        return runCatching { PokemonProperties.parse(syncedEggProperties) }.getOrNull()
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = super.getUpdateTag(registries)
        tag.putString("SyncedEggProperties", syncedEggProperties)
        return tag
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener> = ClientboundBlockEntityDataPacket.create(this)

    private fun updateBlockState() {
        val level = level ?: return
        val state = blockState
        val hasEgg = CobbreedingCompat.isEgg(items[SLOT_INPUT])
        val open = openers > 0
        if (state.getValue(EggIncubatorBlock.HAS_EGG) != hasEgg ||
            state.getValue(EggIncubatorBlock.OPEN) != open
        ) {
            level.setBlock(
                worldPosition,
                state
                    .setValue(EggIncubatorBlock.HAS_EGG, hasEgg)
                    .setValue(EggIncubatorBlock.OPEN, open),
                3
            )
        }
    }
}
