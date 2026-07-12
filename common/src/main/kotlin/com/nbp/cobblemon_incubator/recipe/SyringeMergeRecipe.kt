package com.nbp.cobblemon_incubator.recipe

import com.nbp.cobblemon_incubator.item.StemCellSyringeItem
import com.nbp.cobblemon_incubator.registry.ModRegistries
import net.minecraft.core.HolderLookup
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.CustomRecipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.level.Level

class SyringeMergeRecipe(category: CraftingBookCategory) : CustomRecipe(category) {

    override fun matches(input: CraftingInput, level: Level): Boolean {
        val syringes = input.items().filter { it.`is`(ModRegistries.STEM_CELL_SYRINGE.get()) }
        val nonEmpty = input.items().count { !it.isEmpty }
        return syringes.size == 2 && nonEmpty == 2
    }

    override fun assemble(input: CraftingInput, registries: HolderLookup.Provider): ItemStack {
        val syringes = input.items().filter { it.`is`(ModRegistries.STEM_CELL_SYRINGE.get()) }
        val result = ItemStack(ModRegistries.STEM_CELL_SYRINGE.get())

        val combined = mutableMapOf<String, Int>()
        for (syringe in syringes) {
            for ((type, amount) in StemCellSyringeItem.getCharges(syringe)) {
                val current = combined[type] ?: 0
                val max = StemCellSyringeItem.getMaxPerType()
                combined[type] = minOf(current + amount, max)
            }
        }
        if (combined.isNotEmpty()) {
            StemCellSyringeItem.setCharges(result, combined)
        }
        return result
    }

    override fun canCraftInDimensions(width: Int, height: Int): Boolean = width * height >= 2

    override fun getSerializer(): RecipeSerializer<*> = ModRegistries.SYRINGE_MERGE_RECIPE_SERIALIZER.get()
}
