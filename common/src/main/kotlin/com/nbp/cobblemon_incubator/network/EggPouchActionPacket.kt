package com.nbp.cobblemon_incubator.network

import com.nbp.cobblemon_incubator.CobblemonIncubator
import com.nbp.cobblemon_incubator.menu.EggPouchMenu
import dev.architectury.networking.NetworkManager
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

data class EggPouchActionPacket(
    val scrollRow: Int,
    val searchQuery: String
) : CustomPacketPayload {

    companion object {
        val TYPE: CustomPacketPayload.Type<EggPouchActionPacket> =
            CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath(CobblemonIncubator.MOD_ID, "egg_pouch_action"))

        val CODEC: StreamCodec<RegistryFriendlyByteBuf, EggPouchActionPacket> = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EggPouchActionPacket::scrollRow,
            ByteBufCodecs.stringUtf8(128), EggPouchActionPacket::searchQuery,
            ::EggPouchActionPacket
        )

        fun handle(packet: EggPouchActionPacket, context: NetworkManager.PacketContext) {
            context.queue {
                val player = context.player as? ServerPlayer ?: return@queue
                val menu = player.containerMenu as? EggPouchMenu ?: return@queue
                menu.updateFilter(packet.scrollRow, packet.searchQuery)
            }
        }
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
}
