package com.nbp.cobblemon_incubator.config

import com.google.gson.GsonBuilder
import com.nbp.cobblemon_incubator.CobblemonIncubator
import dev.architectury.platform.Platform
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.roundToInt

object IncubatorConfig {
    private const val FILE_NAME = "cobblemon_incubator.json"
    private const val SYNC_MARKER = 1 shl 30
    const val DISPLAY_MODEL = 1 shl 0
    const val DISPLAY_SPECIES = 1 shl 1
    const val DISPLAY_NATURE = 1 shl 2
    const val DISPLAY_ABILITY = 1 shl 3
    const val DISPLAY_IVS = 1 shl 4
    const val UPGRADE_SPEED = 1 shl 0
    const val UPGRADE_PC = 1 shl 1
    const val UPGRADE_FILTER = 1 shl 2
    const val FILTER_SPECIES = 1 shl 0
    const val FILTER_NATURE = 1 shl 1
    const val FILTER_ABILITY = 1 shl 2
    const val FILTER_IVS = 1 shl 3
    const val FILTER_REJECT_ACTION = 1 shl 4
    const val FILTER_DELETE_REJECT_ACTION = 1 shl 5
    const val AUTOMATION_INPUT = 1 shl 0
    const val AUTOMATION_OUTPUT = 1 shl 1
    private val gson = GsonBuilder().setPrettyPrinting().create()

    private data class Values(
        var baseSpeed: Int = 5,
        var speedUpgradeMultiplier: Double = 2.0,
        var display: DisplayOptions? = DisplayOptions(),
        var upgrades: UpgradeOptions? = UpgradeOptions(),
        var filters: FilterOptions? = FilterOptions(),
        var automation: AutomationOptions? = AutomationOptions(),
        var geneFusion: GeneFusionOptions? = GeneFusionOptions()
    )

    private data class DisplayOptions(
        var pokemonModel: Boolean = true,
        var species: Boolean = true,
        var nature: Boolean = true,
        var ability: Boolean = true,
        var ivs: Boolean = true
    )

    private data class UpgradeOptions(
        var speed: Boolean = true,
        var pc: Boolean = true,
        var filter: Boolean = true
    )

    private data class FilterOptions(
        var species: Boolean = true,
        var nature: Boolean = true,
        var ability: Boolean = true,
        var ivs: Boolean = true,
        var rejectAction: Boolean = true,
        var deleteRejectedEggs: Boolean = true
    )

    private data class AutomationOptions(
        var autoInputFromPastures: Boolean = true,
        var autoOutputToInventories: Boolean = true
    )

    private data class GeneFusionOptions(
        var costMultiplier: Double = 1.0,
        var chargePerLevel: Int = 10,
        var syringeMaxCharge: Int = 500
    )

    private var values = Values()

    val baseSpeed: Int
        get() = values.baseSpeed

    val speedUpgradeMultiplier: Double
        get() = values.speedUpgradeMultiplier

    val upgradedSpeed: Int
        get() = (baseSpeed * speedUpgradeMultiplier).roundToInt().coerceAtLeast(1)

    val displayPokemonModel: Boolean
        get() = values.display?.pokemonModel ?: true

    val displaySpecies: Boolean
        get() = values.display?.species ?: true

    val displayNature: Boolean
        get() = values.display?.nature ?: true

    val displayAbility: Boolean
        get() = values.display?.ability ?: true

    val displayIvs: Boolean
        get() = values.display?.ivs ?: true

    val speedUpgradeEnabled: Boolean
        get() = values.upgrades?.speed ?: true

    val pcUpgradeEnabled: Boolean
        get() = values.upgrades?.pc ?: true

    val filterUpgradeEnabled: Boolean
        get() = values.upgrades?.filter ?: true

    val speciesFilterEnabled: Boolean
        get() = values.filters?.species ?: true

    val natureFilterEnabled: Boolean
        get() = values.filters?.nature ?: true

    val abilityFilterEnabled: Boolean
        get() = values.filters?.ability ?: true

    val ivFilterEnabled: Boolean
        get() = values.filters?.ivs ?: true

    val rejectActionFilterEnabled: Boolean
        get() = values.filters?.rejectAction ?: true

    val deleteRejectedEggsEnabled: Boolean
        get() = values.filters?.deleteRejectedEggs ?: true

    val autoInputFromPastures: Boolean
        get() = values.automation?.autoInputFromPastures ?: true

    val autoOutputToInventories: Boolean
        get() = values.automation?.autoOutputToInventories ?: true

    val geneFusionCostMultiplier: Double
        get() = values.geneFusion?.costMultiplier ?: 1.0

    val geneFusionChargePerLevel: Int
        get() = values.geneFusion?.chargePerLevel ?: 10

