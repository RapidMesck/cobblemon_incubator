package com.nbp.cobblemon_incubator.item

import com.nbp.cobblemon_incubator.config.IncubatorConfig
import com.nbp.cobblemon_incubator.registry.ModRegistries
import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag

class StemCellSyringeItem(properties: Properties) : Item(properties) {

    companion object {
        private const val SEPARATOR = ";"
        private const val KV_SEPARATOR = ":"

        fun getMaxPerType(): Int = IncubatorConfig.geneFusionSyringeMaxCharge

        fun getCharges(stack: ItemStack): Map<String, Int> {
            return decode(stack.getOrDefault(ModRegistries.SYRINGE_CHARGE.get(), ""))
        }

        fun setCharges(stack: ItemStack, charges: Map<String, Int>) {
            stack.set(ModRegistries.SYRINGE_CHARGE.get(), encode(charges))
            updateCustomModelData(stack, charges)
        }

        fun getCharge(stack: ItemStack, type: String): Int {
            return getCharges(stack)[type] ?: 0
        }

        fun getTotalCharge(stack: ItemStack): Int {
            return getCharges(stack).values.sum()
        }

        fun addCharge(stack: ItemStack, type: String, amount: Int): Int {
            val charges = getCharges(stack).toMutableMap()
            val current = charges[type] ?: 0
            val max = getMaxPerType()
            val added = amount.coerceAtMost(max - current)
            if (added <= 0) return 0
            charges[type] = current + added
            setCharges(stack, charges)
            return added
        }

        fun consumeCharge(stack: ItemStack, types: List<String>, amount: Int): Boolean {
            val charges = getCharges(stack).toMutableMap()
            for (type in types) {
                val current = charges[type] ?: 0
                if (current < amount) return false
            }
            for (type in types) {
                charges[type] = (charges[type] ?: 0) - amount
            }
            setCharges(stack, charges.filterValues { it > 0 })
            return true
        }

        fun getChargeForTypes(stack: ItemStack, types: List<String>): Int {
            return types.minOfOrNull { getCharge(stack, it) } ?: 0
        }

        private fun encode(charges: Map<String, Int>): String {
            return charges.entries.joinToString(SEPARATOR) { "${it.key}$KV_SEPARATOR${it.value}" }
        }

        private fun decode(encoded: String): Map<String, Int> {
            if (encoded.isBlank()) return emptyMap()
            return encoded.split(SEPARATOR).mapNotNull { part ->
                val parts = part.split(KV_SEPARATOR, limit = 2)
                if (parts.size == 2) {
                    val value = parts[1].toIntOrNull() ?: return@mapNotNull null
                    parts[0] to value
                } else null
            }.toMap()
        }

        private fun updateCustomModelData(stack: ItemStack, charges: Map<String, Int>) {
            if (charges.isEmpty()) {
                stack.remove(DataComponents.CUSTOM_MODEL_DATA)
                return
            }
            val max = getMaxPerType()
            val avgPercent = charges.values.map { it.toFloat() / max }.average().toFloat()
            val level = when {
                avgPercent >= 0.66f -> 3
                avgPercent >= 0.33f -> 2
                else -> 1
            }
            stack.set(DataComponents.CUSTOM_MODEL_DATA, net.minecraft.world.item.component.CustomModelData(level))
        }

        fun displayName(type: String): String {
            val key = type.substringAfterLast(':').substringAfterLast('/')
            return key.replaceFirstChar { it.uppercase() }
        }
    }

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
        val charges = getCharges(stack)

        if (charges.isEmpty()) {
            tooltipComponents.add(
                Component.translatable("item.cobblemon_incubator.stem_cell_syringe.empty")
                    .withStyle(ChatFormatting.GRAY)
            )
        } else {
            val max = getMaxPerType()
            val sorted = charges.entries.sortedByDescending { it.value }
            for ((type, charge) in sorted) {
                val color = when {
                    charge >= max -> ChatFormatting.GREEN
                    charge >= max * 0.4 -> ChatFormatting.YELLOW
                    else -> ChatFormatting.RED
                }
                tooltipComponents.add(
                    Component.literal("  ${displayName(type)}: $charge / $max").withStyle(color)
                )
            }
        }

        tooltipComponents.add(
            Component.translatable("item.cobblemon_incubator.stem_cell_syringe.description")
                .withStyle(ChatFormatting.GRAY)
        )
    }

    override fun isBarVisible(stack: ItemStack): Boolean {
        return getCharges(stack).isNotEmpty()
    }

    override fun getBarWidth(stack: ItemStack): Int {
        val total = getTotalCharge(stack)
        val maxTotal = getMaxPerType() * getCharges(stack).size.coerceAtLeast(1)
        if (maxTotal <= 0) return 0
        return (13.0f * total / maxTotal).toInt().coerceIn(0, 13)
    }

    override fun getBarColor(stack: ItemStack): Int {
        val charges = getCharges(stack)
        if (charges.isEmpty()) return 0xFF5555
        val max = getMaxPerType()
        val avgPercent = charges.values.map { it.toFloat() / max }.average().toFloat()
        return when {
            avgPercent >= 0.8f -> 0x55FF55
            avgPercent >= 0.4f -> 0xFFFF55
            else -> 0xFF5555
        }
    }
}
