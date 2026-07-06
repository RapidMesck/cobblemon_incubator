package com.nbp.cobblemon_incubator.blockentity

import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.pokemon.IVs
import com.nbp.cobblemon_incubator.block.GeneFusionBlock
import com.nbp.cobblemon_incubator.config.IncubatorConfig
import com.nbp.cobblemon_incubator.item.StemCellSyringeItem
import com.nbp.cobblemon_incubator.menu.GeneFusionMenu
import com.nbp.cobblemon_incubator.registry.ModRegistries
import com.nbp.cobblemon_incubator.util.CobbreedingCompat
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.ContainerHelper
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity
import net.minecraft.world.level.block.state.BlockState
import kotlin.math.roundToInt

class GeneFusionBlockEntity(pos: BlockPos, state: BlockState) :
    BaseContainerBlockEntity(ModRegistries.GENE_FUSION_BLOCK_ENTITY.get(), pos, state) {

    companion object {
        const val SLOT_EGG_START = 0
        const val SLOT_EGG_END = 5
        const val SLOT_OUTPUT = 6
        const val SLOT_SYRINGE = 7
        const val CONTAINER_SIZE = 8
        const val DATA_COUNT = 6

        val STATS_ORDER = listOf(Stats.HP, Stats.ATTACK, Stats.DEFENCE, Stats.SPECIAL_ATTACK, Stats.SPECIAL_DEFENCE, Stats.SPEED)

        fun serverTick(level: Level, pos: BlockPos, state: BlockState, blockEntity: GeneFusionBlockEntity) {
            blockEntity.tickServer(level, pos, state)
        }
    }

    private var items: NonNullList<ItemStack> = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY)
    private var selectedNature = ""
    private var selectedAbility = ""
    private var openers = 0

    private val dataAccess = object : ContainerData {
        override fun get(index: Int): Int {
            return when (index) {
                0 -> availableNatures().indexOf(selectedNature).coerceAtLeast(0)
                1 -> availableAbilities().indexOf(selectedAbility).coerceAtLeast(0)
                2 -> syringeCharge()
                3 -> requiredCharge()
                4 -> fusionStatus()
                5 -> eggCount()
                else -> 0
            }
        }

        override fun set(index: Int, value: Int) {
            if (level == null || level!!.isClientSide) return
        }

        override fun getCount(): Int = DATA_COUNT
    }

    override fun getContainerSize(): Int = CONTAINER_SIZE
    override fun getItems(): NonNullList<ItemStack> = items
    override fun setItems(items: NonNullList<ItemStack>) { this.items = items }
    override fun getDefaultName(): Component = Component.translatable("container.cobblemon_incubator.gene_fusion")
    override fun createMenu(containerId: Int, inventory: Inventory): AbstractContainerMenu {
        return GeneFusionMenu(containerId, inventory, this, dataAccess)
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
        selectedNature = tag.getString("SelectedNature")
        selectedAbility = tag.getString("SelectedAbility")
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        ContainerHelper.saveAllItems(tag, items, registries)
        tag.putString("SelectedNature", selectedNature)
        tag.putString("SelectedAbility", selectedAbility)
    }

    override fun canPlaceItem(slot: Int, stack: ItemStack): Boolean {
        return when (slot) {
            in SLOT_EGG_START..SLOT_EGG_END -> CobbreedingCompat.isEgg(stack)
            SLOT_OUTPUT -> false
            SLOT_SYRINGE -> stack.`is`(ModRegistries.STEM_CELL_SYRINGE.get())
            else -> false
        }
    }

    private fun tickServer(level: Level, pos: BlockPos, state: BlockState) {
        updateBlockState()
    }

    fun eggCount(): Int {
        return (SLOT_EGG_START..SLOT_EGG_END).count { CobbreedingCompat.isEgg(items[it]) }
    }

    fun syringeCharge(): Int {
        val syringe = items[SLOT_SYRINGE]
        if (!syringe.`is`(ModRegistries.STEM_CELL_SYRINGE.get())) return 0
        return StemCellSyringeItem.getCharge(syringe)
    }

    fun requiredCharge(): Int {
        val bestIvs = computeBestIvs() ?: return 0
        val sum = STATS_ORDER.sumOf { bestIvs.getOrDefault(it) }
        return (sum * IncubatorConfig.geneFusionCostMultiplier).roundToInt().coerceAtLeast(0)
    }

    fun fusionStatus(): Int {
        if (eggCount() < 2) return 0
        if (syringeCharge() < requiredCharge()) return 0
        return 1
    }

    fun availableNatures(): List<String> {
        return (SLOT_EGG_START..SLOT_EGG_END)
            .mapNotNull { CobbreedingCompat.extractProperties(items[it])?.nature }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    fun availableAbilities(): List<String> {
        return (SLOT_EGG_START..SLOT_EGG_END)
            .mapNotNull { CobbreedingCompat.extractProperties(items[it])?.ability }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    fun getSelectedNature(): String = selectedNature
    fun getSelectedAbility(): String = selectedAbility

    fun selectNature(nature: String) {
        selectedNature = nature
        setChanged()
    }

    fun selectAbility(ability: String) {
        selectedAbility = ability
        setChanged()
    }

    fun computeBestIvs(): IVs? {
        val allProperties = (SLOT_EGG_START..SLOT_EGG_END)
            .mapNotNull { CobbreedingCompat.extractProperties(items[it]) }
        if (allProperties.isEmpty()) return null

        val result = IVs()
        STATS_ORDER.forEach { stat ->
            val best = allProperties.maxOf { it.ivs?.getOrDefault(stat) ?: 0 }
            result[stat] = best
        }
        return result
    }

    fun getPreviewIvs(): IVs? = computeBestIvs()

    fun performFusion(): Boolean {
        if (fusionStatus() != 1) return false

        val bestIvs = computeBestIvs() ?: return false
        val allProps = (SLOT_EGG_START..SLOT_EGG_END)
            .mapNotNull { CobbreedingCompat.extractProperties(items[it]) }
        if (allProps.isEmpty()) return false

        val baseEgg = items[SLOT_EGG_START]
        val baseProps = CobbreedingCompat.extractProperties(baseEgg) ?: return false
        val baseSpecies = baseProps.species ?: return false

        val nature = if (selectedNature.isNotBlank() && selectedNature in availableNatures())
            selectedNature else allProps.first().nature ?: ""

        val ability = if (selectedAbility.isNotBlank() && selectedAbility in availableAbilities())
            selectedAbility else allProps.first().ability ?: ""

        val timers = (SLOT_EGG_START..SLOT_EGG_END)
            .mapNotNull { CobbreedingCompat.getTimer(items[it]) }
        val avgTimer = if (timers.isNotEmpty()) timers.average().roundToInt() else 600

        val fusedProperties = PokemonProperties().apply {
            this.species = baseSpecies
            this.nature = nature
            this.ability = ability
            this.ivs = bestIvs
            this.form = baseProps.form
        }

        val resultEgg = CobbreedingCompat.createFusedEgg(fusedProperties, avgTimer) ?: return false
        if (!items[SLOT_OUTPUT].isEmpty) return false

        val syringe = items[SLOT_SYRINGE]
        val cost = requiredCharge()
        if (!StemCellSyringeItem.consumeCharge(syringe, cost)) return false

        for (i in SLOT_EGG_START..SLOT_EGG_END) {
            items[i] = ItemStack.EMPTY
        }
        items[SLOT_OUTPUT] = resultEgg
        selectedNature = ""
        selectedAbility = ""
        updateBlockState()
        setChanged()
        return true
    }

    private fun updateBlockState() {
        val level = level ?: return
        val state = blockState
        val open = openers > 0
        if (state.getValue(GeneFusionBlock.OPEN) != open) {
            level.setBlock(worldPosition, state.setValue(GeneFusionBlock.OPEN, open), 3)
        }
    }
}
