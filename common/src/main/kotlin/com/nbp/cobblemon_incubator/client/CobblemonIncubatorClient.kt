package com.nbp.cobblemon_incubator.client

import com.nbp.cobblemon_incubator.client.screen.EggIncubatorScreen
import com.nbp.cobblemon_incubator.client.screen.GeneFusionScreen
import com.nbp.cobblemon_incubator.registry.ModRegistries
import dev.architectury.registry.menu.MenuRegistry
import dev.architectury.registry.client.rendering.RenderTypeRegistry
import net.minecraft.client.renderer.RenderType

object CobblemonIncubatorClient {
    fun registerScreen() {
        MenuRegistry.registerScreenFactory(ModRegistries.EGG_INCUBATOR_MENU.get(), ::EggIncubatorScreen)
        MenuRegistry.registerScreenFactory(ModRegistries.GENE_FUSION_MENU.get(), ::GeneFusionScreen)
    }

    fun registerRenderTypes() {
        RenderTypeRegistry.register(RenderType.translucent(), ModRegistries.EGG_INCUBATOR.get())
        RenderTypeRegistry.register(RenderType.translucent(), ModRegistries.GENE_FUSION.get())
        RenderTypeRegistry.register(RenderType.cutout(), ModRegistries.EGG_INCUBATOR.get())
        RenderTypeRegistry.register(RenderType.cutout(), ModRegistries.GENE_FUSION.get())
    }

    fun init() {
        registerScreen()
        registerRenderTypes()
    }
}
