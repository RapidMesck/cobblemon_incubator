package com.nbp.cobblemon_incubator.menu

import com.nbp.cobblemon_incubator.item.EggPouchItem
import com.nbp.cobblemon_incubator.registry.ModRegistries
import com.nbp.cobblemon_incubator.storage.EggPouchSavedData
import com.nbp.cobblemon_incubator.util.CobbreedingCompat
import com.nbp.cobblemon_incubator.util.EggPouchSearchFilter
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.Container
import net.minecraft.world.InteractionHand
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import java.util.UUID

class EggPouchMenu : AbstractContainerMenu {
    companion object {
        const val POUCH_SLOTS = 54
        const val SLOTS_PER_ROW = 9
        const val VISIBLE_ROWS = 6
        const val BUTTON_DEPOSIT_ALL = 1
    }

    val pouchUuid: UUID
    val isAdvanced: Boolean
    val hand: InteractionHand
    private val playerInventory: Inventory
    private val visibleContainer: Container
    val dataAccess: ContainerData

    var searchQuery: String = ""
        private set
    var scrollRow: Int = 0
        private set

    // Client-side constructor
    constructor(containerId: Int, playerInventory: Inventory, buffer: FriendlyByteBuf) : this(
        containerId,
        playerInventory,
        buffer.readUUID(),
        buffer.readBoolean(),
        buffer.readEnum(InteractionHand::class.java),
        SimpleContainer(POUCH_SLOTS),
        SimpleContainerData(3)
    )

    // Server-side constructor
    constructor(
        containerId: Int,
        playerInventory: Inventory,
        pouchUuid: UUID,
        isAdvanced: Boolean,
        hand: InteractionHand
    ) : this(
        containerId,
        playerInventory,
        pouchUuid,
        isAdvanced,
        hand,
        SimpleContainer(POUCH_SLOTS),
        SimpleContainerData(3)
    )

