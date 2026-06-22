package com.nbp.cobblemon_incubator

import com.cobblemon.mod.common.api.events.CobblemonEvents
import net.minecraft.network.chat.Component
import org.slf4j.LoggerFactory

object CobblemonIncubator {
    const val MOD_ID = "cobblemon_incubator"

    private val logger = LoggerFactory.getLogger("CobblemonIncubator")

    fun init() {
        logger.info("Cobblemon Incubator carregado!")

        CobblemonEvents.POKEMON_CAPTURED.subscribe { event ->
            val player = event.player
            val pokemon = event.pokemon

            player.sendSystemMessage(
                Component.literal("Voce capturou: ${pokemon.species.name}")
            )
        }
    }
}
