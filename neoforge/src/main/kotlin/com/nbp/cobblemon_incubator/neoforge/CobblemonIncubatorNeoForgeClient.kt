package com.nbp.cobblemon_incubator.neoforge

import com.nbp.cobblemon_incubator.CobblemonIncubator
import com.nbp.cobblemon_incubator.client.CobblemonIncubatorClient
import com.nbp.cobblemon_incubator.client.screen.EggIncubatorScreen
import com.nbp.cobblemon_incubator.client.screen.GeneFusionScreen
import com.nbp.cobblemon_incubator.registry.ModRegistries
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

class CobblemonIncubatorNeoForgeClient {
    init {
        MOD_BUS.addListener(this::onClientSetup)
        MOD_BUS.addListener(this::registerMenuScreens)
    }

    private fun onClientSetup(event: FMLClientSetupEvent) {
        event.enqueueWork(CobblemonIncubatorClient::registerRenderTypes)
    }

    private fun registerMenuScreens(event: RegisterMenuScreensEvent) {
        event.register(ModRegistries.EGG_INCUBATOR_MENU.get(), ::EggIncubatorScreen)
        event.register(ModRegistries.GENE_FUSION_MENU.get(), ::GeneFusionScreen)
    }
}
