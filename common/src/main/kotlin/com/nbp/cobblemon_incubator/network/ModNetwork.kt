package com.nbp.cobblemon_incubator.network

import dev.architectury.networking.NetworkManager

object ModNetwork {
    fun register() {
        NetworkManager.registerReceiver(
            NetworkManager.c2s(),
            EggPouchActionPacket.TYPE,
            EggPouchActionPacket.CODEC
        ) { packet, context ->
            EggPouchActionPacket.handle(packet, context)
        }
    }

    fun sendToServer(packet: EggPouchActionPacket) {
        NetworkManager.sendToServer(packet)
    }
}
