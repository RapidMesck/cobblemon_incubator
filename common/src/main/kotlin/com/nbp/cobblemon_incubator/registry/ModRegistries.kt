package com.nbp.cobblemon_incubator.registry

import com.mojang.serialization.Codec
import com.nbp.cobblemon_incubator.CobblemonIncubator
import com.nbp.cobblemon_incubator.block.EggIncubatorBlock
import com.nbp.cobblemon_incubator.blockentity.EggIncubatorBlockEntity
import com.nbp.cobblemon_incubator.item.BreedingScannerItem
import com.nbp.cobblemon_incubator.item.FilterUpgradeItem
import com.nbp.cobblemon_incubator.item.PcUpgradeItem
import com.nbp.cobblemon_incubator.item.SpeedUpgradeItem
import com.nbp.cobblemon_incubator.menu.EggIncubatorMenu
import dev.architectury.registry.CreativeTabRegistry
import dev.architectury.registry.menu.MenuRegistry
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.Registries
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour

object ModRegistries {
    private val BLOCKS: DeferredRegister<Block> = DeferredRegister.create(CobblemonIncubator.MOD_ID, Registries.BLOCK)
    private val ITEMS: DeferredRegister<Item> = DeferredRegister.create(CobblemonIncubator.MOD_ID, Registries.ITEM)
    private val BLOCK_ENTITY_TYPES: DeferredRegister<BlockEntityType<*>> =
        DeferredRegister.create(CobblemonIncubator.MOD_ID, Registries.BLOCK_ENTITY_TYPE)
    private val MENU_TYPES: DeferredRegister<MenuType<*>> =
        DeferredRegister.create(CobblemonIncubator.MOD_ID, Registries.MENU)
    private val CREATIVE_MODE_TABS: DeferredRegister<CreativeModeTab> =
        DeferredRegister.create(CobblemonIncubator.MOD_ID, Registries.CREATIVE_MODE_TAB)
    private val DATA_COMPONENT_TYPES: DeferredRegister<DataComponentType<*>> =
        DeferredRegister.create(CobblemonIncubator.MOD_ID, Registries.DATA_COMPONENT_TYPE)

    val EGG_INCUBATOR: RegistrySupplier<EggIncubatorBlock> = BLOCKS.register("egg_incubator") {
        EggIncubatorBlock(
            BlockBehaviour.Properties.of()
                .strength(3.5f, 6.0f)
                .sound(SoundType.COPPER)
                .requiresCorrectToolForDrops()
                .noOcclusion()
        )
    }

    val EGG_INCUBATOR_ITEM: RegistrySupplier<Item> = ITEMS.register("egg_incubator") {
        BlockItem(EGG_INCUBATOR.get(), Item.Properties())
    }

    val SPEED_UPGRADE: RegistrySupplier<Item> = ITEMS.register("speed_upgrade") {
        SpeedUpgradeItem(Item.Properties().stacksTo(16))
    }

    val PC_UPGRADE: RegistrySupplier<Item> = ITEMS.register("pc_upgrade") {
        PcUpgradeItem(Item.Properties().stacksTo(1))
    }

    val FILTER_UPGRADE: RegistrySupplier<Item> = ITEMS.register("filter_upgrade") {
        FilterUpgradeItem(Item.Properties().stacksTo(1))
    }

    val BREEDING_SCANNER: RegistrySupplier<Item> = ITEMS.register("breeding_scanner") {
        BreedingScannerItem(Item.Properties().stacksTo(1))
    }

    val CREATIVE_TAB: RegistrySupplier<CreativeModeTab> = CREATIVE_MODE_TABS.register("main") {
        CreativeTabRegistry.create { builder ->
            builder
                .title(net.minecraft.network.chat.Component.translatable("itemGroup.cobblemon_incubator.main"))
                .icon { ItemStack(EGG_INCUBATOR_ITEM.get()) }
                .displayItems { _, output ->
                    output.accept(EGG_INCUBATOR_ITEM.get())
                    output.accept(SPEED_UPGRADE.get())
                    output.accept(PC_UPGRADE.get())
                    output.accept(FILTER_UPGRADE.get())
                    output.accept(BREEDING_SCANNER.get())
                }
        }
    }

    val PC_UPGRADE_OWNER_UUID: RegistrySupplier<DataComponentType<String>> =
        DATA_COMPONENT_TYPES.register("pc_upgrade_owner_uuid") {
            DataComponentType.builder<String>().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8)
                .build()
        }

    val PC_UPGRADE_OWNER_NAME: RegistrySupplier<DataComponentType<String>> =
        DATA_COMPONENT_TYPES.register("pc_upgrade_owner_name") {
            DataComponentType.builder<String>().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8)
                .build()
        }

    val FILTER_CONFIG: RegistrySupplier<DataComponentType<String>> = DATA_COMPONENT_TYPES.register("filter_config") {
        DataComponentType.builder<String>().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8)
            .build()
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val EGG_INCUBATOR_BLOCK_ENTITY: RegistrySupplier<BlockEntityType<EggIncubatorBlockEntity>> =
        BLOCK_ENTITY_TYPES.register("egg_incubator") {
            BlockEntityType.Builder.of(::EggIncubatorBlockEntity, EGG_INCUBATOR.get()).build(null)
        }

    val EGG_INCUBATOR_MENU: RegistrySupplier<MenuType<EggIncubatorMenu>> = MENU_TYPES.register("egg_incubator") {
        MenuRegistry.ofExtended(::EggIncubatorMenu)
    }

    fun register() {
        DATA_COMPONENT_TYPES.register()
        BLOCKS.register()
        ITEMS.register()
        CREATIVE_MODE_TABS.register()
        BLOCK_ENTITY_TYPES.register()
        MENU_TYPES.register()
    }
}
