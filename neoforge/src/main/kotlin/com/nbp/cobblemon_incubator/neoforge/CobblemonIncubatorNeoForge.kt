package com.nbp.cobblemon_incubator.neoforge

import com.nbp.cobblemon_incubator.CobblemonIncubator
import com.nbp.cobblemon_incubator.client.CobblemonIncubatorClient
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

@Mod(CobblemonIncubator.MOD_ID)
class CobblemonIncubatorNeoForge {
    init {
        CobblemonIncubator.init()
        MOD_BUS.addListener(this::onClientSetup)
    }

    private fun onClientSetup(event: FMLClientSetupEvent) {
        event.enqueueWork {
            CobblemonIncubatorClient.init()
        }
    }
}
