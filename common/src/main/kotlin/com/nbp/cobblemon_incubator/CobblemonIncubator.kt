package com.nbp.cobblemon_incubator

import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent
import com.nbp.cobblemon_incubator.command.FillSyringeCommand
import com.nbp.cobblemon_incubator.config.IncubatorConfig
import com.nbp.cobblemon_incubator.item.StemCellSyringeItem
import com.nbp.cobblemon_incubator.registry.ModRegistries
import dev.architectury.event.events.common.CommandRegistrationEvent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import org.slf4j.LoggerFactory
import kotlin.math.roundToInt

object CobblemonIncubator {
    const val MOD_ID = "cobblemon_incubator"

    val logger = LoggerFactory.getLogger("CobblemonIncubator")

    fun init() {
        IncubatorConfig.load()
        ModRegistries.register()
        registerEvents()
        registerCommands()
        logger.info("Cobblemon Incubator loaded!")
    }

    private fun registerCommands() {
        CommandRegistrationEvent.EVENT.register { dispatcher, _, _ ->
            FillSyringeCommand.register(dispatcher)
        }
    }

    private fun registerEvents() {
        CobblemonEvents.BATTLE_VICTORY.subscribe { event ->
            handleBattleVictory(event)
        }
    }

    private fun handleBattleVictory(event: BattleVictoryEvent) {
        val playerActors =
            event.winners.filter { it.type == com.cobblemon.mod.common.api.battles.model.actor.ActorType.PLAYER }
        if (playerActors.isEmpty()) return

        val serverPlayers = event.battle.players
        if (serverPlayers.isEmpty()) return

        val losers = event.losers.filter { it.type == com.cobblemon.mod.common.api.battles.model.actor.ActorType.WILD }

        // Build map of type -> charge gained from defeated Pokemon
        val gainedCharges = mutableMapOf<String, Int>()
        for (loser in losers) {
            for (bp in loser.pokemonList) {
                val pokemon = bp.effectedPokemon
                val level = pokemon.level
                val chargeAmount = (level * IncubatorConfig.geneFusionChargePerLevel).roundToInt()
                if (chargeAmount <= 0) continue

                for (type in pokemon.species.types) {
                    val typeName = type.name.lowercase()
                    gainedCharges[typeName] = (gainedCharges[typeName] ?: 0) + chargeAmount
                }
            }
        }
        if (gainedCharges.isEmpty()) return

        for (serverPlayer in serverPlayers) {
            val syringe = findSyringeInInventory(serverPlayer) ?: continue
            for ((type, amount) in gainedCharges) {
                StemCellSyringeItem.addCharge(syringe, type, amount)
            }
        }
    }

    private fun findSyringeInInventory(player: ServerPlayer): ItemStack? {
        for (stack in player.inventory.items) {
            if (stack.`is`(ModRegistries.STEM_CELL_SYRINGE.get())) return stack
        }
        return null
    }
}
