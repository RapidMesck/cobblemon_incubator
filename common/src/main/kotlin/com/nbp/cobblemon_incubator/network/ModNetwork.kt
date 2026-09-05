package com.nbp.cobblemon_incubator.network

import com.nbp.cobblemon_incubator.CobblemonIncubator
import dev.architectury.networking.NetworkChannel
import net.minecraft.resources.ResourceLocation

object ModNetwork {
    val CHANNEL: NetworkChannel = NetworkChannel.create(
        ResourceLocation.fromNamespaceAndPath(CobblemonIncubator.MOD_ID, "main")
    )

    fun register() {
        CHANNEL.register(
            EggPouchActionPacket::class.java,
            EggPouchActionPacket::encode,
            EggPouchActionPacket::decode,
            EggPouchActionPacket::handle
        )
    }
}
