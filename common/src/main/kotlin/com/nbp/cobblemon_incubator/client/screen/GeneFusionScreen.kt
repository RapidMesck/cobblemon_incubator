package com.nbp.cobblemon_incubator.client.screen

import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.api.pokemon.stats.Stat
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.client.CobblemonResources
import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.util.cobblemonResource
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferUploader
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import com.nbp.cobblemon_incubator.CobblemonIncubator
import com.nbp.cobblemon_incubator.menu.GeneFusionMenu
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.client.sounds.SoundManager
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.phys.Vec2
import org.joml.Vector3f
import kotlin.math.roundToInt

class GeneFusionScreen(menu: GeneFusionMenu, inventory: Inventory, title: Component) :
    AbstractContainerScreen<GeneFusionMenu>(menu, inventory, title) {

    private lateinit var naturePrevButton: PcTextButton
    private lateinit var natureNextButton: PcTextButton
    private lateinit var abilityPrevButton: PcTextButton
    private lateinit var abilityNextButton: PcTextButton
    private lateinit var fuseButton: PcTextButton

    private val chartStats: List<Pair<String, Stat>> = listOf(
        "HP" to Stats.HP,
        "Atk" to Stats.ATTACK,
        "Def" to Stats.DEFENCE,
        "Spe" to Stats.SPEED,
        "SpD" to Stats.SPECIAL_DEFENCE,
        "SpA" to Stats.SPECIAL_ATTACK
    )

    init {
        imageWidth = BASE_WIDTH
        imageHeight = BASE_HEIGHT
        inventoryLabelX = 85
        inventoryLabelY = 111
        titleLabelX = 14
        titleLabelY = 12
    }

    override fun init() {
        super.init()
        val rightX = leftPos + 230

        naturePrevButton = addButton(rightX, topPos + 30, 14, 14, "<") {
            Minecraft.getInstance().gameMode?.handleInventoryButtonClick(menu.containerId, 1)
        }
        natureNextButton = addButton(rightX + 55, topPos + 30, 14, 14, ">") {
            Minecraft.getInstance().gameMode?.handleInventoryButtonClick(menu.containerId, 0)
        }
        abilityPrevButton = addButton(rightX, topPos + 50, 14, 14, "<") {
            Minecraft.getInstance().gameMode?.handleInventoryButtonClick(menu.containerId, 3)
        }
        abilityNextButton = addButton(rightX + 55, topPos + 50, 14, 14, ">") {
            Minecraft.getInstance().gameMode?.handleInventoryButtonClick(menu.containerId, 2)
        }
        fuseButton = addFuseButton(rightX, topPos + 75, 69, 20) {
            Minecraft.getInstance().gameMode?.handleInventoryButtonClick(menu.containerId, 4)
        }
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick)
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        renderTooltip(guiGraphics, mouseX, mouseY)

        fuseButton.active = menu.canFuse
    }

    override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        val left = leftPos
        val top = topPos

        blitk(
            matrixStack = guiGraphics.pose(),
            texture = backgroundResource,
            x = left,
            y = top,
            width = BASE_WIDTH,
            height = BASE_HEIGHT
        )

        if (menu.eggCount >= 2) {
            renderIvChart(guiGraphics, left, top)
        }

        val charge = menu.syringeCharge
        val required = menu.requiredCharge
        if (required > 0) {
            renderChargeBar(guiGraphics, left + 230, top + 100, 69, 6, charge, required)
        }

        val be = menu.getBlockEntity() ?: return
        val selectedNature = be.getSelectedNature()
        val selectedAbility = be.getSelectedAbility()

        drawTinyText(guiGraphics, Component.literal("Nature"), left + 230f, top + 18f, colour = 0xA0A0A0)
        drawSmallText(
            guiGraphics,
            Component.literal(if (selectedNature.isNotBlank()) displayName(selectedNature) else "None"),
            left + 230f, top + 48f, colour = if (selectedNature.isNotBlank()) 0xFFFFFF else 0x888888
        )

        drawTinyText(guiGraphics, Component.literal("Ability"), left + 230f, top + 37f, colour = 0xA0A0A0)
        drawSmallText(
            guiGraphics,
            Component.literal(if (selectedAbility.isNotBlank()) displayName(selectedAbility) else "None"),
            left + 230f, top + 67f, colour = if (selectedAbility.isNotBlank()) 0xFFFFFF else 0x888888
        )
    }

    override fun renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        drawPcText(guiGraphics, title, 14F, 12F, shadow = true)
        drawPcText(guiGraphics, playerInventoryTitle, 168F, 111F, centred = true, shadow = true)
    }

    private fun renderIvChart(guiGraphics: GuiGraphics, left: Int, top: Int) {
        val centerX = left + 44f
        val centerY = top + 65f
        val scale = 0.22f

        blitk(
            matrixStack = guiGraphics.pose(),
            texture = statsChartResource,
            x = centerX - 166f * scale / 2f,
            y = centerY - 192f * scale / 2f,
            width = 166,
            height = 192,
            scale = scale
        )

        val be = menu.getBlockEntity() ?: return
        val ivs = be.getPreviewIvs() ?: return
        val radius = 13f

        drawIvPolygon(
            centerX = centerX,
            centerY = centerY,
            radius = radius,
            ratios = chartStats.map { (_, stat) -> ivs.getOrDefault(stat) / 31F },
            colour = Vector3f(0f, 216F / 255F, 100F / 255F)
        )

        val values = chartStats.map { (_, stat) -> ivs.getOrDefault(stat).toString() }
        val positions = listOf(
            Vec2(centerX, centerY - 22F),
            Vec2(centerX + 22F, centerY - 6F),
            Vec2(centerX + 22F, centerY + 10F),
            Vec2(centerX, centerY + 23F),
            Vec2(centerX - 22F, centerY + 10F),
            Vec2(centerX - 22F, centerY - 6F)
        )
        values.forEachIndexed { index, value ->
            drawTinyText(guiGraphics, Component.literal(chartStats[index].first), positions[index].x, positions[index].y - 5F, centred = true, colour = 0xD8D8D8)
            drawTinyText(guiGraphics, Component.literal(value), positions[index].x, positions[index].y + 1F, centred = true)
        }
    }

    private fun renderChargeBar(guiGraphics: GuiGraphics, x: Int, y: Int, width: Int, height: Int, charge: Int, required: Int) {
        val ratio = (charge.toFloat() / required.coerceAtLeast(1)).coerceAtMost(1f)
        val filled = (width * ratio).toInt()

        guiGraphics.fill(x, y, x + width, y + height, 0xFF3A3A3A.toInt())
        val color = when {
            ratio >= 1f -> 0xFF55FF55.toInt()
            ratio >= 0.5f -> 0xFFFFFF55.toInt()
            else -> 0xFFFF5555.toInt()
        }
        if (filled > 0) {
            guiGraphics.fill(x, y, x + filled, y + height, color)
        }
        guiGraphics.renderOutline(x, y, width, height, 0xFF888888.toInt())
    }

    private fun addButton(x: Int, y: Int, width: Int, height: Int, label: String, onPress: () -> Unit): PcTextButton {
        return addRenderableWidget(PcTextButton(x, y, width, height, Component.literal(label), onPress))
    }

    private fun addFuseButton(x: Int, y: Int, width: Int, height: Int, onPress: () -> Unit): PcTextButton {
        return addRenderableWidget(PcTextButton(x, y, width, height, Component.translatable("gui.cobblemon_incubator.gene_fusion.fuse"), onPress))
    }

    private fun chartPoints(centerX: Float, centerY: Float, radius: Float): List<Vec2> {
        val startAngle = -90.0
        val step = 360.0 / chartStats.size
        return List(chartStats.size) { index ->
            val angle = Math.toRadians(startAngle + index * step)
            Vec2(
                centerX + radius * Math.cos(angle).toFloat(),
                centerY + radius * Math.sin(angle).toFloat()
            )
        }
    }

    private fun drawIvPolygon(centerX: Float, centerY: Float, radius: Float, ratios: List<Float>, colour: Vector3f) {
        val maxPoints = chartPoints(centerX, centerY, radius)
        val coercedRatios = ratios.map { it.coerceIn(5F / radius, 1F) }
        val vertices = maxPoints.mapIndexed { index, maxPoint ->
            Vec2(
                centerX + (maxPoint.x - centerX) * coercedRatios[index],
                centerY + (maxPoint.y - centerY) * coercedRatios[index]
            )
        }
        val center = Vec2(centerX, centerY)

        RenderSystem.disableDepthTest()
        for (index in vertices.indices) {
            drawTriangle(colour, vertices[index], center, vertices[(index + 1) % vertices.size])
        }
        RenderSystem.enableDepthTest()
    }

    private fun drawTriangle(colour: Vector3f, v1: Vec2, v2: Vec2, v3: Vec2, opacity: Float = 0.6F) {
        CobblemonResources.WHITE.let { RenderSystem.setShaderTexture(0, it) }
        RenderSystem.setShaderColor(colour.x, colour.y, colour.z, opacity)
        val bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION)
        bufferBuilder.addVertex(v1.x, v1.y, 10F)
        bufferBuilder.addVertex(v2.x, v2.y, 10F)
        bufferBuilder.addVertex(v3.x, v3.y, 10F)
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow())
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F)
    }

    private fun displayName(value: String?): String {
        val clean = value?.substringAfter(':')?.substringAfterLast('/') ?: return "Any"
        return clean.split('-', '_', ' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { word -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
    }

    private fun drawPcText(
        guiGraphics: GuiGraphics,
        text: Component,
        x: Float,
        y: Float,
        centred: Boolean = false,
        shadow: Boolean = false,
        colour: Int = 0xFFFFFF,
        scale: Float = 0.85F
    ) {
        val drawX = if (centred) x.roundToInt() - font.width(text) / 2 else x.roundToInt()
        guiGraphics.drawString(font, text, drawX, y.roundToInt(), colour, shadow)
    }

    private fun drawSmallText(
        guiGraphics: GuiGraphics,
        text: Component,
        x: Float,
        y: Float,
        centred: Boolean = false,
        colour: Int = 0xFFFFFF,
        scale: Float = 0.65F
    ) {
        val pose = guiGraphics.pose()
        val drawX = if (centred) x - (font.width(text) * scale / 2F) else x
        pose.pushPose()
        pose.scale(scale, scale, 1F)
        guiGraphics.drawString(font, text, (drawX / scale).roundToInt(), (y / scale).roundToInt(), colour, false)
        pose.popPose()
    }

    private fun drawTinyText(
        guiGraphics: GuiGraphics,
        text: Component,
        x: Float,
        y: Float,
        centred: Boolean = false,
        colour: Int = 0xFFFFFF,
        scale: Float = 0.48F
    ) {
        val pose = guiGraphics.pose()
        val drawX = if (centred) x - (font.width(text) * scale / 2F) else x
        pose.pushPose()
        pose.scale(scale, scale, 1F)
        guiGraphics.drawString(font, text, (drawX / scale).roundToInt(), (y / scale).roundToInt(), colour, false)
        pose.popPose()
    }

    private class PcTextButton(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        message: Component,
        private val onPressAction: () -> Unit
    ) : Button(x, y, width, height, message, { onPressAction() }, DEFAULT_NARRATION) {
        var highlighted = false

        override fun renderWidget(context: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
            val base = when {
                highlighted && isHovered() -> 0xFF78967B.toInt()
                highlighted -> 0xFF627D65.toInt()
                isHovered() -> 0xFF858585.toInt()
                else -> 0xFF6F6F6F.toInt()
            }
            val top = if (active) 0xFFB7B7B7.toInt() else 0xFF777777.toInt()
            val bottom = if (active) 0xFF393939.toInt() else 0xFF2E2E2E.toInt()
            context.fill(x, y, x + width, y + height, base)
            context.fill(x, y, x + width, y + 1, top)
            context.fill(x, y, x + 1, y + height, top)
            context.fill(x, y + height - 1, x + width, y + height, bottom)
            context.fill(x + width - 1, y, x + width, y + height, bottom)

            val font = Minecraft.getInstance().font
            val colour = if (active) 0xFFFFFF else 0xBDBDBD
            val textX = x + width / 2 - font.width(message) / 2
            context.drawString(font, message, textX, y + (height - 8) / 2, colour, true)
        }

        override fun playDownSound(soundManager: SoundManager) {
            soundManager.play(SimpleSoundInstance.forUI(CobblemonSounds.PC_CLICK, 1F))
        }
    }

    companion object {
        private const val BASE_WIDTH = 349
        private const val BASE_HEIGHT = 205

        private fun incubatorResource(path: String): ResourceLocation {
            return ResourceLocation.fromNamespaceAndPath(CobblemonIncubator.MOD_ID, path)
        }

        private val backgroundResource = incubatorResource("textures/gui/gene_fusion.png")
        private val statsChartResource = cobblemonResource("textures/gui/summary/summary_stats_chart.png")
    }
}
