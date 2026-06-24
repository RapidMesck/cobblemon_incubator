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
    private val gson = GsonBuilder().setPrettyPrinting().create()

    private data class Values(
        var baseSpeed: Int = 5,
        var speedUpgradeMultiplier: Double = 2.0
    )

    private var values = Values()

    val baseSpeed: Int
        get() = values.baseSpeed

    val speedUpgradeMultiplier: Double
        get() = values.speedUpgradeMultiplier

    val upgradedSpeed: Int
        get() = (baseSpeed * speedUpgradeMultiplier).roundToInt().coerceAtLeast(1)

    fun load() {
        val path = Platform.getConfigFolder().resolve(FILE_NAME)
        values = read(path).sanitize()
        write(path, values)
        CobblemonIncubator.logger.info(
            "Loaded incubator config: baseSpeed={}, speedUpgradeMultiplier={}, upgradedSpeed={}",
            baseSpeed,
            speedUpgradeMultiplier,
            upgradedSpeed
        )
    }

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
        return this
    }
}
