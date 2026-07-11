package com.nbp.cobblemon_incubator.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.nbp.cobblemon_incubator.config.IncubatorConfig
import com.nbp.cobblemon_incubator.item.StemCellSyringeItem
import com.nbp.cobblemon_incubator.registry.ModRegistries
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

object FillSyringeCommand {

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("fillsyringe")
                .requires { it.hasPermission(2) }
                .then(
                    Commands.argument("type", StringArgumentType.word())
                        .then(
                            Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes { ctx ->
                                    val type = StringArgumentType.getString(ctx, "type").lowercase()
                                    val amount = IntegerArgumentType.getInteger(ctx, "amount")
                                    val player = ctx.source.playerOrException
                                    fillSyringe(player, type, amount)
                                    1
                                }
                        )
                )
                .executes { ctx ->
                    val player = ctx.source.playerOrException
                    fillAllSyringes(player)
                    1
                }
        )
    }

    private fun fillSyringe(player: ServerPlayer, type: String, amount: Int) {
        val max = IncubatorConfig.geneFusionSyringeMaxCharge
        val clamped = amount.coerceIn(1, max)
        var filled = false

        for (stack in player.inventory.items) {
            if (!stack.`is`(ModRegistries.STEM_CELL_SYRINGE.get())) continue
            StemCellSyringeItem.addCharge(stack, type, clamped)
            filled = true
        }

        if (filled) {
            player.sendSystemMessage(
                Component.translatable("command.cobblemon_incubator.fillsyringe.type", type, clamped)
            )
        } else {
            player.sendSystemMessage(
                Component.translatable("command.cobblemon_incubator.fillsyringe.no_syringe")
            )
        }
    }

    private fun fillAllSyringes(player: ServerPlayer) {
        val max = IncubatorConfig.geneFusionSyringeMaxCharge
        var filled = false

        val allTypes = listOf(
            "normal", "fire", "water", "electric", "grass", "ice",
            "fighting", "poison", "ground", "flying", "psychic", "bug",
            "rock", "ghost", "dragon", "dark", "steel", "fairy"
        )

        for (stack in player.inventory.items) {
            if (!stack.`is`(ModRegistries.STEM_CELL_SYRINGE.get())) continue
            for (type in allTypes) {
                StemCellSyringeItem.addCharge(stack, type, max)
            }
            filled = true
        }

        if (filled) {
            player.sendSystemMessage(
                Component.translatable("command.cobblemon_incubator.fillsyringe.all", max)
            )
        } else {
            player.sendSystemMessage(
                Component.translatable("command.cobblemon_incubator.fillsyringe.no_syringe")
            )
        }
    }
}
