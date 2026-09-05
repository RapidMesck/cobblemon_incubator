package com.nbp.cobblemon_incubator.network

import com.nbp.cobblemon_incubator.menu.EggPouchMenu
import dev.architectury.networking.NetworkManager
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import java.util.function.Supplier

data class EggPouchActionPacket(
    val scrollRow: Int,
    val searchQuery: String
) {
    companion object {
        fun encode(packet: EggPouchActionPacket, buffer: FriendlyByteBuf) {
            buffer.writeVarInt(packet.scrollRow)
            buffer.writeUtf(packet.searchQuery, 128)
        }

        fun decode(buffer: FriendlyByteBuf): EggPouchActionPacket {
            val scrollRow = buffer.readVarInt()
            val searchQuery = buffer.readUtf(128)
            return EggPouchActionPacket(scrollRow, searchQuery)
        }

        fun handle(packet: EggPouchActionPacket, contextSupplier: Supplier<NetworkManager.PacketContext>) {
            val context = contextSupplier.get()
            context.queue {
                val player = context.player as? ServerPlayer ?: return@queue
                val menu = player.containerMenu as? EggPouchMenu ?: return@queue
                menu.updateFilter(packet.scrollRow, packet.searchQuery)
            }
        }
    }
}
