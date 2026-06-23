package com.nbp.cobblemon_incubator

import com.nbp.cobblemon_incubator.registry.ModRegistries
import org.slf4j.LoggerFactory

object CobblemonIncubator {
    const val MOD_ID = "cobblemon_incubator"

    val logger = LoggerFactory.getLogger("CobblemonIncubator")

    fun init() {
        ModRegistries.register()
        logger.info("Cobblemon Incubator loaded!")
    }
}
