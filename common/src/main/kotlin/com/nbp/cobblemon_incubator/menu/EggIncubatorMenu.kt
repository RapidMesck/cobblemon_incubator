package com.nbp.cobblemon_incubator.menu

import com.nbp.cobblemon_incubator.blockentity.EggIncubatorBlockEntity
import com.nbp.cobblemon_incubator.registry.ModRegistries
import com.nbp.cobblemon_incubator.util.CobbreedingCompat
import com.nbp.cobblemon_incubator.util.FilterConfig
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

class EggIncubatorMenu : AbstractContainerMenu {
    private val playerInventory: Inventory
    private val container: Container
    private val data: ContainerData
    var blockPos: BlockPos? = null
        private set

    constructor(containerId: Int, playerInventory: Inventory, buffer: FriendlyByteBuf) : this(
        containerId,
        playerInventory,
        SimpleContainer(EggIncubatorBlockEntity.CONTAINER_SIZE),
        SimpleContainerData(EggIncubatorBlockEntity.DATA_COUNT)
    ) {
        blockPos = buffer.readBlockPos()
    }

    constructor(
        containerId: Int,
        playerInventory: Inventory,
        container: Container,
        data: ContainerData
    ) : super(ModRegistries.EGG_INCUBATOR_MENU.get(), containerId) {
        this.playerInventory = playerInventory
        this.container = container
        this.data = data

        checkContainerSize(container, EggIncubatorBlockEntity.CONTAINER_SIZE)
        container.startOpen(playerInventory.player)

        val mainX = 92
        addSlot(EggSlot(container, EggIncubatorBlockEntity.SLOT_INPUT, 110, 52))
        addSlot(OutputSlot(container, EggIncubatorBlockEntity.SLOT_OUTPUT, 218, 52))

        addSlot(UpgradeSlot(container, 2, 141, 91))
        addSlot(UpgradeSlot(container, 3, 164, 91))
        addSlot(UpgradeSlot(container, 4, 187, 91))

        for (row in 0..2) {
            for (column in 0..8) {
                addSlot(Slot(playerInventory, column + row * 9 + 9, mainX + column * 18, 122 + row * 18))
            }
        }

        for (column in 0..8) {
            addSlot(Slot(playerInventory, column, mainX + column * 18, 180))
        }

        addDataSlots(data)
    }

    val remainingTimer: Int
        get() = data.get(0)

    val maxTimer: Int
        get() = data.get(1)

    val speedMultiplier: Int
        get() = data.get(2)

    val hasPcUpgrade: Boolean
        get() = data.get(3) == 1

    val inputStack: ItemStack
        get() = slots[EggIncubatorBlockEntity.SLOT_INPUT].item

    val filterStack: ItemStack
        get() = (EggIncubatorBlockEntity.SLOT_UPGRADE_START..EggIncubatorBlockEntity.SLOT_UPGRADE_END)
            .map { slots[it].item }
            .firstOrNull { it.`is`(ModRegistries.FILTER_UPGRADE.get()) }
            ?: ItemStack.EMPTY

    val filterConfig: FilterConfig
        get() = if (filterStack.isEmpty) FilterConfig() else FilterConfig.fromStack(filterStack)

    override fun stillValid(player: Player): Boolean = container.stillValid(player)

    override fun clickMenuButton(player: Player, id: Int): Boolean {
        val filter = findFilterStack() ?: return false
        val current = FilterConfig.fromStack(filter)
        val next = when (id) {
            0 -> FilterConfig()
            1 -> current.withSpeciesFrom(CobbreedingCompat.extractProperties(container.getItem(EggIncubatorBlockEntity.SLOT_INPUT)))
            2 -> current.cycleNature(-1)
            3 -> current.cycleNature(1)
            4 -> current.cycleAbility(-1)
            5 -> current.cycleAbility(1)
            6 -> current.cycleRejectAction()
            in 100..105 -> current.cycleIvOperator(id - 100)
            in 110..115 -> current.adjustIv(id - 110, -1)
            in 120..125 -> current.adjustIv(id - 120, 1)
            in 130..135 -> current.clearIv(id - 130)
            in 2000..2099 -> current.setNatureByIndex(id - 2000)
            in 3000..9999 -> current.setAbilityByIndex(id - 3000)
            else -> return false
        }
        FilterConfig.save(filter, next)
        container.setChanged()
        broadcastChanges()
        return true
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var result = ItemStack.EMPTY
        val slot = slots[index]
        if (!slot.hasItem()) return result

        val stack = slot.item
        result = stack.copy()
        val containerSlots = EggIncubatorBlockEntity.CONTAINER_SIZE

        if (index < containerSlots) {
            if (!moveItemStackTo(stack, containerSlots, slots.size, true)) return ItemStack.EMPTY
        } else if (CobbreedingCompat.isEgg(stack)) {
            if (!moveItemStackTo(
                    stack,
                    EggIncubatorBlockEntity.SLOT_INPUT,
                    EggIncubatorBlockEntity.SLOT_INPUT + 1,
                    false
                )
            ) {
                return ItemStack.EMPTY
            }
        } else if (UpgradeSlot.isUpgrade(stack)) {
            if (!moveItemStackTo(
                    stack,
                    EggIncubatorBlockEntity.SLOT_UPGRADE_START,
                    EggIncubatorBlockEntity.SLOT_UPGRADE_END + 1,
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

    private fun findFilterStack(): ItemStack? {
        for (slot in EggIncubatorBlockEntity.SLOT_UPGRADE_START..EggIncubatorBlockEntity.SLOT_UPGRADE_END) {
            val stack = container.getItem(slot)
            if (stack.`is`(ModRegistries.FILTER_UPGRADE.get())) return stack
        }
        return null
    }

    private class EggSlot(container: Container, slot: Int, x: Int, y: Int) : Slot(container, slot, x, y) {
        override fun mayPlace(stack: ItemStack): Boolean = CobbreedingCompat.isEgg(stack)
    }

    private class OutputSlot(container: Container, slot: Int, x: Int, y: Int) : Slot(container, slot, x, y) {
        override fun mayPlace(stack: ItemStack): Boolean = false
    }

    private class UpgradeSlot(container: Container, slot: Int, x: Int, y: Int) : Slot(container, slot, x, y) {
        override fun mayPlace(stack: ItemStack): Boolean = isUpgrade(stack)

        companion object {
            fun isUpgrade(stack: ItemStack): Boolean {
                return stack.`is`(ModRegistries.SPEED_UPGRADE.get()) ||
                        stack.`is`(ModRegistries.PC_UPGRADE.get()) ||
                        stack.`is`(ModRegistries.FILTER_UPGRADE.get())
            }
        }
    }
}
