package com.nbp.cobblemon_incubator.util

import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.pokeball.PokeBalls
import com.cobblemon.mod.common.api.pokemon.Natures
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.api.pokemon.egg.EggGroup
import com.cobblemon.mod.common.api.pokemon.stats.Stat
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.pokemon.FormData
import com.cobblemon.mod.common.pokemon.Pokemon
import com.nbp.cobblemon_incubator.CobblemonIncubator
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.world.item.Item
import java.lang.reflect.Method

object CobbreedingPreviewCompat {
    private val statOrder = listOf(
        Stats.HP,
        Stats.ATTACK,
        Stats.DEFENCE,
        Stats.SPECIAL_ATTACK,
        Stats.SPECIAL_DEFENCE,
        Stats.SPEED
    )

    private val statLabels = mapOf(
        Stats.HP to "HP",
        Stats.ATTACK to "Atk",
        Stats.DEFENCE to "Def",
        Stats.SPECIAL_ATTACK to "SpA",
        Stats.SPECIAL_DEFENCE to "SpD",
        Stats.SPEED to "Spe"
    )

    private val getPossibleEggsMethod: Method? by lazy {
        runCatching {
            Class.forName("ludichat.cobbreeding.BreedingUtilities")
                .methods
                .firstOrNull { method ->
                    method.name == "getPossibleEggs" &&
                        method.parameterTypes.size == 1 &&
                        List::class.java.isAssignableFrom(method.parameterTypes[0])
                }
        }.onFailure {
            CobblemonIncubator.logger.debug("Cobbreeding BreedingUtilities not available", it)
        }.getOrNull()
    }

    private val cobbreedingConfig: Any?
        get() = runCatching {
            val cobbreeding = Class.forName("ludichat.cobbreeding.Cobbreeding")
            val instance = cobbreeding.getField("INSTANCE").get(null)
            cobbreeding.getMethod("getConfig").invoke(instance)
        }.getOrNull()

    val available: Boolean
        get() = getPossibleEggsMethod != null

    fun buildReport(pokemon: List<Pokemon>): List<Component> {
        val previews = possibleEggs(pokemon)
        if (!available) {
            return listOf(
                Component.translatable("message.cobblemon_incubator.breeding_scanner.no_cobbreeding")
                    .withStyle(ChatFormatting.RED)
            )
        }
        if (pokemon.size < 2) {
            return listOf(
                Component.translatable("message.cobblemon_incubator.breeding_scanner.not_enough_pokemon")
                    .withStyle(ChatFormatting.YELLOW)
            )
        }
        if (previews.isEmpty()) {
            return listOf(
                Component.translatable("message.cobblemon_incubator.breeding_scanner.none")
                    .withStyle(ChatFormatting.YELLOW)
            )
        }

        return buildList {
            add(
                Component.translatable(
                    "message.cobblemon_incubator.breeding_scanner.header",
                    previews.size,
                    previews.sumOf { it.parents.size }
                ).withStyle(ChatFormatting.GOLD)
            )
            previews.sortedWith(compareBy({ it.form.species.nationalPokedexNumber }, { it.form.formOnlyShowdownId() }))
                .forEach { preview ->
                    add(
                        Component.translatable(
                            "message.cobblemon_incubator.breeding_scanner.egg",
                            eggName(preview.form),
                            preview.parents.size
                        ).withStyle(ChatFormatting.AQUA)
                    )
                    preview.parents.forEachIndexed { index, parents ->
                        add(Component.literal("  ${index + 1}. ${parentNames(parents)}").withStyle(ChatFormatting.WHITE))
                        ivSummary(parents).forEach { line ->
                            add(Component.literal("     ").append(line))
                        }
                        add(Component.literal("     ").append(natureSummary(parents)))
                        add(Component.literal("     ").append(abilitySummary(preview.form, parents)))
                        add(Component.literal("     ").append(ballSummary(parents)))
                        add(Component.literal("     ").append(movesSummary(preview.form, parents)))
                    }
                }
        }
    }

    private fun possibleEggs(pokemon: List<Pokemon>): List<EggPreview> {
        val method = getPossibleEggsMethod ?: return emptyList()
        val entries = runCatching { method.invoke(null, pokemon) as? Collection<*> }
            .onFailure { CobblemonIncubator.logger.warn("Failed to query Cobbreeding possible eggs", it) }
            .getOrNull()
            ?: return emptyList()

        return entries.mapNotNull { entry ->
            val mapEntry = entry as? Map.Entry<*, *> ?: return@mapNotNull null
            val form = mapEntry.key as? FormData ?: return@mapNotNull null
            val parents = (mapEntry.value as? List<*>)?.mapNotNull { pair ->
                val kotlinPair = pair as? Pair<*, *> ?: return@mapNotNull null
                val father = kotlinPair.first as? Pokemon ?: return@mapNotNull null
                val mother = kotlinPair.second as? Pokemon ?: return@mapNotNull null
                father to mother
            } ?: return@mapNotNull null
            EggPreview(form, parents)
        }
    }

