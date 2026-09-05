package com.nbp.cobblemon_incubator.item

import com.nbp.cobblemon_incubator.menu.EggPouchMenu
import com.nbp.cobblemon_incubator.registry.ModRegistries
import com.nbp.cobblemon_incubator.storage.EggPouchSavedData
import com.nbp.cobblemon_incubator.util.CobbreedingCompat
import dev.architectury.registry.menu.ExtendedMenuProvider
import dev.architectury.registry.menu.MenuRegistry
import net.minecraft.ChatFormatting
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.SlotAccess
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ClickAction
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import java.util.UUID

open class EggPouchItem(
    properties: Properties,
    val isAdvanced: Boolean = false
) : Item(properties) {

    companion object {
        fun getPouchUuid(stack: ItemStack): UUID? {
            val raw = stack.get(ModRegistries.POUCH_UUID.get()) ?: return null
            return runCatching { UUID.fromString(raw) }.getOrNull()
        }

        fun getOrCreatePouchUuid(stack: ItemStack): UUID {
            getPouchUuid(stack)?.let { return it }
            val uuid = UUID.randomUUID()
            stack.set(ModRegistries.POUCH_UUID.get(), uuid.toString())
            return uuid
        }

        fun getEggCount(stack: ItemStack): Int {
            return stack.get(ModRegistries.POUCH_EGG_COUNT.get()) ?: 0
        }

        fun setEggCount(stack: ItemStack, count: Int) {
            stack.set(ModRegistries.POUCH_EGG_COUNT.get(), count)
        }
    }

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true)
        }
        if (player !is ServerPlayer) {
            return InteractionResultHolder.pass(stack)
        }

        val uuid = getOrCreatePouchUuid(stack)
        val data = EggPouchSavedData.get(player.server)
        setEggCount(stack, data.getEggCount(uuid))

        MenuRegistry.openExtendedMenu(player, object : ExtendedMenuProvider {
            override fun createMenu(containerId: Int, playerInventory: Inventory, p: Player): AbstractContainerMenu {
                return EggPouchMenu(containerId, playerInventory, uuid, isAdvanced, hand)
            }

            override fun getDisplayName(): Component = stack.hoverName

            override fun saveExtraData(buffer: FriendlyByteBuf) {
                buffer.writeUUID(uuid)
                buffer.writeBoolean(isAdvanced)
                buffer.writeEnum(hand)
            }
        })

        return InteractionResultHolder.sidedSuccess(stack, false)
    }

    // Drop an egg onto the pouch in inventory
    override fun overrideOtherStackedOnMe(
        stack: ItemStack,
        other: ItemStack,
        slot: Slot,
        action: ClickAction,
        player: Player,
        access: SlotAccess
    ): Boolean {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player)) return false
        if (other.isEmpty) return false
        if (!CobbreedingCompat.isEgg(other)) return false

        if (player is ServerPlayer) {
            val uuid = getOrCreatePouchUuid(stack)
            val data = EggPouchSavedData.get(player.server)
            val eggToStore = other.split(1)
            access.set(other)
            data.addEgg(uuid, eggToStore)
            setEggCount(stack, data.getEggCount(uuid))
            slot.setChanged()
            player.level().playSound(null, player.blockPosition(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 0.8f, 1.0f)
        }
        return true
    }

    // Pick up the pouch and right click on an egg in inventory
    override fun overrideStackedOnOther(
        stack: ItemStack,
        slot: Slot,
        action: ClickAction,
        player: Player
    ): Boolean {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player)) return false
        val target = slot.item
        if (target.isEmpty || !CobbreedingCompat.isEgg(target)) return false

        if (player is ServerPlayer) {
            val uuid = getOrCreatePouchUuid(stack)
            val data = EggPouchSavedData.get(player.server)
            val eggToStore = slot.safeTake(1, 1, player)
            if (!eggToStore.isEmpty) {
                data.addEgg(uuid, eggToStore)
                setEggCount(stack, data.getEggCount(uuid))
                slot.setChanged()
                player.level().playSound(null, player.blockPosition(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 0.8f, 1.0f)
            }
        }
        return true
    }

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
        val count = getEggCount(stack)
        tooltipComponents.add(
            Component.translatable("item.cobblemon_incubator.egg_pouch.count", count)
                .withStyle(ChatFormatting.GRAY)
        )
        if (isAdvanced) {
            tooltipComponents.add(
                Component.translatable("item.cobblemon_incubator.advanced_egg_pouch.scanner_badge")
                    .withStyle(ChatFormatting.GOLD)
            )
        }
    }
}
