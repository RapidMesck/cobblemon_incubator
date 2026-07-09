package com.nbp.cobblemon_incubator.client

import com.nbp.cobblemon_incubator.client.screen.EggIncubatorScreen
import com.nbp.cobblemon_incubator.registry.ModRegistries
import dev.architectury.registry.menu.MenuRegistry
import dev.architectury.registry.client.rendering.RenderTypeRegistry
import net.minecraft.client.renderer.RenderType

object CobblemonIncubatorClient {
    fun registerScreen() {
        MenuRegistry.registerScreenFactory(ModRegistries.EGG_INCUBATOR_MENU.get(), ::EggIncubatorScreen)
    }

    fun registerRenderTypes() {
        RenderTypeRegistry.register(RenderType.cutout(), ModRegistries.EGG_INCUBATOR.get())
    }

    fun init() {
        registerScreen()
        registerRenderTypes()
    }
}
