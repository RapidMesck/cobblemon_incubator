package com.nbp.cobblemon_incubator.fabric

import com.nbp.cobblemon_incubator.client.CobblemonIncubatorClient
import net.fabricmc.api.ClientModInitializer

class CobblemonIncubatorFabricClient : ClientModInitializer {
    override fun onInitializeClient() {
        CobblemonIncubatorClient.init()
    }
}
