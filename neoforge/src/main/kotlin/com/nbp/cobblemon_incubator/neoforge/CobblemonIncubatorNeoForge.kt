package com.nbp.cobblemon_incubator.neoforge

import com.nbp.cobblemon_incubator.CobblemonIncubator
import net.neoforged.api.distmarker.Dist
import net.neoforged.fml.common.Mod
import net.neoforged.fml.loading.FMLEnvironment

@Mod(CobblemonIncubator.MOD_ID)
class CobblemonIncubatorNeoForge {
    init {
        NeoForgeItemTransferHelper.register()
        CobblemonIncubator.init()
        if (FMLEnvironment.dist == Dist.CLIENT) {
            CobblemonIncubatorNeoForgeClient()
        }
    }
}
