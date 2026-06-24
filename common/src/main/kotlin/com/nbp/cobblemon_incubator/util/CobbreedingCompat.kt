package com.nbp.cobblemon_incubator.util

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.events.pokemon.HatchEggEvent
import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.nbp.cobblemon_incubator.CobblemonIncubator
import net.minecraft.ChatFormatting
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import java.lang.reflect.Method
import java.util.UUID

object CobbreedingCompat {
    private val timerId = ResourceLocation.fromNamespaceAndPath("cobbreeding", "timer")
    private val nameId = ResourceLocation.fromNamespaceAndPath("cobbreeding", "name")

    private val extractPropertiesMethod: Method? by lazy {
        runCatching {
            Class.forName("ludichat.cobbreeding.EggUtilities")
                .methods
                .firstOrNull { method ->
                    method.name == "extractProperties" &&
                        method.parameterTypes.size == 1 &&
                        ItemStack::class.java.isAssignableFrom(method.parameterTypes[0])
                }
        }.getOrNull()
    }

    fun isEgg(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        return extractProperties(stack) != null || BuiltInRegistries.ITEM.getKey(stack.item).namespace == "cobbreeding"
    }

    fun extractProperties(stack: ItemStack): PokemonProperties? {
        if (stack.isEmpty) return null
        return runCatching {
            extractPropertiesMethod?.invoke(null, stack) as? PokemonProperties
        }.onFailure {
            CobblemonIncubator.logger.debug("Nao foi possivel ler propriedades do ovo Cobbreeding", it)
        }.getOrNull()
    }

    fun getEggName(stack: ItemStack): String? {
        val component = BuiltInRegistries.DATA_COMPONENT_TYPE.get(nameId)
        @Suppress("UNCHECKED_CAST")
        return component?.let { stack.get(it as net.minecraft.core.component.DataComponentType<String>) }
    }

    fun getTimer(stack: ItemStack): Int? {
        val component = BuiltInRegistries.DATA_COMPONENT_TYPE.get(timerId)
        @Suppress("UNCHECKED_CAST")
        return component?.let { stack.get(it as net.minecraft.core.component.DataComponentType<Int>) }
    }

    fun setTimer(stack: ItemStack, timer: Int) {
        val component = BuiltInRegistries.DATA_COMPONENT_TYPE.get(timerId) ?: return
        @Suppress("UNCHECKED_CAST")
        stack.set(component as net.minecraft.core.component.DataComponentType<Int>, timer.coerceAtLeast(0))
    }

    fun getSpeciesDisplayName(stack: ItemStack): Component {
        val properties = extractProperties(stack)
        val speciesId = properties?.species ?: return Component.literal("Pokemon")
        val species = PokemonSpecies.species.firstOrNull {
            it.resourceIdentifier.toString().equals(speciesId, ignoreCase = true) ||
                it.resourceIdentifier.path.equals(speciesId.substringAfter(':'), ignoreCase = true)
        }
        val name = species?.translatedName ?: Component.literal(displayName(speciesId))
        val tooltip = buildPokemonTooltip(properties)
        return name.copy().withStyle { style ->
            style
                .withColor(ChatFormatting.GREEN)
                .withUnderlined(true)
                .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip))
        }
    }

    private fun buildPokemonTooltip(properties: PokemonProperties): Component {
        val nature = displayName(properties.nature)
        val ability = displayName(properties.ability)
        val ivs = properties.ivs

        return Component.empty()
            .append(Component.translatable("tooltip.cobblemon_incubator.nature", nature))
            .append("\n")
            .append(Component.translatable("tooltip.cobblemon_incubator.ability", ability))
            .append("\n")
            .append(
                if (ivs == null) {
                    Component.translatable("tooltip.cobblemon_incubator.ivs_unknown")
                } else {
                    Component.translatable(
                        "tooltip.cobblemon_incubator.ivs",
                        ivs.getOrDefault(Stats.HP),
                        ivs.getOrDefault(Stats.ATTACK),
                        ivs.getOrDefault(Stats.DEFENCE),
                        ivs.getOrDefault(Stats.SPECIAL_ATTACK),
                        ivs.getOrDefault(Stats.SPECIAL_DEFENCE),
                        ivs.getOrDefault(Stats.SPEED)
                    )
                }
            )
            .withStyle(ChatFormatting.GRAY)
    }

    private fun displayName(value: String?): String {
        val clean = value?.substringAfter(':')?.substringAfterLast('/') ?: return "Any"
        return clean.split('-', '_', ' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }

    fun hatchToPc(player: ServerPlayer, ownerUuid: UUID, egg: ItemStack): Boolean {
        val properties = extractProperties(egg) ?: return false
        val species = properties.species ?: return false
        if (species == "random" || PokemonSpecies.getByName(species) == null) return false

        return runCatching {
            val pc = Cobblemon.storage.getPC(ownerUuid, player.registryAccess())
            if (pc.getFirstAvailablePosition() == null) {
                player.sendSystemMessage(Component.translatable("message.cobblemon_incubator.pc_full"))
                return@runCatching false
            }

            CobblemonEvents.HATCH_EGG_PRE.post(HatchEggEvent.Pre(properties, player))
            val pokemon = properties.create()
            pokemon.setFriendship(120)
            if (!pc.add(pokemon)) {
                player.sendSystemMessage(Component.translatable("message.cobblemon_incubator.pc_full"))
                return@runCatching false
            }
            Cobblemon.playerDataManager.getPokedexData(ownerUuid).obtain(pokemon)
            CobblemonEvents.HATCH_EGG_POST.post(HatchEggEvent.Post(player, pokemon))
            true
        }.onFailure {
            CobblemonIncubator.logger.error("Falha ao chocar ovo para o PC", it)
        }.getOrDefault(false)
    }

    fun summarize(stack: ItemStack): List<String> {
        val properties = extractProperties(stack) ?: return listOf("No egg data")
        val ivs = properties.ivs?.toString()
        return buildList {
            add("Species: ${properties.species ?: "Unknown"}")
            properties.form?.let { add("Form: $it") }
            properties.nature?.let { add("Nature: $it") }
            properties.ability?.let { add("Ability: $it") }
            properties.gender?.let { add("Gender: $it") }
            properties.shiny?.let { add("Shiny: $it") }
            if (ivs != null) add("IVs: $ivs")
            properties.moves?.takeIf { it.isNotEmpty() }?.let { add("Moves: ${it.joinToString()}") }
        }
    }
}
