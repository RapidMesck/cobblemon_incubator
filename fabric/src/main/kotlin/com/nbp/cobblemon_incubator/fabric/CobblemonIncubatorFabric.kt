package com.nbp.cobblemon_incubator.fabric

import com.nbp.cobblemon_incubator.CobblemonIncubator
import net.fabricmc.api.ModInitializer

class CobblemonIncubatorFabric : ModInitializer {
    override fun onInitialize() {
        CobblemonIncubator.init()
    }
}
