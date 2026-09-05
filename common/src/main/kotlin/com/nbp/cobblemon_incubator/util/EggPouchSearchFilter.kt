package com.nbp.cobblemon_incubator.util

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.api.pokemon.stats.Stat
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import net.minecraft.world.item.ItemStack

object EggPouchSearchFilter {
    private val STAT_MAP: Map<String, Stat> = mapOf(
        "hp" to Stats.HP,
        "atk" to Stats.ATTACK,
        "attack" to Stats.ATTACK,
        "def" to Stats.DEFENCE,
        "defence" to Stats.DEFENCE,
        "defense" to Stats.DEFENCE,
        "spa" to Stats.SPECIAL_ATTACK,
        "spatk" to Stats.SPECIAL_ATTACK,
        "spd" to Stats.SPECIAL_DEFENCE,
        "spdef" to Stats.SPECIAL_DEFENCE,
        "spe" to Stats.SPEED,
        "speed" to Stats.SPEED
    )

    private val STAT_IV_REGEX = Regex("""^iv:(hp|atk|attack|def|defence|defense|spa|spatk|spd|spdef|spe|speed)(>=|<=|=|>|<)(\d+)$""")

    fun matches(egg: ItemStack, query: String, isAdvanced: Boolean): Boolean {
        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isEmpty()) return true

        val properties = CobbreedingCompat.extractProperties(egg) ?: return false
        val speciesId = properties.species ?: ""
        val species = PokemonSpecies.species.firstOrNull {
            it.resourceIdentifier.toString().equals(speciesId, ignoreCase = true) ||
                it.resourceIdentifier.path.equals(speciesId.substringAfter(':'), ignoreCase = true)
        }

        val speciesName = speciesId.substringAfter(':').lowercase().replace("_", " ")
        val translatedName = species?.translatedName?.string?.lowercase().orEmpty()

        if (!isAdvanced) {
            val terms = cleanQuery.split(" ").filter { it.isNotBlank() }
            return terms.all { term ->
                speciesName.contains(term) || translatedName.contains(term)
            }
        }

        val terms = cleanQuery.split(" ").filter { it.isNotBlank() }
        return terms.all { term ->
            matchAdvancedTerm(term, properties, species, speciesName, translatedName)
        }
    }

    private fun matchAdvancedTerm(
        term: String,
        properties: com.cobblemon.mod.common.api.pokemon.PokemonProperties,
        species: com.cobblemon.mod.common.pokemon.Species?,
        speciesName: String,
        translatedName: String
    ): Boolean {
        // Elemental Type
        if (term.startsWith("t:") || term.startsWith("type:")) {
            val target = term.substringAfter(':')
            val types = species?.types?.map { it.name.lowercase() } ?: emptyList()
            return types.any { it.contains(target) }
        }

        // Ability
        if (term.startsWith("a:") || term.startsWith("ability:")) {
            val target = term.substringAfter(':').replace("_", " ")
            val ability = properties.ability?.lowercase()?.replace("_", " ").orEmpty()
            return ability.contains(target)
        }

        // Nature
        if (term.startsWith("n:") || term.startsWith("nature:")) {
            val target = term.substringAfter(':').replace("_", " ")
            val nature = properties.nature?.lowercase()?.replace("_", " ").orEmpty()
            return nature.contains(target)
        }

        // Gender
        if (term.startsWith("g:") || term.startsWith("gender:")) {
            val target = term.substringAfter(':')
            val gender = CobbreedingCompat.resolveGenderName(properties)?.lowercase().orEmpty()
            return gender.startsWith(target)
        }

        // Shiny
        if (term == "shiny" || term == "shiny:true" || term == "s:true" || term == "s:shiny") {
            return properties.shiny == true
        }
        if (term == "!shiny" || term == "shiny:false" || term == "s:false") {
            return properties.shiny != true
        }

        // Perfect IV count: e.g. "5iv", "6iv", "0iv"
        if (term.endsWith("iv") && term.length > 2 && term.dropLast(2).all { it.isDigit() }) {
            val min = term.dropLast(2).toIntOrNull() ?: return false
            val ivs = properties.ivs ?: return false
            val perfectCount = FilterConfig.STATS.count { ivs.getOrDefault(it.stat) == 31 }
            return perfectCount >= min
        }

        // IV min count: "iv>=5", "iv=6"
        if (term.startsWith("iv>=") || term.startsWith("iv=")) {
            val min = term.substringAfter('=').toIntOrNull() ?: return false
            val ivs = properties.ivs ?: return false
            val perfectCount = FilterConfig.STATS.count { ivs.getOrDefault(it.stat) == 31 }
            return if (term.startsWith("iv>=")) perfectCount >= min else perfectCount == min
        }

        // Specific Stat IV: e.g. iv:hp=31, iv:atk<=0, iv:spe>=30
        val statMatch = STAT_IV_REGEX.matchEntire(term)
        if (statMatch != null) {
            val statKey = statMatch.groupValues[1]
            val op = statMatch.groupValues[2]
            val value = statMatch.groupValues[3].toIntOrNull() ?: return false
            val stat = STAT_MAP[statKey] ?: return false
            val ivs = properties.ivs ?: return false
            val actual = ivs.getOrDefault(stat)
            return when (op) {
                ">=" -> actual >= value
                "<=" -> actual <= value
                "=" -> actual == value
                ">" -> actual > value
                "<" -> actual < value
                else -> false
            }
        }

        // Default: Species search
        return speciesName.contains(term) || translatedName.contains(term)
    }
}
