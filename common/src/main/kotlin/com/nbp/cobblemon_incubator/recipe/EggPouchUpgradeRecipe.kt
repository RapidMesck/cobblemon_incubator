package com.nbp.cobblemon_incubator.recipe

import com.nbp.cobblemon_incubator.registry.ModRegistries
import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.CustomRecipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.level.Level

class EggPouchUpgradeRecipe(category: CraftingBookCategory) : CustomRecipe(category) {

    override fun matches(input: CraftingInput, level: Level): Boolean {
        var hasNormalPouch = false
        var hasScanner = false
        var otherCount = 0

        for (item in input.items()) {
            if (item.isEmpty) continue
            if (item.`is`(ModRegistries.EGG_POUCH.get())) {
                if (hasNormalPouch) return false
                hasNormalPouch = true
            } else if (item.`is`(ModRegistries.BREEDING_SCANNER.get())) {
                if (hasScanner) return false
                hasScanner = true
            } else {
                otherCount++
            }
        }
        return hasNormalPouch && hasScanner && otherCount == 0
    }

    override fun assemble(input: CraftingInput, registries: HolderLookup.Provider): ItemStack {
        val sourcePouch = input.items().firstOrNull { it.`is`(ModRegistries.EGG_POUCH.get()) } ?: ItemStack.EMPTY
        val result = ItemStack(ModRegistries.ADVANCED_EGG_POUCH.get())

        if (!sourcePouch.isEmpty) {
            val uuid = sourcePouch.get(ModRegistries.POUCH_UUID.get())
            if (uuid != null) {
                result.set(ModRegistries.POUCH_UUID.get(), uuid)
            }
            val count = sourcePouch.get(ModRegistries.POUCH_EGG_COUNT.get())
            if (count != null) {
                result.set(ModRegistries.POUCH_EGG_COUNT.get(), count)
            }
            val customName = sourcePouch.get(DataComponents.CUSTOM_NAME)
            if (customName != null) {
                result.set(DataComponents.CUSTOM_NAME, customName)
            }
        }
        return result
    }

    override fun canCraftInDimensions(width: Int, height: Int): Boolean = width * height >= 2

    override fun isSpecial(): Boolean = false

    override fun showNotification(): Boolean = true

    override fun getResultItem(registries: HolderLookup.Provider): ItemStack =
        ItemStack(ModRegistries.ADVANCED_EGG_POUCH.get())

    override fun getIngredients(): net.minecraft.core.NonNullList<net.minecraft.world.item.crafting.Ingredient> =
        net.minecraft.core.NonNullList.of(
            net.minecraft.world.item.crafting.Ingredient.EMPTY,
            net.minecraft.world.item.crafting.Ingredient.of(ModRegistries.EGG_POUCH.get()),
            net.minecraft.world.item.crafting.Ingredient.of(ModRegistries.BREEDING_SCANNER.get())
        )

    override fun getSerializer(): RecipeSerializer<*> = ModRegistries.EGG_POUCH_UPGRADE_RECIPE_SERIALIZER.get()
}
