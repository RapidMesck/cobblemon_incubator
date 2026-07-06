package com.nbp.cobblemon_incubator

import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent
import com.nbp.cobblemon_incubator.config.IncubatorConfig
import com.nbp.cobblemon_incubator.item.StemCellSyringeItem
import com.nbp.cobblemon_incubator.registry.ModRegistries
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import org.slf4j.LoggerFactory

object CobblemonIncubator {
    const val MOD_ID = "cobblemon_incubator"

    val logger = LoggerFactory.getLogger("CobblemonIncubator")

    fun init() {
        IncubatorConfig.load()
        ModRegistries.register()
        registerEvents()
        logger.info("Cobblemon Incubator loaded!")
    }

    private fun registerEvents() {
        CobblemonEvents.BATTLE_VICTORY.subscribe { event ->
            handleBattleVictory(event)
        }
    }

    private fun handleBattleVictory(event: BattleVictoryEvent) {
        val players = event.winners.filter { it.type == com.cobblemon.mod.common.api.battles.model.actor.ActorType.PLAYER }
        if (players.isEmpty()) return

        val serverPlayers = event.battle.players
        if (serverPlayers.isEmpty()) return

        val losers = event.losers.filter { it.type == com.cobblemon.mod.common.api.battles.model.actor.ActorType.WILD }
        var totalCharge = 0
        for (loser in losers) {
            for (bp in loser.pokemonList) {
                val level = bp.effectedPokemon.level
                totalCharge += level * IncubatorConfig.geneFusionChargePerLevel
            }
        }
        if (totalCharge <= 0) return

        for (serverPlayer in serverPlayers) {
            val syringes = findSyringesInInventory(serverPlayer)
            if (syringes.isEmpty()) continue

            var remaining = totalCharge
            for (syringe in syringes) {
                if (remaining <= 0) break
                val added = StemCellSyringeItem.addCharge(syringe, remaining)
                remaining -= added
            }
        }
    }

    private fun findSyringesInInventory(player: ServerPlayer): List<ItemStack> {
        val result = mutableListOf<ItemStack>()
        val maxCharge = IncubatorConfig.geneFusionSyringeMaxCharge
        for (stack in player.inventory.items) {
            if (stack.`is`(ModRegistries.STEM_CELL_SYRINGE.get())) {
                val charge = StemCellSyringeItem.getCharge(stack)
                if (charge < maxCharge) {
                    result.add(stack)
                }
            }
        }
        return result
    }
}