    private fun ivSummary(parents: Pair<Pokemon, Pokemon>): List<Component> {
        val (father, mother) = parents
        val inheritedCount = if (father.heldItem().item == CobblemonItems.DESTINY_KNOT ||
            mother.heldItem().item == CobblemonItems.DESTINY_KNOT
        ) {
            5
        } else {
            3
        }

        val powerItems = listOfNotNull(
            powerItemToIV(father.heldItem().item)?.let { "F ${statLabel(it)}" },
            powerItemToIV(mother.heldItem().item)?.let { "M ${statLabel(it)}" }
        )
        val forced = if (powerItems.isEmpty()) {
            "first random"
        } else {
            "first from ${powerItems.joinToString(" or ")}"
        }
        val parentValues = statOrder.map { stat ->
            "${statLabel(stat)} ${father.ivs[stat] ?: 0}/${mother.ivs[stat] ?: 0}"
        }
        return listOf(
            Component.empty()
                .append(term("IVs", "tooltip.cobblemon_incubator.breeding_scanner.ivs"))
                .append(gray(": $inheritedCount "))
                .append(term("inherited", "tooltip.cobblemon_incubator.breeding_scanner.inherited_ivs"))
                .append(gray("; $forced")),
            Component.empty()
                .append(term("F/M", "tooltip.cobblemon_incubator.breeding_scanner.f_m"))
                .append(gray(": ${parentValues.take(3).joinToString(" ")}")),
            Component.empty()
                .append(term("F/M", "tooltip.cobblemon_incubator.breeding_scanner.f_m"))
                .append(gray(": ${parentValues.drop(3).joinToString(" ")}"))
        )
    }

    private fun natureSummary(parents: Pair<Pokemon, Pokemon>): Component {
        val everstoneParents = listOfNotNull(
            parents.first.takeIf { it.heldItem().item == CobblemonItems.EVERSTONE },
            parents.second.takeIf { it.heldItem().item == CobblemonItems.EVERSTONE }
        )
        if (everstoneParents.isEmpty()) {
            return Component.empty()
                .append(term("Nature", "tooltip.cobblemon_incubator.breeding_scanner.nature"))
                .append(gray(": random (${Natures.all().size})"))
        }
        return Component.empty()
            .append(term("Nature", "tooltip.cobblemon_incubator.breeding_scanner.nature"))
            .append(gray(": "))
            .append(term("Everstone", "tooltip.cobblemon_incubator.breeding_scanner.everstone"))
            .append(gray(" (${everstoneParents.joinToString(" or ") { displayName(it.nature.name.toString()) }})"))
    }

    private fun abilitySummary(form: FormData, parents: Pair<Pokemon, Pokemon>): Component {
        val ancestor = if (isDitto(parents.second)) parents.first else parents.second
        val oldAbility = ancestor.ability
        if (oldAbility.forced && forcedAbilitiesEnabled()) {
            return Component.empty()
                .append(term("Ability", "tooltip.cobblemon_incubator.breeding_scanner.ability"))
                .append(gray(": ${displayName(oldAbility.name)} (forced)"))
        }

        val priorityAndIndex = if (oldAbility.index >= 0) {
            oldAbility.priority to oldAbility.index
        } else {
            val entry = ancestor.form.abilities.mapping.entries.firstOrNull { (_, abilities) ->
                abilities.map { it.template }.contains(oldAbility.template)
            }
            entry?.let { it.key to it.value.map { ability -> ability.template }.indexOf(oldAbility.template) }
                ?: (Priority.LOWEST to 0)
        }

        val (priority, index) = priorityAndIndex
        val inheritedAbility = form.abilities.mapping[priority]?.getOrNull(index)?.template
            ?: form.abilities.mapping[priority]?.firstOrNull()?.template
            ?: form.abilities.mapping[Priority.LOWEST]?.firstOrNull()?.template

        if (inheritedAbility == null) {
            return Component.empty()
                .append(term("Ability", "tooltip.cobblemon_incubator.breeding_scanner.ability"))
                .append(gray(": unknown"))
        }

        val remaining = if (hiddenAbilitiesEnabled()) {
            form.abilities.mapping.values.flatten().map { it.template }
        } else {
            form.abilities.mapping.values.flatten().filterNot { it.priority == Priority.LOW }.map { it.template }
        }.filterNot { it == inheritedAbility }

        val chance = if (priority == Priority.LOW) 60 else 80
        val inheritedName = displayName(inheritedAbility.name)
        return if (remaining.isEmpty()) {
            Component.empty()
                .append(term("Ability", "tooltip.cobblemon_incubator.breeding_scanner.ability"))
                .append(gray(": $inheritedName"))
        } else {
            val others = remaining.joinToString(", ") { displayName(it.name) }
            Component.empty()
                .append(term("Ability", "tooltip.cobblemon_incubator.breeding_scanner.ability"))
                .append(gray(": $inheritedName ${chance}%; else $others"))
        }
    }

