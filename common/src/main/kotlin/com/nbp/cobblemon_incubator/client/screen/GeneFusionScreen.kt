package com.nbp.cobblemon_incubator.client.screen

import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.api.pokemon.stats.Stat
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.client.CobblemonResources
import com.cobblemon.mod.common.client.gui.summary.widgets.ModelWidget
import com.cobblemon.mod.common.util.cobblemonResource
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferUploader
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import com.nbp.cobblemon_incubator.CobblemonIncubator
import com.nbp.cobblemon_incubator.blockentity.GeneFusionBlockEntity
import com.nbp.cobblemon_incubator.item.StemCellSyringeItem
import com.nbp.cobblemon_incubator.menu.GeneFusionMenu
import com.nbp.cobblemon_incubator.registry.ModRegistries
import com.nbp.cobblemon_incubator.util.CobbreedingCompat
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

    private lateinit var naturePrevButton: ArrowButton
    private lateinit var natureNextButton: ArrowButton
    private lateinit var abilityPrevButton: ArrowButton
    private lateinit var abilityNextButton: ArrowButton
    private lateinit var fuseButton: PcTextButton

    private var modelWidget: ModelWidget? = null
    private var modelKey: String? = null

    private val chartStats: List<Pair<String, Stat>> = listOf(
        "HP" to Stats.HP,
        "Atk" to Stats.ATTACK,
        "Def" to Stats.DEFENCE,
        "Spe" to Stats.SPEED,
        "SpD" to Stats.SPECIAL_DEFENCE,
        "SpA" to Stats.SPECIAL_ATTACK
    )

    init {
        imageWidth = TEXTURE_WIDTH
        imageHeight = TEXTURE_HEIGHT
        inventoryLabelX = 54
        inventoryLabelY = 110
        titleLabelX = 135
        titleLabelY = 12
    }

    override fun init() {
        super.init()

        val bgX = leftPos - X_OFFSET
        naturePrevButton =
            ArrowButton(bgX + NATURE_ARROW_X, topPos + NATURE_ARROW_Y, 11, 12, Component.literal("<"), Button.OnPress {
                Minecraft.getInstance().gameMode?.handleInventoryButtonClick(menu.containerId, 1)
            }).also { addRenderableWidget(it) }
        natureNextButton = ArrowButton(
            bgX + NATURE_ARROW_RIGHT_X,
            topPos + NATURE_ARROW_Y,
            11,
            12,
            Component.literal(">"),
            Button.OnPress {
                Minecraft.getInstance().gameMode?.handleInventoryButtonClick(menu.containerId, 0)
            }).also { addRenderableWidget(it) }
        abilityPrevButton = ArrowButton(
            bgX + ABILITY_ARROW_X,
            topPos + ABILITY_ARROW_Y,
            11,
            12,
            Component.literal("<"),
            Button.OnPress {
                Minecraft.getInstance().gameMode?.handleInventoryButtonClick(menu.containerId, 3)
            }).also { addRenderableWidget(it) }
        abilityNextButton = ArrowButton(
            bgX + ABILITY_ARROW_RIGHT_X,
            topPos + ABILITY_ARROW_Y,
            11,
            12,
            Component.literal(">"),
            Button.OnPress {
                Minecraft.getInstance().gameMode?.handleInventoryButtonClick(menu.containerId, 2)
            }).also { addRenderableWidget(it) }

        fuseButton = PcTextButton(
            bgX + FUSE_BUTTON_X, topPos + FUSE_BUTTON_Y, FUSE_BUTTON_WIDTH, FUSE_BUTTON_HEIGHT,
            Component.translatable("gui.cobblemon_incubator.gene_fusion.fuse"),
            Button.OnPress {
                Minecraft.getInstance().gameMode?.handleInventoryButtonClick(menu.containerId, 4)
            }
        ).also { addRenderableWidget(it) }
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        menu.blockPos?.let { pos ->
            val blockEntity = Minecraft.getInstance().level?.getBlockEntity(pos) as? GeneFusionBlockEntity
            menu.clientEggPropertiesOverride = blockEntity?.clientEggPropertiesList()
        }

        renderBackground(guiGraphics, mouseX, mouseY, partialTick)
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        renderTooltip(guiGraphics, mouseX, mouseY)

        val canFuse = menu.fusionStatus == 1
        fuseButton.visible = canFuse
        fuseButton.active = canFuse

        val hasEgg = menu.eggCount >= 1
        val natureOptions = if (hasEgg) menu.availableNatures() else emptyList()
        val abilityOptions = if (hasEgg) menu.availableAbilities() else emptyList()
        naturePrevButton.visible = natureOptions.size > 1
        natureNextButton.visible = natureOptions.size > 1
        abilityPrevButton.visible = abilityOptions.size > 1
        abilityNextButton.visible = abilityOptions.size > 1
    }

    override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        val properties = eggProperties()
        updateModelWidget(properties)

        val bgX = leftPos - X_OFFSET
        val bgY = topPos

        blitk(
            matrixStack = guiGraphics.pose(),
            texture = backgroundResource,
            x = bgX,
            y = bgY,
            width = TEXTURE_WIDTH,
            height = TEXTURE_HEIGHT,
            textureWidth = TEXTURE_WIDTH,
            textureHeight = TEXTURE_HEIGHT
        )

        blitk(
            matrixStack = guiGraphics.pose(),
            texture = portraitBackgroundResource,
            x = bgX + PORTRAIT_X,
            y = bgY + PORTRAIT_Y,
            width = PORTRAIT_SIZE,
            height = PORTRAIT_SIZE
        )

        modelWidget?.render(guiGraphics, mouseX, mouseY, partialTick)

        renderDnaProgress(guiGraphics)
    }

    override fun renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        drawPcText(guiGraphics, title, titleLabelX.toFloat(), titleLabelY.toFloat(), centered = true, shadow = true)
        drawPcText(guiGraphics, playerInventoryTitle, 133F, 111F, centered = true, shadow = true)

        renderChargeInfo(guiGraphics)

        val properties = eggProperties()
        if (properties == null) {
            drawPcText(guiGraphics, Component.literal("No Egg"), -33F, 12F, shadow = true)
        } else {
            drawPcText(guiGraphics, Component.literal(displayName(properties.species)), -33F, 12F, shadow = true)
        }

        renderSelectors(guiGraphics)

        val previewIvs = menu.previewIvs()
        if (previewIvs != null) {
            renderIvChart(guiGraphics, previewIvs)
        } else if (eggProperties() != null) {
            drawPcText(
                guiGraphics,
                Component.literal("IVs unknown"),
                IV_CHART_CENTER_X,
                IV_CHART_CENTER_Y + 18F,
                centered = true,
                colour = 0xB8B8B8
            )
        }
    }

    private fun renderSelectors(guiGraphics: GuiGraphics) {
        if (menu.eggCount < 1) return

        val selectedNature = menu.getSelectedNature()
        val natureText = if (selectedNature.isNotEmpty()) displayName(selectedNature) else "Any"
        val natureColor =
            if (selectedNature.isNotEmpty() && menu.availableNatures().isNotEmpty()) 0xFFFFFF else 0xB8B8B8

        val selectedAbility = menu.getSelectedAbility()
        val abilityText = if (selectedAbility.isNotEmpty()) displayName(selectedAbility) else "Any"
        val abilityColor =
            if (selectedAbility.isNotEmpty() && menu.availableAbilities().isNotEmpty()) 0xFFFFFF else 0xB8B8B8

        drawSmallText(guiGraphics, Component.literal("Nature"), NATURE_TEXT_X, NATURE_LABEL_Y, centered = true)
        drawSmallText(
            guiGraphics,
            Component.literal(natureText.fit(13)),
            NATURE_TEXT_X,
            NATURE_VALUE_Y,
            centered = true,
            colour = natureColor
        )

        drawSmallText(guiGraphics, Component.literal("Ability"), ABILITY_TEXT_X, ABILITY_LABEL_Y, centered = true)
        drawSmallText(
            guiGraphics,
            Component.literal(abilityText.fit(13)),
            ABILITY_TEXT_X,
            ABILITY_VALUE_Y,
            centered = true,
            colour = abilityColor
        )
    }

    private fun renderDnaProgress(guiGraphics: GuiGraphics) {
        val required = menu.requiredCharge
        if (required <= 0) return

        val charge = menu.syringeCharge
        val progress = (charge.toFloat() / required).coerceIn(0F, 1F)
        val frame = (progress * (DNA_FRAMES - 1)).roundToInt()
        val bgX = leftPos - X_OFFSET

        blitk(
            matrixStack = guiGraphics.pose(),
            texture = dnaFramesResource,
            x = bgX + DNA_PROGRESS_X,
            y = topPos + DNA_PROGRESS_Y,
            width = DNA_PROGRESS_WIDTH,
            height = DNA_PROGRESS_HEIGHT,
            vOffset = frame * DNA_PROGRESS_HEIGHT,
            textureWidth = DNA_PROGRESS_WIDTH,
            textureHeight = DNA_PROGRESS_HEIGHT * DNA_FRAMES
        )
    }

    private fun renderChargeInfo(guiGraphics: GuiGraphics) {
        val required = menu.requiredCharge
        val types = eggTypes() ?: return

        val syringe = menu.slots[GeneFusionBlockEntity.SLOT_SYRINGE].item
        val hasSyringe = syringe.`is`(ModRegistries.STEM_CELL_SYRINGE.get())
        val allCharges = if (hasSyringe) StemCellSyringeItem.getCharges(syringe) else emptyMap()

        val parts = types.map { type ->
            val charge = allCharges[type] ?: 0
            val reqText = if (required > 0) " / $required" else ""
            "${StemCellSyringeItem.displayName(type)}: $charge$reqText"
        }
        val line = parts.joinToString("  ")
        val color = if (required > 0 && types.all { (allCharges[it] ?: 0) >= required }) 0x55FF55 else 0xFF5555

        drawSmallText(
            guiGraphics,
            Component.literal(line),
            titleLabelX.toFloat(),
            titleLabelY + 20F,
            centered = true,
            colour = color
        )
    }

    private fun eggTypes(): List<String>? {
        val props = menu.clientEggPropertiesOverride?.firstOrNull { it != null }
            ?: (GeneFusionBlockEntity.SLOT_EGG_START..GeneFusionBlockEntity.SLOT_EGG_END)
                .mapNotNull { CobbreedingCompat.extractProperties(menu.container.getItem(it)) }
                .firstOrNull()
        val speciesId = props?.species ?: return null
        val species = PokemonSpecies.species.firstOrNull {
            it.resourceIdentifier.toString().equals(speciesId, ignoreCase = true) ||
                    it.resourceIdentifier.path.equals(speciesId.substringAfter(':'), ignoreCase = true)
        } ?: return null
        return species.types.map { it.name.lowercase() }
    }

    private fun renderIvChart(guiGraphics: GuiGraphics, ivs: com.cobblemon.mod.common.pokemon.IVs) {
        val centerX = IV_CHART_CENTER_X
        val centerY = IV_CHART_CENTER_Y
        val scale = 0.24F
        val textureX = centerX - (166F * scale / 2F)
        val textureY = centerY - (192F * scale / 2F)
        blitk(
            matrixStack = guiGraphics.pose(),
            texture = statsChartResource,
            x = textureX / scale,
            y = textureY / scale,
            width = 166,
            height = 192,
            scale = scale
        )

        val radius = 14.4F
        drawIvPolygon(
            centerX = leftPos + centerX,
            centerY = topPos + centerY,
            radius = radius,
            ratios = chartStats.map { (_, stat) -> ivs.getOrDefault(stat) / 31F },
            colour = Vector3f(216F / 255F, 100F / 255F, 1F)
        )

        val values = chartStats.map { (_, stat) -> ivs.getOrDefault(stat).toString() }
        val positions = listOf(
            Vec2(centerX, centerY - 24F),
            Vec2(centerX + 24F, centerY - 7F),
            Vec2(centerX + 24F, centerY + 11F),
            Vec2(centerX, centerY + 25F),
            Vec2(centerX - 24F, centerY + 11F),
            Vec2(centerX - 24F, centerY - 7F)
        )
        values.forEachIndexed { index, value ->
            drawTinyText(
                guiGraphics,
                Component.literal(chartStats[index].first),
                positions[index].x,
                positions[index].y - 5F,
                centered = true,
                colour = 0xD8D8D8
            )
            drawTinyText(
                guiGraphics,
                Component.literal(value),
                positions[index].x,
                positions[index].y + 1F,
                centered = true
            )
        }
    }

    private fun updateModelWidget(properties: PokemonProperties?) {
        val key = properties?.asString(" ")
        if (key == modelKey) return
        modelKey = key

        val renderable = runCatching { properties?.create()?.asRenderablePokemon() }.getOrNull()
            ?: runCatching { properties?.asRenderablePokemon() }.getOrNull()

        modelWidget = renderable?.let {
            ModelWidget(
                pX = leftPos - X_OFFSET + PORTRAIT_X,
                pY = topPos + PORTRAIT_Y,
                pWidth = PORTRAIT_SIZE,
                pHeight = PORTRAIT_SIZE,
                pokemon = it,
                baseScale = 2.0F,
                rotationY = 325F,
                offsetY = -10.0,
                playCryOnClick = false,
                shouldFollowCursor = true
            )
        }
    }

    private fun eggProperties(): PokemonProperties? {
        return menu.clientEggPropertiesOverride?.firstOrNull { it != null }
            ?: (GeneFusionBlockEntity.SLOT_EGG_START..GeneFusionBlockEntity.SLOT_EGG_END)
                .mapNotNull { CobbreedingCompat.extractProperties(menu.container.getItem(it)) }
                .firstOrNull()
    }

    private fun displayName(value: String?): String {
        val clean = value?.substringAfter(':')?.substringAfterLast('/') ?: return "Unknown"
        return clean.split('-', '_', ' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { word -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
    }

    private fun String.fit(max: Int): String {
        return if (length <= max) this else take(max - 1) + "."
    }

    private fun drawPcText(
        guiGraphics: GuiGraphics,
        text: Component,
        x: Float,
        y: Float,
        centered: Boolean = false,
        shadow: Boolean = false,
        colour: Int = 0xFFFFFF
    ) {
        val drawX = if (centered) x.roundToInt() - font.width(text) / 2 else x.roundToInt()
        guiGraphics.drawString(font, text, drawX, y.roundToInt(), colour, shadow)
    }

    private fun drawSmallText(
        guiGraphics: GuiGraphics,
        text: Component,
        x: Float,
        y: Float,
        centered: Boolean = false,
        colour: Int = 0xFFFFFF,
        scale: Float = 0.65F
    ) {
        val pose = guiGraphics.pose()
        val drawX = if (centered) x - (font.width(text) * scale / 2F) else x
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
        centered: Boolean = false,
        colour: Int = 0xFFFFFF,
        scale: Float = 0.48F
    ) {
        val pose = guiGraphics.pose()
        val drawX = if (centered) x - (font.width(text) * scale / 2F) else x
        pose.pushPose()
        pose.scale(scale, scale, 1F)
        guiGraphics.drawString(font, text, (drawX / scale).roundToInt(), (y / scale).roundToInt(), colour, false)
        pose.popPose()
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

    private class ArrowButton(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        message: Component,
        onPress: OnPress
    ) : Button(x, y, width, height, message, onPress, DEFAULT_NARRATION) {
        override fun renderWidget(context: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
            val font = Minecraft.getInstance().font
            val colour = when {
                !active -> 0x666666
                isHovered() -> 0xFFFFFF
                else -> 0xB8B8B8
            }
            val textX = x + width / 2 - font.width(message) / 2
            context.drawString(font, message, textX, y + 3, colour, false)
        }

        override fun playDownSound(soundManager: SoundManager) {
            soundManager.play(SimpleSoundInstance.forUI(CobblemonSounds.PC_CLICK, 1F))
        }
    }

    private class PcTextButton(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        message: Component,
        onPress: OnPress
    ) : Button(x, y, width, height, message, onPress, DEFAULT_NARRATION) {
        override fun renderWidget(context: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
            val base = when {
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
            context.drawString(font, message, textX, y + 5, colour, true)
        }

        override fun playDownSound(soundManager: SoundManager) {
            soundManager.play(SimpleSoundInstance.forUI(CobblemonSounds.PC_CLICK, 1F))
        }
    }

    companion object {
        private const val TEXTURE_WIDTH = 269
        private const val TEXTURE_HEIGHT = 205
        private const val X_OFFSET = 38

        private const val PORTRAIT_X = 6
        private const val PORTRAIT_Y = 27
        private const val PORTRAIT_SIZE = 66

        private const val NATURE_ARROW_X = 9
        private const val NATURE_ARROW_RIGHT_X = 59
        private const val NATURE_ARROW_Y = 100
        private const val ABILITY_ARROW_X = 9
        private const val ABILITY_ARROW_RIGHT_X = 59
        private const val ABILITY_ARROW_Y = 120

        private const val NATURE_LABEL_Y = 96F
        private const val NATURE_VALUE_Y = 105F
        private const val ABILITY_LABEL_Y = 116F
        private const val ABILITY_VALUE_Y = 125F
        private const val NATURE_TEXT_X = 0F
        private const val ABILITY_TEXT_X = 0F

        private const val IV_CHART_CENTER_X = 4F
        private const val IV_CHART_CENTER_Y = 163.7F

        private const val DNA_PROGRESS_X = 152
        private const val DNA_PROGRESS_Y = 44
        private const val DNA_PROGRESS_WIDTH = 52
        private const val DNA_PROGRESS_HEIGHT = 60
        private const val DNA_FRAMES = 10

        private const val FUSE_BUTTON_X = 206
        private const val FUSE_BUTTON_Y = 66
        private const val FUSE_BUTTON_WIDTH = 40
        private const val FUSE_BUTTON_HEIGHT = 16

        private fun incubatorResource(path: String): ResourceLocation {
            return ResourceLocation.fromNamespaceAndPath(CobblemonIncubator.MOD_ID, path)
        }

        private val backgroundResource = incubatorResource("textures/gui/gene_fusion.png")
        private val portraitBackgroundResource = incubatorResource("textures/gui/portrait_background.png")
        private val dnaFramesResource = incubatorResource("textures/gui/dna_frame.png")
        private val statsChartResource = cobblemonResource("textures/gui/summary/summary_stats_chart.png")
    }
}
