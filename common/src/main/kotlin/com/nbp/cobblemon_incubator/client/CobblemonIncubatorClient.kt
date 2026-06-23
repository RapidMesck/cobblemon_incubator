package com.nbp.cobblemon_incubator.client

import com.nbp.cobblemon_incubator.client.screen.EggIncubatorScreen
import com.nbp.cobblemon_incubator.registry.ModRegistries
import dev.architectury.registry.menu.MenuRegistry

object CobblemonIncubatorClient {
    fun init() {
        MenuRegistry.registerScreenFactory(ModRegistries.EGG_INCUBATOR_MENU.get(), ::EggIncubatorScreen)
    }
}