    private fun ballSummary(parents: Pair<Pokemon, Pokemon>): Component {
        var ball = parents.second.caughtBall
        if (parents.first.species.name == parents.second.species.name) {
            val fatherBall = normalizedBallName(parents.first)
            val motherBall = normalizedBallName(parents.second)
            return if (fatherBall == motherBall) {
                Component.empty()
                    .append(term("Ball", "tooltip.cobblemon_incubator.breeding_scanner.ball"))
                    .append(gray(": $fatherBall"))
            } else {
                Component.empty()
                    .append(term("Ball", "tooltip.cobblemon_incubator.breeding_scanner.ball"))
                    .append(gray(": $fatherBall or $motherBall"))
            }
        }
        if (parents.second.species.name == "Ditto") ball = parents.first.caughtBall
        if (ball == PokeBalls.CHERISH_BALL || ball == PokeBalls.MASTER_BALL) ball = PokeBalls.POKE_BALL
        return Component.empty()
            .append(term("Ball", "tooltip.cobblemon_incubator.breeding_scanner.ball"))
            .append(gray(": ${displayName(ball.name.toString())}"))
    }

    private fun movesSummary(child: FormData, parents: Pair<Pokemon, Pokemon>): Component {
        val eggMoves = linkedSetOf<String>()
        for (parent in listOf(parents.first, parents.second)) {
            val accessible = parent.allAccessibleMoves.toMutableSet()
            parent.moveSet.getMoves().forEach { accessible.add(it.template) }
            accessible.filter { it in child.moves.eggMoves }.forEach { eggMoves.add(displayName(it.name)) }
        }
        if (child.species.name == "Pichu" &&
            listOf(parents.first, parents.second).any { it.heldItem().item == CobblemonItems.LIGHT_BALL }
        ) {
            eggMoves.add("Volt Tackle")
        }
        return if (eggMoves.isEmpty()) {
            Component.empty()
                .append(term("Egg moves", "tooltip.cobblemon_incubator.breeding_scanner.egg_moves"))
                .append(gray(": none"))
        } else {
            Component.empty()
                .append(term("Egg moves", "tooltip.cobblemon_incubator.breeding_scanner.egg_moves"))
                .append(gray(": ${eggMoves.joinToString(", ")}"))
        }
    }

    private fun powerItemToIV(item: Item): Stat? = when (item) {
        CobblemonItems.POWER_WEIGHT -> Stats.HP
        CobblemonItems.POWER_BRACER -> Stats.ATTACK
        CobblemonItems.POWER_BELT -> Stats.DEFENCE
        CobblemonItems.POWER_LENS -> Stats.SPECIAL_ATTACK
        CobblemonItems.POWER_BAND -> Stats.SPECIAL_DEFENCE
        CobblemonItems.POWER_ANKLET -> Stats.SPEED
        else -> null
    }

    private fun parentNames(parents: Pair<Pokemon, Pokemon>): String {
        return "${pokemonName(parents.first)} x ${pokemonName(parents.second)}"
    }

    private fun pokemonName(pokemon: Pokemon): String {
        return "${pokemon.getDisplayName().string} (${displayName(pokemon.gender.name.lowercase())})"
    }

    private fun eggName(form: FormData): String {
        return if (form.name == "Normal") {
            form.species.translatedName.string
        } else {
            "${form.species.translatedName.string} ${form.name}"
        }
    }

    private fun isDitto(pokemon: Pokemon): Boolean = pokemon.species == PokemonSpecies.getByName("ditto")

    private fun statLabel(stat: Stat): String = statLabels[stat] ?: stat.toString()

    private fun normalizedBallName(pokemon: Pokemon): String {
        val ball = if (pokemon.caughtBall == PokeBalls.CHERISH_BALL || pokemon.caughtBall == PokeBalls.MASTER_BALL) {
            PokeBalls.POKE_BALL
        } else {
            pokemon.caughtBall
        }
        return displayName(ball.name.toString())
    }

    private fun hiddenAbilitiesEnabled(): Boolean = configBoolean("getHiddenAbilitiesEnabled", true)

    private fun forcedAbilitiesEnabled(): Boolean = configBoolean("getForcedAbilitiesEnabled", false)

    private fun configBoolean(getter: String, default: Boolean): Boolean {
        val config = cobbreedingConfig ?: return default
        return runCatching { config.javaClass.getMethod(getter).invoke(config) as? Boolean }
            .getOrNull() ?: default
    }

    private fun displayName(value: String?): String {
        val clean = value?.substringAfter(':')?.substringAfterLast('/') ?: return "Any"
        return clean.split('-', '_', ' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }

    private fun gray(text: String): Component = Component.literal(text).withStyle(ChatFormatting.GRAY)

    private fun term(text: String, tooltipKey: String): Component {
        return Component.literal(text).withStyle { style ->
            style
                .withColor(ChatFormatting.GRAY)
                .withUnderlined(true)
                .withHoverEvent(
                    HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        Component.translatable(tooltipKey).withStyle(ChatFormatting.GRAY)
                    )
                )
        }
    }

    private data class EggPreview(
        val form: FormData,
        val parents: List<Pair<Pokemon, Pokemon>>
    )
}
