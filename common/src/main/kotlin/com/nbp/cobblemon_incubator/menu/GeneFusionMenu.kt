package com.nbp.cobblemon_incubator.menu

import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import com.nbp.cobblemon_incubator.blockentity.GeneFusionBlockEntity
import com.nbp.cobblemon_incubator.registry.ModRegistries
import com.nbp.cobblemon_incubator.util.CobbreedingCompat
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

class GeneFusionMenu : AbstractContainerMenu {
    private val playerInventory: Inventory
    val container: Container
    private val data: ContainerData
    var blockPos: BlockPos? = null
        private set

    constructor(containerId: Int, playerInventory: Inventory, buffer: FriendlyByteBuf) : this(
        containerId,
        playerInventory,
        SimpleContainer(GeneFusionBlockEntity.CONTAINER_SIZE),
        SimpleContainerData(GeneFusionBlockEntity.DATA_COUNT)
    ) {
        blockPos = buffer.readBlockPos()
    }

    constructor(
        containerId: Int,
        playerInventory: Inventory,
        container: Container,
        data: ContainerData
    ) : super(ModRegistries.GENE_FUSION_MENU.get(), containerId) {
        this.playerInventory = playerInventory
        this.container = container
        this.data = data

        checkContainerSize(container, GeneFusionBlockEntity.CONTAINER_SIZE)
        container.startOpen(playerInventory.player)

        val eggCols = intArrayOf(61, 82)
        val eggRows = intArrayOf(45, 66, 87)
        for (row in 0..2) {
            for (col in 0..1) {
                val index = GeneFusionBlockEntity.SLOT_EGG_START + col + row * 2
                addSlot(EggSlot(container, index, eggCols[col], eggRows[row]))
            }
        }

        addSlot(OutputSlot(container, GeneFusionBlockEntity.SLOT_OUTPUT, 180, 45))
        addSlot(SyringeSlot(container, GeneFusionBlockEntity.SLOT_SYRINGE, 180, 87))

        val mainX = 54
        for (row in 0..2) {
            for (col in 0..8) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, mainX + col * 18, 122 + row * 18))
            }
        }
        for (col in 0..8) {
            addSlot(Slot(playerInventory, col, mainX + col * 18, 180))
        }

        addDataSlots(data)
    }

    val selectedNatureIndex: Int get() = data.get(0)
    val selectedAbilityIndex: Int get() = data.get(1)
    val syringeCharge: Int get() = data.get(2)
    val requiredCharge: Int get() = data.get(3)
    val fusionStatus: Int get() = data.get(4)
    val eggCount: Int get() = data.get(5)

    private fun eggItems(): List<ItemStack> =
        (GeneFusionBlockEntity.SLOT_EGG_START..GeneFusionBlockEntity.SLOT_EGG_END).map { container.getItem(it) }

    /** Setado pelo GeneFusionScreen a cada frame com os dados já resolvidos no servidor. */
    var clientEggPropertiesOverride: List<PokemonProperties?>? = null

    private fun eggPropertiesList(): List<PokemonProperties?> =
        clientEggPropertiesOverride ?: eggItems().map { CobbreedingCompat.extractProperties(it) }

    fun availableNatures(): List<String> = GeneFusionBlockEntity.natureOptions(eggPropertiesList())
    fun availableAbilities(): List<String> = GeneFusionBlockEntity.abilityOptions(eggPropertiesList())
    fun previewIvs() = GeneFusionBlockEntity.bestIvs(eggPropertiesList())

    fun getSelectedNature(): String = availableNatures().getOrNull(selectedNatureIndex) ?: ""
    fun getSelectedAbility(): String = availableAbilities().getOrNull(selectedAbilityIndex) ?: ""

    override fun stillValid(player: Player): Boolean = container.stillValid(player)

    override fun clickMenuButton(player: Player, id: Int): Boolean {
        val be = container as? GeneFusionBlockEntity ?: return false
        return when (id) {
            0 -> {
                val natures = be.availableNatures()
                if (natures.isEmpty()) return false
                val current = be.getSelectedNature()
                val idx = natures.indexOf(current).let { if (it < 0) -1 else it }
                be.selectNature(natures[Math.floorMod(idx + 1, natures.size)])
                true
            }

            1 -> {
                val natures = be.availableNatures()
                if (natures.isEmpty()) return false
                val current = be.getSelectedNature()
                val idx = natures.indexOf(current).let { if (it < 0) 0 else it }
                be.selectNature(natures[Math.floorMod(idx - 1, natures.size)])
                true
            }

            2 -> {
                val abilities = be.availableAbilities()
                if (abilities.isEmpty()) return false
                val current = be.getSelectedAbility()
                val idx = abilities.indexOf(current).let { if (it < 0) -1 else it }
                be.selectAbility(abilities[Math.floorMod(idx + 1, abilities.size)])
                true
            }

            3 -> {
                val abilities = be.availableAbilities()
                if (abilities.isEmpty()) return false
                val current = be.getSelectedAbility()
                val idx = abilities.indexOf(current).let { if (it < 0) 0 else it }
                be.selectAbility(abilities[Math.floorMod(idx - 1, abilities.size)])
                true
            }

            4 -> {
                be.performFusion()
                true
            }

            else -> false
        }
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var result = ItemStack.EMPTY
        val slot = slots[index]
        if (!slot.hasItem()) return result

        val stack = slot.item
        result = stack.copy()
        val containerSlots = GeneFusionBlockEntity.CONTAINER_SIZE

        if (index < containerSlots) {
            if (!moveItemStackTo(stack, containerSlots, slots.size, true)) return ItemStack.EMPTY
        } else if (CobbreedingCompat.isEgg(stack)) {
            if (!moveItemStackTo(
                    stack,
                    GeneFusionBlockEntity.SLOT_EGG_START,
                    GeneFusionBlockEntity.SLOT_EGG_END + 1,
                    false
                )
            ) {
                return ItemStack.EMPTY
            }
        } else if (stack.`is`(ModRegistries.STEM_CELL_SYRINGE.get())) {
            if (!moveItemStackTo(
                    stack,
                    GeneFusionBlockEntity.SLOT_SYRINGE,
                    GeneFusionBlockEntity.SLOT_SYRINGE + 1,
                    false
                )
            ) {
                return ItemStack.EMPTY
            }
        } else {
            return ItemStack.EMPTY
        }

        if (stack.isEmpty) slot.setByPlayer(ItemStack.EMPTY) else slot.setChanged()
        return result
    }

    override fun removed(player: Player) {
        super.removed(player)
        container.stopOpen(player)
    }

    private class EggSlot(container: Container, index: Int, x: Int, y: Int) : Slot(container, index, x, y) {
        override fun mayPlace(stack: ItemStack): Boolean {
            if (!CobbreedingCompat.isEgg(stack)) return false

            val newSpecies = CobbreedingCompat.extractProperties(stack)?.species ?: return false

            for (i in GeneFusionBlockEntity.SLOT_EGG_START..GeneFusionBlockEntity.SLOT_EGG_END) {
                if (i == index) continue
                val existing = container.getItem(i)
                if (existing.isEmpty) continue
                val existingSpecies = CobbreedingCompat.extractProperties(existing)?.species
                if (existingSpecies != null && existingSpecies != newSpecies) return false
            }

            return true
        }
    }

    private class OutputSlot(container: Container, slot: Int, x: Int, y: Int) : Slot(container, slot, x, y) {
        override fun mayPlace(stack: ItemStack): Boolean = false
    }

    private class SyringeSlot(container: Container, slot: Int, x: Int, y: Int) : Slot(container, slot, x, y) {
        override fun mayPlace(stack: ItemStack): Boolean = stack.`is`(ModRegistries.STEM_CELL_SYRINGE.get())
    }
}