    val geneFusionSyringeMaxCharge: Int
        get() = values.geneFusion?.syringeMaxCharge ?: 500

    val displayMask: Int
        get() = maskOf(
            DISPLAY_MODEL to displayPokemonModel,
            DISPLAY_SPECIES to displaySpecies,
            DISPLAY_NATURE to displayNature,
            DISPLAY_ABILITY to displayAbility,
            DISPLAY_IVS to displayIvs
        )

    val upgradeMask: Int
        get() = maskOf(
            UPGRADE_SPEED to speedUpgradeEnabled,
            UPGRADE_PC to pcUpgradeEnabled,
            UPGRADE_FILTER to filterUpgradeEnabled
        )

    val filterMask: Int
        get() = maskOf(
            FILTER_SPECIES to speciesFilterEnabled,
            FILTER_NATURE to natureFilterEnabled,
            FILTER_ABILITY to abilityFilterEnabled,
            FILTER_IVS to ivFilterEnabled,
            FILTER_REJECT_ACTION to rejectActionFilterEnabled,
            FILTER_DELETE_REJECT_ACTION to deleteRejectedEggsEnabled
        )

    val automationMask: Int
        get() = maskOf(
            AUTOMATION_INPUT to autoInputFromPastures,
            AUTOMATION_OUTPUT to autoOutputToInventories
        )

    val syncedDisplayMask: Int
        get() = syncMask(displayMask)

    val syncedUpgradeMask: Int
        get() = syncMask(upgradeMask)

    val syncedFilterMask: Int
        get() = syncMask(filterMask)

    val syncedAutomationMask: Int
        get() = syncMask(automationMask)

    fun load() {
        val path = Platform.getConfigFolder().resolve(FILE_NAME)
        values = read(path).sanitize()
        write(path, values)
        CobblemonIncubator.logger.info(
            "Loaded incubator config: baseSpeed={}, speedUpgradeMultiplier={}, upgradedSpeed={}, displayMask={}, upgradeMask={}, filterMask={}, automationMask={}, geneFusion(costMultiplier={}, chargePerLevel={}, syringeMaxCharge={})",
            baseSpeed,
            speedUpgradeMultiplier,
            upgradedSpeed,
            displayMask,
            upgradeMask,
            filterMask,
            automationMask,
            geneFusionCostMultiplier,
            geneFusionChargePerLevel,
            geneFusionSyringeMaxCharge
        )
    }

    fun resolveSyncedMask(value: Int, fallback: Int): Int {
        return if (value and SYNC_MARKER != 0) value and SYNC_MARKER.inv() else fallback
    }

    fun enabled(mask: Int, flag: Int): Boolean = mask and flag != 0

    private fun read(path: Path): Values {
        if (!Files.exists(path)) return Values()
        return runCatching {
            Files.newBufferedReader(path, StandardCharsets.UTF_8).use { reader ->
                gson.fromJson(reader, Values::class.java) ?: Values()
            }
        }.onFailure {
            CobblemonIncubator.logger.error("Failed to read $path; using default values", it)
        }.getOrDefault(Values())
    }

    private fun write(path: Path, config: Values) {
        runCatching {
            Files.createDirectories(path.parent)
            Files.newBufferedWriter(path, StandardCharsets.UTF_8).use { writer ->
                gson.toJson(config, writer)
            }
        }.onFailure {
            CobblemonIncubator.logger.error("Failed to write incubator config to $path", it)
        }
    }

    private fun Values.sanitize(): Values {
        baseSpeed = baseSpeed.coerceIn(1, 100_000)
        speedUpgradeMultiplier = speedUpgradeMultiplier
            .takeIf { it.isFinite() }
            ?.coerceIn(0.01, 1_000.0)
            ?: 2.0
        display = display ?: DisplayOptions()
        upgrades = upgrades ?: UpgradeOptions()
        filters = filters ?: FilterOptions()
        automation = automation ?: AutomationOptions()
        geneFusion = geneFusion ?: GeneFusionOptions()
        geneFusion?.costMultiplier = geneFusion!!.costMultiplier
            .takeIf { it.isFinite() }
            ?.coerceIn(0.01, 1000.0)
            ?: 1.0
        geneFusion?.chargePerLevel = geneFusion!!.chargePerLevel.coerceIn(1, 1000)
        geneFusion?.syringeMaxCharge = geneFusion!!.syringeMaxCharge.coerceIn(1, 100000)
        return this
    }

    private fun syncMask(mask: Int): Int = mask or SYNC_MARKER

    private fun maskOf(vararg entries: Pair<Int, Boolean>): Int {
        return entries.fold(0) { mask, (flag, enabled) -> if (enabled) mask or flag else mask }
    }
}
