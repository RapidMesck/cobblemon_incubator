package com.nbp.cobblemon_incubator.util

import com.cobblemon.mod.common.api.abilities.Abilities
import com.cobblemon.mod.common.api.pokemon.Natures
import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import com.cobblemon.mod.common.api.pokemon.stats.Stat
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.nbp.cobblemon_incubator.registry.ModRegistries
import net.minecraft.world.item.ItemStack

data class FilterConfig(
    val species: String? = null,
    val nature: String? = null,
    val ability: String? = null,
    val ivRules: Map<String, IvRule> = emptyMap(),
    val rejectAction: RejectAction = RejectAction.OUTPUT
) {
    companion object {
        val STATS: List<StatEntry> = listOf(
            StatEntry("hp", "HP", Stats.HP),
            StatEntry("atk", "Atk", Stats.ATTACK),
            StatEntry("def", "Def", Stats.DEFENCE),
            StatEntry("spa", "SpA", Stats.SPECIAL_ATTACK),
            StatEntry("spd", "SpD", Stats.SPECIAL_DEFENCE),
            StatEntry("spe", "Spe", Stats.SPEED)
        )

        fun fromStack(stack: ItemStack): FilterConfig {
            val encoded = stack.get(ModRegistries.FILTER_CONFIG.get()) ?: return FilterConfig()
            return decode(encoded)
        }

        fun save(stack: ItemStack, config: FilterConfig) {
            if (config == FilterConfig()) {
                stack.remove(ModRegistries.FILTER_CONFIG.get())
            } else {
                stack.set(ModRegistries.FILTER_CONFIG.get(), config.encode())
            }
        }

        private fun decode(encoded: String): FilterConfig {
            val values = encoded.split(";")
                .mapNotNull {
                    val split = it.split("=", limit = 2)
                    if (split.size == 2) split[0] to split[1] else null
                }
                .toMap()
            val rules = STATS.mapNotNull { stat ->
                val raw = values["iv_${stat.key}"] ?: return@mapNotNull null
                val parts = raw.split(":")
                val operator = parts.getOrNull(0)?.let { IvOperator.byId(it) } ?: return@mapNotNull null
                val value = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 31) ?: return@mapNotNull null
                stat.key to IvRule(operator, value)
            }.toMap()
            return FilterConfig(
                species = values["species"]?.takeIf { it.isNotBlank() },
                nature = values["nature"]?.takeIf { it.isNotBlank() },
                ability = values["ability"]?.takeIf { it.isNotBlank() },
                ivRules = rules,
                rejectAction = values["reject"]?.let { RejectAction.byId(it) } ?: RejectAction.OUTPUT
            )
        }
    }

    fun encode(): String {
        return buildList {
            species?.let { add("species=$it") }
            nature?.let { add("nature=$it") }
            ability?.let { add("ability=$it") }
            add("reject=${rejectAction.id}")
            for ((key, rule) in ivRules) {
                add("iv_$key=${rule.operator.id}:${rule.value}")
            }
        }.joinToString(";")
    }

    fun hasCriteria(): Boolean {
        return species != null || nature != null || ability != null || ivRules.isNotEmpty()
    }

    fun matches(properties: PokemonProperties): Boolean {
        if (species != null && normalize(properties.species) != normalize(species)) return false
        if (nature != null && normalize(properties.nature) != normalize(nature)) return false
        if (ability != null && normalize(properties.ability) != normalize(ability)) return false

        if (ivRules.isNotEmpty()) {
            val ivs = properties.ivs ?: return false
            for ((key, rule) in ivRules) {
                val stat = STATS.firstOrNull { it.key == key }?.stat ?: return false
                if (!rule.matches(ivs.getOrDefault(stat))) return false
            }
        }
        return true
    }

    fun withSpeciesFrom(properties: PokemonProperties?): FilterConfig {
        val current = normalize(species)
        val nextSpecies = properties?.species?.takeIf { it.isNotBlank() }
        return copy(species = if (nextSpecies != null && normalize(nextSpecies) != current) nextSpecies else null)
    }

    fun cycleNature(delta: Int): FilterConfig {
        val options = natureOptions()
        return copy(nature = cycleOption(options, nature, delta))
    }

    fun cycleAbility(delta: Int): FilterConfig {
        val options = abilityOptions()
        if (options.size <= 1) return this
        return copy(ability = cycleOption(options, ability, delta))
    }

    fun setNatureByIndex(index: Int): FilterConfig {
        val options = natureOptions()
        return copy(nature = options.getOrNull(index))
    }

    fun setAbilityByIndex(index: Int): FilterConfig {
        val options = abilityOptions()
        return copy(ability = options.getOrNull(index))
    }

    fun cycleRejectAction(): FilterConfig = copy(rejectAction = rejectAction.next())

    fun cycleIvOperator(statIndex: Int): FilterConfig {
        val stat = STATS.getOrNull(statIndex) ?: return this
        val current = ivRules[stat.key] ?: IvRule(IvOperator.GREATER_OR_EQUAL, 31)
        return copy(ivRules = ivRules + (stat.key to current.copy(operator = current.operator.next())))
    }

    fun adjustIv(statIndex: Int, delta: Int): FilterConfig {
        val stat = STATS.getOrNull(statIndex) ?: return this
        val current = ivRules[stat.key] ?: IvRule(IvOperator.GREATER_OR_EQUAL, 31)
        return copy(ivRules = ivRules + (stat.key to current.copy(value = (current.value + delta).coerceIn(0, 31))))
    }

    fun clearIv(statIndex: Int): FilterConfig {
        val stat = STATS.getOrNull(statIndex) ?: return this
        return copy(ivRules = ivRules - stat.key)
    }

    fun summary(): String {
        val parts = mutableListOf<String>()
        species?.let { parts.add("Species $it") }
        nature?.let { parts.add("Nature $it") }
        ability?.let { parts.add("Ability $it") }
        if (ivRules.isNotEmpty()) {
            parts.add(
                ivRules.entries.joinToString(" ") { (key, rule) ->
                    val label = STATS.firstOrNull { it.key == key }?.label ?: key
                    "$label ${rule.operator.label}${rule.value}"
                }
            )
        }
        return parts.joinToString(" | ")
    }

    private fun <T> cycleOption(options: List<T>, current: T, delta: Int): T {
        val index = options.indexOfFirst { normalizeOption(it) == normalizeOption(current) }.let { if (it < 0) 0 else it }
        return options[Math.floorMod(index + delta, options.size)]
    }

    private fun normalizeOption(value: Any?): String = normalize(value?.toString()).orEmpty()

    private fun natureOptions(): List<String?> = listOf<String?>(null) + Natures.all().map { it.name.path }.sorted()

    private fun abilityOptions(): List<String?> = listOf<String?>(null) + Abilities.all().map { it.name }.sorted()

    private fun normalize(value: String?): String? {
        return value
            ?.substringAfter(':')
            ?.lowercase()
            ?.replace(" ", "")
            ?.replace("_", "")
            ?.takeIf { it.isNotBlank() }
    }
}

data class IvRule(val operator: IvOperator, val value: Int) {
    fun matches(actual: Int): Boolean {
        return when (operator) {
            IvOperator.GREATER_OR_EQUAL -> actual >= value
            IvOperator.LESS_OR_EQUAL -> actual <= value
            IvOperator.EQUAL -> actual == value
        }
    }
}

data class StatEntry(val key: String, val label: String, val stat: Stat)

enum class IvOperator(val id: String, val label: String) {
    GREATER_OR_EQUAL("ge", ">="),
    LESS_OR_EQUAL("le", "<="),
    EQUAL("eq", "=");

    fun next(): IvOperator = entries[(ordinal + 1) % entries.size]

    companion object {
        fun byId(id: String): IvOperator? = entries.firstOrNull { it.id == id }
    }
}

enum class RejectAction(val id: String, val label: String) {
    OUTPUT("output", "Output"),
    DELETE("delete", "Delete");

    fun next(): RejectAction = entries[(ordinal + 1) % entries.size]

    companion object {
        fun byId(id: String): RejectAction? = entries.firstOrNull { it.id == id }
    }
}
