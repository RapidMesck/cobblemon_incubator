package com.nbp.cobblemon_incubator.storage

import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.core.component.DataComponents
import net.minecraft.server.MinecraftServer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.saveddata.SavedData
import java.util.UUID

class EggPouchSavedData : SavedData() {
    private val pouches: MutableMap<UUID, MutableList<ItemStack>> = mutableMapOf()

    fun getEggs(uuid: UUID): MutableList<ItemStack> {
        return pouches.getOrPut(uuid) { mutableListOf() }
    }

    fun addEgg(uuid: UUID, egg: ItemStack) {
        if (egg.isEmpty) return
        val list = getEggs(uuid)
        list.add(cleanForComparison(egg))
        setDirty()
    }

    fun removeEgg(uuid: UUID, index: Int): ItemStack {
        val list = getEggs(uuid)
        if (index !in list.indices) return ItemStack.EMPTY
        val removed = list.removeAt(index)
        setDirty()
        return removed
    }

    fun removeEgg(uuid: UUID, egg: ItemStack): Boolean {
        val list = getEggs(uuid)
        val cleanTarget = cleanForComparison(egg)
        val iterator = list.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (ItemStack.matches(cleanForComparison(item), cleanTarget)) {
                iterator.remove()
                setDirty()
                return true
            }
        }
        return false
    }

    fun cleanForComparison(stack: ItemStack): ItemStack {
        if (stack.isEmpty) return ItemStack.EMPTY
        val copy = stack.copy()
        val customData = copy.get(DataComponents.CUSTOM_DATA) ?: return copy
        val tag = customData.copyTag()
        if (tag.contains("PouchEggProps") || tag.contains("PouchEggTimer") || tag.contains("PouchEggGender")) {
            tag.remove("PouchEggProps")
            tag.remove("PouchEggTimer")
            tag.remove("PouchEggGender")
            if (tag.isEmpty) {
                copy.remove(DataComponents.CUSTOM_DATA)
            } else {
                copy.set(DataComponents.CUSTOM_DATA, CustomData.of(tag))
            }
        }
        return copy
    }

    fun getEggCount(uuid: UUID): Int {
        return pouches[uuid]?.size ?: 0
    }

    override fun save(tag: CompoundTag, registries: HolderLookup.Provider): CompoundTag {
        val pouchesListTag = ListTag()
        for ((uuid, items) in pouches) {
            val pouchTag = CompoundTag()
            pouchTag.putUUID("uuid", uuid)
            val itemsTag = ListTag()
            for (stack in items) {
                if (!stack.isEmpty) {
                    val saved = cleanForComparison(stack).saveOptional(registries)
                    itemsTag.add(saved)
                }
            }
            pouchTag.put("items", itemsTag)
            pouchesListTag.add(pouchTag)
        }
        tag.put("pouches", pouchesListTag)
        return tag
    }

    companion object {
        private const val DATA_NAME = "cobblemon_incubator_egg_pouches"

        fun load(tag: CompoundTag, registries: HolderLookup.Provider): EggPouchSavedData {
            val data = EggPouchSavedData()
            val pouchesListTag = tag.getList("pouches", Tag.TAG_COMPOUND.toInt())
            for (i in 0 until pouchesListTag.size) {
                val pouchTag = pouchesListTag.getCompound(i)
                if (pouchTag.hasUUID("uuid")) {
                    val uuid = pouchTag.getUUID("uuid")
                    val itemsTag = pouchTag.getList("items", Tag.TAG_COMPOUND.toInt())
                    val list = mutableListOf<ItemStack>()
                    for (j in 0 until itemsTag.size) {
                        val itemCompound = itemsTag.getCompound(j)
                        val stack = ItemStack.parseOptional(registries, itemCompound)
                        if (!stack.isEmpty) {
                            list.add(stack)
                        }
                    }
                    data.pouches[uuid] = list
                }
            }
            return data
        }

        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        fun get(server: MinecraftServer): EggPouchSavedData {
            val dataStorage = server.overworld().dataStorage
            return dataStorage.computeIfAbsent(
                Factory(::EggPouchSavedData, ::load, null),
                DATA_NAME
            )
        }
    }
}