    private constructor(
        containerId: Int,
        playerInventory: Inventory,
        pouchUuid: UUID,
        isAdvanced: Boolean,
        hand: InteractionHand,
        visibleContainer: Container,
        dataAccess: ContainerData
    ) : super(ModRegistries.EGG_POUCH_MENU.get(), containerId) {
        this.playerInventory = playerInventory
        this.pouchUuid = pouchUuid
        this.isAdvanced = isAdvanced
        this.hand = hand
        this.visibleContainer = visibleContainer
        this.dataAccess = dataAccess

        // 54 Pouch slots (9x6 grid)
        for (row in 0 until VISIBLE_ROWS) {
            for (col in 0 until SLOTS_PER_ROW) {
                val index = col + row * SLOTS_PER_ROW
                addSlot(PouchSlot(visibleContainer, index, 8 + col * 18, 18 + row * 18))
            }
        }

        // Player inventory (3 rows x 9)
        val playerInvY = 140
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, playerInvY + row * 18))
            }
        }

        // Player hotbar (1 row x 9)
        val hotbarY = 198
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, hotbarY))
        }

        addDataSlots(dataAccess)

        if (playerInventory.player is ServerPlayer) {
            refreshVisibleSlots()
        }
    }

    val filteredCount: Int
        get() = dataAccess.get(0)

    val currentScrollRow: Int
        get() = dataAccess.get(1)

    val totalEggsCount: Int
        get() = dataAccess.get(2)

    val maxScrollRows: Int
        get() = maxOf(0, (filteredCount + SLOTS_PER_ROW - 1) / SLOTS_PER_ROW - VISIBLE_ROWS)

    override fun stillValid(player: Player): Boolean {
        val held = player.getItemInHand(hand)
        if (held.item is EggPouchItem && EggPouchItem.getPouchUuid(held) == pouchUuid) {
            return true
        }
        for (stack in player.inventory.items) {
            if (stack.item is EggPouchItem && EggPouchItem.getPouchUuid(stack) == pouchUuid) {
                return true
            }
        }
        return false
    }

    fun updateFilter(row: Int, query: String) {
        this.searchQuery = query
        this.scrollRow = maxOf(0, row)
        refreshVisibleSlots()
        broadcastChanges()
    }

    fun refreshVisibleSlots() {
        val player = playerInventory.player as? ServerPlayer ?: return
        val serverData = EggPouchSavedData.get(player.server)
        val allEggs = serverData.getEggs(pouchUuid)

        val filteredEggs = allEggs.filter { EggPouchSearchFilter.matches(it, searchQuery, isAdvanced) }
        val maxRow = maxOf(0, (filteredEggs.size + SLOTS_PER_ROW - 1) / SLOTS_PER_ROW - VISIBLE_ROWS)
        scrollRow = scrollRow.coerceIn(0, maxRow)

        dataAccess.set(0, filteredEggs.size)
        dataAccess.set(1, scrollRow)
        dataAccess.set(2, allEggs.size)

        val startIndex = scrollRow * SLOTS_PER_ROW
        for (i in 0 until POUCH_SLOTS) {
            val eggIndex = startIndex + i
            val item = if (eggIndex in filteredEggs.indices) filteredEggs[eggIndex].copy() else ItemStack.EMPTY
            visibleContainer.setItem(i, item)
        }
    }

    private fun updateHeldPouchCount(player: ServerPlayer, count: Int) {
        val held = player.getItemInHand(hand)
        if (held.item is EggPouchItem && EggPouchItem.getPouchUuid(held) == pouchUuid) {
            EggPouchItem.setEggCount(held, count)
        }
        for (stack in player.inventory.items) {
            if (stack.item is EggPouchItem && EggPouchItem.getPouchUuid(stack) == pouchUuid) {
                EggPouchItem.setEggCount(stack, count)
            }
        }
    }

    override fun clicked(slotId: Int, button: Int, clickType: ClickType, player: Player) {
        // Prevent moving or picking up the active pouch itself
        if (slotId in POUCH_SLOTS until slots.size) {
            val clickedSlot = slots[slotId]
            val clickedItem = clickedSlot.item
            if (clickedItem.item is EggPouchItem && EggPouchItem.getPouchUuid(clickedItem) == pouchUuid) {
                return
            }
        }

        if (slotId in 0 until POUCH_SLOTS && player is ServerPlayer) {
            val oldItem = visibleContainer.getItem(slotId).copy()
            val serverData = EggPouchSavedData.get(player.server)

            if (clickType == ClickType.PICKUP) {
                val carried = carried.copy()
                if (carried.isEmpty && !oldItem.isEmpty) {
                    // Pick up egg from pouch to cursor
                    super.clicked(slotId, button, clickType, player)
                    serverData.removeEgg(pouchUuid, oldItem)
                    updateHeldPouchCount(player, serverData.getEggCount(pouchUuid))
                    refreshVisibleSlots()
                    return
                } else if (!carried.isEmpty && oldItem.isEmpty) {
                    // Place egg from cursor into pouch
                    if (CobbreedingCompat.isEgg(carried) && carried.item !is EggPouchItem) {
                        super.clicked(slotId, button, clickType, player)
                        val placed = visibleContainer.getItem(slotId)
                        serverData.addEgg(pouchUuid, placed.copy())
                        updateHeldPouchCount(player, serverData.getEggCount(pouchUuid))
                        refreshVisibleSlots()
                    }
                    return
                } else if (!carried.isEmpty && !oldItem.isEmpty) {
                    // Swap egg
                    if (CobbreedingCompat.isEgg(carried) && carried.item !is EggPouchItem) {
                        super.clicked(slotId, button, clickType, player)
                        val placed = visibleContainer.getItem(slotId)
                        serverData.removeEgg(pouchUuid, oldItem)
                        serverData.addEgg(pouchUuid, placed.copy())
                        updateHeldPouchCount(player, serverData.getEggCount(pouchUuid))
                        refreshVisibleSlots()
                    }
                    return
                }
            } else if (clickType == ClickType.THROW) {
                if (!oldItem.isEmpty) {
                    super.clicked(slotId, button, clickType, player)
                    serverData.removeEgg(pouchUuid, oldItem)
                    updateHeldPouchCount(player, serverData.getEggCount(pouchUuid))
                    refreshVisibleSlots()
                    return
                }
            }
        }

        super.clicked(slotId, button, clickType, player)
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        val slot = slots[index]
        if (!slot.hasItem()) return ItemStack.EMPTY
        val stack = slot.item
        val copy = stack.copy()

        if (index in 0 until POUCH_SLOTS) {
            // Transfer egg from pouch to player inventory
            if (!moveItemStackTo(stack, POUCH_SLOTS, slots.size, true)) {
                return ItemStack.EMPTY
            }
            if (stack.isEmpty) {
                slot.setByPlayer(ItemStack.EMPTY)
            } else {
                slot.setChanged()
            }
            if (player is ServerPlayer) {
                val serverData = EggPouchSavedData.get(player.server)
                serverData.removeEgg(pouchUuid, copy)
                updateHeldPouchCount(player, serverData.getEggCount(pouchUuid))
                refreshVisibleSlots()
            }
            return copy
        } else {
            // Transfer egg from player inventory to pouch
            if (!CobbreedingCompat.isEgg(stack) || stack.item is EggPouchItem) {
                return ItemStack.EMPTY
            }
            // CRITICAL: Empty the slot on both client and server so vanilla's while-loop terminates!
            slot.setByPlayer(ItemStack.EMPTY)
            if (player is ServerPlayer) {
                val serverData = EggPouchSavedData.get(player.server)
                if (copy.count > 1) {
                    for (i in 0 until copy.count) {
                        val single = copy.copy().apply { count = 1 }
                        serverData.addEgg(pouchUuid, single)
                    }
                } else {
                    serverData.addEgg(pouchUuid, copy)
                }
                updateHeldPouchCount(player, serverData.getEggCount(pouchUuid))
                refreshVisibleSlots()
            }
            return copy
        }
    }

    override fun clickMenuButton(player: Player, id: Int): Boolean {
        if (id == BUTTON_DEPOSIT_ALL) {
            if (player !is ServerPlayer) return true
            val serverData = EggPouchSavedData.get(player.server)
            var deposited = 0
            for (slotIndex in POUCH_SLOTS until slots.size) {
                val slot = slots[slotIndex]
                val stack = slot.item
                if (!stack.isEmpty && CobbreedingCompat.isEgg(stack) && stack.item !is EggPouchItem) {
                    serverData.addEgg(pouchUuid, stack.copy())
                    slot.setByPlayer(ItemStack.EMPTY)
                    deposited++
                }
            }
            if (deposited > 0) {
                updateHeldPouchCount(player, serverData.getEggCount(pouchUuid))
                player.level().playSound(null, player.blockPosition(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 0.8f, 1.0f)
                refreshVisibleSlots()
                broadcastChanges()
            }
            return true
        }
        return false
    }

    override fun removed(player: Player) {
        super.removed(player)
        if (player is ServerPlayer) {
            val serverData = EggPouchSavedData.get(player.server)
            updateHeldPouchCount(player, serverData.getEggCount(pouchUuid))
        }
    }

    class PouchSlot(container: Container, index: Int, x: Int, y: Int) : Slot(container, index, x, y) {
        override fun mayPlace(stack: ItemStack): Boolean {
            return CobbreedingCompat.isEgg(stack) && stack.item !is EggPouchItem
        }
    }
}
