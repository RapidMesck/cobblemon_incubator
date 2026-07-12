package com.nbp.cobblemon_incubator.client.screen

import com.cobblemon.mod.common.api.abilities.Abilities
import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.api.pokemon.Natures
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
import com.nbp.cobblemon_incubator.blockentity.EggIncubatorBlockEntity
import com.nbp.cobblemon_incubator.menu.EggIncubatorMenu
import com.nbp.cobblemon_incubator.util.CobbreedingCompat
import com.nbp.cobblemon_incubator.util.FilterConfig
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.client.sounds.SoundManager
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.phys.Vec2
import org.joml.Vector3f
import kotlin.math.roundToInt

class EggIncubatorScreen(menu: EggIncubatorMenu, inventory: Inventory, title: Component) :
    AbstractContainerScreen<EggIncubatorMenu>(menu, inventory, title) {

    private data class SelectOption(val index: Int, val value: String?, val label: String)
    private enum class OpenSelect { NONE, SPECIES, NATURE, ABILITY }

    private var selectedIvIndex = 0
    private var modelWidget: ModelWidget? = null
    private var modelKey: String? = null
    private var openSelect = OpenSelect.NONE

    private val filterButtons = mutableListOf<PcTextButton>()
    private val speciesButtons = mutableListOf<PcTextButton>()
    private val natureButtons = mutableListOf<PcTextButton>()
    private val abilityButtons = mutableListOf<PcTextButton>()

    private lateinit var clearButton: PcTextButton
    private lateinit var speciesButton: PcTextButton
    private lateinit var natureSelectButton: PcTextButton
    private lateinit var abilitySelectButton: PcTextButton
    private lateinit var rejectButton: PcTextButton
    private lateinit var ivPrevButton: PcTextButton
    private lateinit var ivNextButton: PcTextButton
    private lateinit var ivOperatorButton: PcTextButton
    private lateinit var ivDownButton: PcTextButton
    private lateinit var ivUpButton: PcTextButton
    private lateinit var ivClearButton: PcTextButton
    private lateinit var speciesSearch: EditBox
    private lateinit var natureSearch: EditBox
    private lateinit var abilitySearch: EditBox

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
        inventoryLabelX = 93
        inventoryLabelY = 96
        titleLabelX = 126
        titleLabelY = 12
    }

    override fun init() {
        super.init()
        filterButtons.clear()
        speciesButtons.clear()
        natureButtons.clear()
        abilityButtons.clear()

        val filterX = leftPos + 274
        clearButton = addFilterButton(filterX, topPos + 31, 32, 14, 0)
        rejectButton = addFilterButton(filterX + 35, topPos + 31, 34, 14, 6)
        speciesButton = addLocalButton(filterX, topPos + 50, 69, 14) {
            openSelect = if (openSelect == OpenSelect.SPECIES) OpenSelect.NONE else OpenSelect.SPECIES
            if (openSelect == OpenSelect.SPECIES) speciesSearch.setFocused(true)
        }
        speciesSearch = addSearchBox(filterX, topPos + 50, "Species")
        repeat(7) { row ->
            speciesButtons += addDynamicFilterButton(filterX, topPos + 64 + row * 14, 69, 13) {
                speciesOptions().getOrNull(row)?.let {
                    openSelect = OpenSelect.NONE
                    speciesSearch.setFocused(false)
                    4000 + it.index
                } ?: -1
            }
        }

        natureSelectButton = addLocalButton(filterX, topPos + 69, 69, 14) {
            openSelect = if (openSelect == OpenSelect.NATURE) OpenSelect.NONE else OpenSelect.NATURE
            if (openSelect == OpenSelect.NATURE) natureSearch.setFocused(true)
        }
        natureSearch = addSearchBox(filterX, topPos + 50, "Nature")
        repeat(7) { row ->
            natureButtons += addDynamicFilterButton(filterX, topPos + 64 + row * 14, 69, 13) {
                natureOptions().getOrNull(row)?.let {
                    openSelect = OpenSelect.NONE
                    natureSearch.setFocused(false)
                    2000 + it.index
                } ?: -1
            }
        }

        abilitySelectButton = addLocalButton(filterX, topPos + 88, 69, 14) {
            openSelect = if (openSelect == OpenSelect.ABILITY) OpenSelect.NONE else OpenSelect.ABILITY
            if (openSelect == OpenSelect.ABILITY) abilitySearch.setFocused(true)
        }
        abilitySearch = addSearchBox(filterX, topPos + 50, "Ability")
        repeat(7) { row ->
            abilityButtons += addDynamicFilterButton(filterX, topPos + 64 + row * 14, 69, 13) {
                abilityOptions().getOrNull(row)?.let {
                    openSelect = OpenSelect.NONE
                    abilitySearch.setFocused(false)
                    3000 + it.index
                } ?: -1
            }
        }

        ivPrevButton = addLocalButton(filterX, topPos + 121, 14, 14) {
            selectedIvIndex = Math.floorMod(selectedIvIndex - 1, FilterConfig.STATS.size)
        }
        ivNextButton = addLocalButton(filterX + 55, topPos + 121, 14, 14) {
            selectedIvIndex = Math.floorMod(selectedIvIndex + 1, FilterConfig.STATS.size)
        }
        ivOperatorButton = addDynamicFilterButton(filterX, topPos + 158, 23, 14) { 100 + selectedIvIndex }
        ivDownButton = addDynamicFilterButton(filterX + 26, topPos + 158, 14, 14) { 110 + selectedIvIndex }
        ivUpButton = addDynamicFilterButton(filterX + 43, topPos + 158, 14, 14) { 120 + selectedIvIndex }
        ivClearButton = addDynamicFilterButton(filterX + 60, topPos + 158, 9, 14) { 130 + selectedIvIndex }
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        refreshDynamicControls()
        renderBackground(guiGraphics, mouseX, mouseY, partialTick)
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        renderTooltip(guiGraphics, mouseX, mouseY)
    }

    override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        val left = leftPos
        val top = topPos
        val matrices = guiGraphics.pose()

        val properties = eggProperties()
        updateModelWidget(properties)

        blitk(
            matrixStack = matrices,
            texture = backgroundResource,
            x = left,
            y = top,
            width = BASE_WIDTH,
            height = BASE_HEIGHT
        )
        if (menu.showPokemonModel) {
            blitk(
                matrixStack = matrices,
                texture = portraitBackgroundResource,
                x = left + PORTRAIT_X,
                y = top + PORTRAIT_Y,
                width = PORTRAIT_SIZE,
                height = PORTRAIT_SIZE
            )
            modelWidget?.render(guiGraphics, mouseX, mouseY, partialTick)
        }

        renderEggProgress(guiGraphics, left, top)
        renderFilterSearchFrames(guiGraphics)
    }

    override fun renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        drawPcText(guiGraphics, title, 172.5F, 12F, centered = true, shadow = true)
        drawPcText(guiGraphics, Component.literal("Filter"), 283.5F, 12F, centered = true, shadow = true)
        drawPcText(guiGraphics, playerInventoryTitle, 172F, 111F, centered = true, shadow = true)

        renderEggInfo(guiGraphics)
        renderFilterLabels(guiGraphics)
    }

    private fun renderEggProgress(guiGraphics: GuiGraphics, left: Int, top: Int) {
        if (menu.inputStack.isEmpty) return

        val max = menu.maxTimer.coerceAtLeast(1)
        val progress = (max - menu.remainingTimer).coerceIn(0, max) / max.toFloat()
        val frame = (progress * (EGG_PROGRESS_FRAMES - 1)).roundToInt()

        blitk(
            matrixStack = guiGraphics.pose(),
            texture = eggProgressResource,
            x = left + EGG_PROGRESS_X,
            y = top + EGG_PROGRESS_Y,
            width = EGG_PROGRESS_WIDTH,
            height = EGG_PROGRESS_HEIGHT,
            vOffset = frame * EGG_PROGRESS_HEIGHT,
            textureWidth = EGG_PROGRESS_WIDTH,
            textureHeight = EGG_PROGRESS_HEIGHT * EGG_PROGRESS_FRAMES
        )
    }

    private fun renderFilterSearchFrames(guiGraphics: GuiGraphics) {
        if (speciesSearch.visible) drawSearchFrame(
            guiGraphics,
            speciesSearch.x,
            speciesSearch.y,
            speciesSearch.width,
            speciesSearch.height
        )
        if (natureSearch.visible) drawSearchFrame(
            guiGraphics,
            natureSearch.x,
            natureSearch.y,
            natureSearch.width,
            natureSearch.height
        )
        if (abilitySearch.visible) drawSearchFrame(
            guiGraphics,
            abilitySearch.x,
            abilitySearch.y,
            abilitySearch.width,
            abilitySearch.height
        )
    }

    private fun drawSearchFrame(guiGraphics: GuiGraphics, x: Int, y: Int, width: Int, height: Int) {
        guiGraphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF2E3438.toInt())
        guiGraphics.fill(x, y, x + width, y + height, 0xFF14191E.toInt())
    }

    private fun renderEggInfo(guiGraphics: GuiGraphics) {
        val properties = eggProperties()
        if (properties == null) {
            drawPcText(guiGraphics, Component.literal("No Egg"), 5F, 12F, shadow = true)
            return
        }

        if (menu.showSpecies) {
            drawPcText(guiGraphics, Component.literal(displayName(properties.species).fit(14)), 5F, 12F, shadow = true)
        }
        if (menu.showNature) {
            drawSmallText(guiGraphics, Component.literal("Nature"), 38.5F, 98F, centered = true)
            drawSmallText(
                guiGraphics,
                Component.literal(displayName(properties.nature).fit(13)),
                38.5F,
                105F,
                centered = true
            )
        }
        if (menu.showAbility) {
            drawSmallText(guiGraphics, Component.literal("Ability"), 38.5F, 115F, centered = true)
            drawSmallText(
                guiGraphics,
                Component.literal(displayName(properties.ability).fit(13)),
                38.5F,
                122F,
                centered = true
            )
        }

        if (menu.showIvs) {
            val ivs = properties.ivs
            if (ivs != null) {
                renderIvChart(guiGraphics, ivs)
            } else {
                drawPcText(guiGraphics, Component.literal("IVs unknown"), 40F, 177F, centered = true, colour = 0xB8B8B8)
            }
        }
    }

    private fun renderFilterLabels(guiGraphics: GuiGraphics) {
        if (menu.filterStack.isEmpty) {
            drawPcText(guiGraphics, Component.literal("No upgrade"), 310F, 88F, centered = true, colour = 0xF08888)
            return
        }

        if (openSelect == OpenSelect.NONE && menu.ivFilterEnabled) {
            val stat = FilterConfig.STATS[selectedIvIndex]
            val rule = menu.filterConfig.ivRules[stat.key]
            val ruleText = rule?.let { "${it.operator.label} ${it.value}" } ?: "Any"
            drawSmallText(guiGraphics, Component.literal("IV Filters"), 310F, 109F, centered = true, colour = 0xD8D8D8)
            drawPcText(guiGraphics, Component.literal(stat.label), 310F, 124F, centered = true)
            drawSmallText(guiGraphics, Component.literal(ruleText), 310F, 143F, centered = true)
        }
    }

    private fun updateModelWidget(properties: PokemonProperties?) {
        if (!menu.showPokemonModel) {
            modelKey = null
            modelWidget = null
            return
        }
        val key = properties?.asString(" ")
        if (key == modelKey) return
        modelKey = key
        val renderable = runCatching { properties?.create()?.asRenderablePokemon() }.getOrNull()
            ?: runCatching { properties?.asRenderablePokemon() }.getOrNull()
        modelWidget = renderable?.let {
            ModelWidget(
                pX = leftPos + PORTRAIT_X,
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
        val pos = menu.blockPos ?: return null
        val blockEntity = Minecraft.getInstance().level?.getBlockEntity(pos) as? EggIncubatorBlockEntity
            ?: return null
        return blockEntity.clientEggProperties()
    }

    private fun renderIvChart(guiGraphics: GuiGraphics, ivs: com.cobblemon.mod.common.pokemon.IVs) {
        val centerX = 40.4F
        val centerY = 163.7F
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

    private fun addSearchBox(x: Int, y: Int, hint: String): EditBox {
        val box = EditBox(font, x + 3, y + 2, 63, 10, Component.literal(hint))
        box.setHint(Component.literal("Search $hint"))
        box.setMaxLength(32)
        box.setBordered(false)
        return addRenderableWidget(box)
    }

    private fun addFilterButton(x: Int, y: Int, width: Int, height: Int, id: Int): PcTextButton {
        return addDynamicFilterButton(x, y, width, height) { id }
    }

    private fun addDynamicFilterButton(x: Int, y: Int, width: Int, height: Int, idProvider: () -> Int): PcTextButton {
        val button = PcTextButton(x, y, width, height, Component.empty()) {
            val id = idProvider()
            if (id >= 0) Minecraft.getInstance().gameMode?.handleInventoryButtonClick(menu.containerId, id)
        }
        filterButtons.add(button)
        return addRenderableWidget(button)
    }

    private fun addLocalButton(x: Int, y: Int, width: Int, height: Int, onPress: () -> Unit): PcTextButton {
        val button = PcTextButton(x, y, width, height, Component.empty()) { onPress() }
        filterButtons.add(button)
        return addRenderableWidget(button)
    }

    private fun refreshDynamicControls() {
        val hasFilter = !menu.filterStack.isEmpty
        for (button in filterButtons) button.active = hasFilter

        val config = menu.filterConfig
        if (!selectAllowed(openSelect)) openSelect = OpenSelect.NONE
        val normalMode = openSelect == OpenSelect.NONE
        clearButton.message = Component.literal("Clear")
        speciesButton.message =
            Component.literal("Species: ${(config.species?.let(::displayName) ?: "Use Egg")}".fit(14))
        natureSelectButton.message = Component.literal("Nature: ${displayName(config.nature)}".fit(14))
        abilitySelectButton.message = Component.literal("Ability: ${displayName(config.ability)}".fit(14))
        rejectButton.message = Component.literal(if (config.rejectAction.id == "output") "Output" else "Delete")

        clearButton.visible = hasFilter && normalMode
        rejectButton.visible =
            hasFilter && normalMode && menu.rejectActionFilterEnabled && menu.deleteRejectActionEnabled
        speciesButton.visible =
            hasFilter && menu.speciesFilterEnabled && (normalMode || openSelect == OpenSelect.SPECIES)
        natureSelectButton.visible =
            hasFilter && menu.natureFilterEnabled && (normalMode || openSelect == OpenSelect.NATURE)
        abilitySelectButton.visible =
            hasFilter && menu.abilityFilterEnabled && (normalMode || openSelect == OpenSelect.ABILITY)
        speciesButton.setPosition(leftPos + 274, topPos + if (openSelect == OpenSelect.SPECIES) 31 else 50)
        natureSelectButton.setPosition(leftPos + 274, topPos + if (openSelect == OpenSelect.NATURE) 31 else 69)
        abilitySelectButton.setPosition(leftPos + 274, topPos + if (openSelect == OpenSelect.ABILITY) 31 else 88)

        val ivVisible = hasFilter && normalMode && menu.ivFilterEnabled
        ivPrevButton.visible = ivVisible
        ivNextButton.visible = ivVisible
        ivOperatorButton.visible = ivVisible
        ivDownButton.visible = ivVisible
        ivUpButton.visible = ivVisible
        ivClearButton.visible = ivVisible

        speciesSearch.visible = hasFilter && menu.speciesFilterEnabled && openSelect == OpenSelect.SPECIES
        speciesSearch.active = speciesSearch.visible
        natureSearch.visible = hasFilter && menu.natureFilterEnabled && openSelect == OpenSelect.NATURE
        natureSearch.active = natureSearch.visible
        abilitySearch.visible = hasFilter && menu.abilityFilterEnabled && openSelect == OpenSelect.ABILITY
        abilitySearch.active = abilitySearch.visible

        val speciesOptions = speciesOptions()
        speciesButtons.forEachIndexed { index, button ->
            val option = speciesOptions.getOrNull(index)
            button.visible =
                hasFilter && menu.speciesFilterEnabled && openSelect == OpenSelect.SPECIES && option != null
            button.message = Component.literal(option?.label?.fit(14) ?: "")
        }

        val natureOptions = natureOptions()
        natureButtons.forEachIndexed { index, button ->
            val option = natureOptions.getOrNull(index)
            button.visible = hasFilter && menu.natureFilterEnabled && openSelect == OpenSelect.NATURE && option != null
            button.message = Component.literal(option?.label?.fit(14) ?: "")
        }

        val abilityOptions = abilityOptions()
        abilityButtons.forEachIndexed { index, button ->
            val option = abilityOptions.getOrNull(index)
            button.visible =
                hasFilter && menu.abilityFilterEnabled && openSelect == OpenSelect.ABILITY && option != null
            button.message = Component.literal(option?.label?.fit(14) ?: "")
        }

        ivPrevButton.message = Component.literal("<")
        ivNextButton.message = Component.literal(">")
        val stat = FilterConfig.STATS[selectedIvIndex]
        val rule = config.ivRules[stat.key]
        ivOperatorButton.message = Component.literal(rule?.operator?.label ?: ">=")
        ivDownButton.message = Component.literal("-")
        ivUpButton.message = Component.literal("+")
        ivClearButton.message = Component.literal("x")

        speciesButton.highlighted = config.species != null
        natureSelectButton.highlighted = config.nature != null
        abilitySelectButton.highlighted = config.ability != null
        rejectButton.danger = config.rejectAction.id == "delete"
        ivOperatorButton.highlighted = rule != null
    }

    private fun selectAllowed(select: OpenSelect): Boolean {
        return when (select) {
            OpenSelect.NONE -> true
            OpenSelect.SPECIES -> menu.speciesFilterEnabled
            OpenSelect.NATURE -> menu.natureFilterEnabled
            OpenSelect.ABILITY -> menu.abilityFilterEnabled
        }
    }

    private fun natureOptions(): List<SelectOption> {
        val values = listOf<String?>(null) + Natures.all().map { it.name.path }.sorted()
        return values.mapIndexed { index, value -> SelectOption(index, value, displayName(value)) }
            .filter { it.matches(natureSearch.value) }
            .take(7)
    }

    private fun speciesOptions(): List<SelectOption> {
        val values = listOf<Pair<String?, String>>(null to "Any") + PokemonSpecies.species
            .sortedBy { it.resourceIdentifier.toString() }
            .map { it.resourceIdentifier.toString() to it.translatedName.string }
        return values.mapIndexed { index, (value, label) -> SelectOption(index, value, label) }
            .filter { it.matches(speciesSearch.value) }
            .take(7)
    }

    private fun abilityOptions(): List<SelectOption> {
        val selectedSpecies = menu.filterConfig.species?.let { id ->
            PokemonSpecies.species.firstOrNull {
                it.resourceIdentifier.toString().equals(id, ignoreCase = true) ||
                        it.resourceIdentifier.path.equals(id.substringAfter(':'), ignoreCase = true)
            }
        }
        val abilities = selectedSpecies?.abilities
            ?.map { it.template.name }
            ?.distinct()
            ?.sorted()
            ?: Abilities.all().map { it.name }.sorted()
        val values = listOf<String?>(null) + abilities
        return values.mapIndexed { index, value -> SelectOption(index, value, displayName(value)) }
            .filter { it.matches(abilitySearch.value) }
            .take(7)
    }

    private fun SelectOption.matches(search: String): Boolean {
        return search.isBlank() || label.contains(search, ignoreCase = true)
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
        centered: Boolean = false,
        shadow: Boolean = false,
        colour: Int = 0xFFFFFF,
        scale: Float = 0.85F
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

    private fun String.fit(max: Int): String {
        return if (length <= max) this else take(max - 1) + "."
    }

    private class PcTextButton(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        message: Component,
        onPress: OnPress
    ) : Button(x, y, width, height, message, onPress, DEFAULT_NARRATION) {
        var highlighted = false
        var danger = false

        override fun renderWidget(context: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
            val base = when {
                danger && isHovered() -> 0xFF9A6666.toInt()
                danger -> 0xFF805555.toInt()
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
            context.drawString(font, message, textX, y + 3, colour, true)
        }

        override fun playDownSound(soundManager: SoundManager) {
            soundManager.play(SimpleSoundInstance.forUI(CobblemonSounds.PC_CLICK, 1F))
        }
    }

    companion object {
        private const val BASE_WIDTH = 349
        private const val BASE_HEIGHT = 205
        private const val PORTRAIT_X = 6
        private const val PORTRAIT_Y = 27
        private const val PORTRAIT_SIZE = 66
        private const val EGG_PROGRESS_X = 158
        private const val EGG_PROGRESS_Y = 44
        private const val EGG_PROGRESS_WIDTH = 28
        private const val EGG_PROGRESS_HEIGHT = 30
        private const val EGG_PROGRESS_FRAMES = 10

        private fun incubatorResource(path: String): ResourceLocation {
            return ResourceLocation.fromNamespaceAndPath(CobblemonIncubator.MOD_ID, path)
        }

        private val backgroundResource = incubatorResource("textures/gui/egg_incubator.png")
        private val portraitBackgroundResource = incubatorResource("textures/gui/portrait_background.png")
        private val eggProgressResource = incubatorResource("textures/gui/egg_frame.png")
        private val statsChartResource = cobblemonResource("textures/gui/summary/summary_stats_chart.png")
    }
}
