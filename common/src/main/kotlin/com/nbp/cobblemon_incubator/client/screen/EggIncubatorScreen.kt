package com.nbp.cobblemon_incubator.client.screen

import com.cobblemon.mod.common.api.abilities.Abilities
import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.api.pokemon.Natures
import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import com.cobblemon.mod.common.api.pokemon.stats.Stat
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.client.CobblemonResources
import com.cobblemon.mod.common.client.gui.summary.widgets.ModelWidget
import com.cobblemon.mod.common.util.cobblemonResource
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferUploader
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import com.nbp.cobblemon_incubator.menu.EggIncubatorMenu
import com.nbp.cobblemon_incubator.util.CobbreedingCompat
import com.nbp.cobblemon_incubator.util.FilterConfig
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.phys.Vec2
import org.joml.Vector3f
import kotlin.math.roundToInt

class EggIncubatorScreen(menu: EggIncubatorMenu, inventory: Inventory, title: Component) :
    AbstractContainerScreen<EggIncubatorMenu>(menu, inventory, title) {

    private data class SelectOption(val index: Int, val value: String?, val label: String)
    private enum class OpenSelect { NONE, NATURE, ABILITY }

    private var selectedIvIndex = 0
    private var modelWidget: ModelWidget? = null
    private var modelKey: String? = null
    private var openSelect = OpenSelect.NONE

    private val filterButtons = mutableListOf<Button>()
    private val natureButtons = mutableListOf<Button>()
    private val abilityButtons = mutableListOf<Button>()

    private lateinit var clearButton: Button
    private lateinit var speciesButton: Button
    private lateinit var natureSelectButton: Button
    private lateinit var abilitySelectButton: Button
    private lateinit var rejectButton: Button
    private lateinit var ivPrevButton: Button
    private lateinit var ivNextButton: Button
    private lateinit var ivOperatorButton: Button
    private lateinit var ivDownButton: Button
    private lateinit var ivUpButton: Button
    private lateinit var ivClearButton: Button
    private lateinit var natureSearch: EditBox
    private lateinit var abilitySearch: EditBox

    private val statsChartResource = cobblemonResource("textures/gui/summary/summary_stats_chart.png")
    private val chartStats: List<Pair<String, Stat>> = listOf(
        "HP" to Stats.HP,
        "Atk" to Stats.ATTACK,
        "Def" to Stats.DEFENCE,
        "Spe" to Stats.SPEED,
        "SpD" to Stats.SPECIAL_DEFENCE,
        "SpA" to Stats.SPECIAL_ATTACK
    )

    init {
        imageWidth = 384
        imageHeight = 206
        inventoryLabelX = 94
        inventoryLabelY = 104
    }

    override fun init() {
        super.init()
        filterButtons.clear()
        natureButtons.clear()
        abilityButtons.clear()

        val filterX = leftPos + 282
        var y = topPos + 18

        clearButton = addFilterButton(filterX, y, 42, 16, 0)
        rejectButton = addFilterButton(filterX + 46, y, 54, 16, 6)
        y += 19
        speciesButton = addFilterButton(filterX, y, 100, 16, 1)

        y = topPos + 60
        natureSelectButton = addLocalButton(filterX, y, 100, 16) {
            openSelect = if (openSelect == OpenSelect.NATURE) OpenSelect.NONE else OpenSelect.NATURE
            if (openSelect == OpenSelect.NATURE) natureSearch.setFocused(true)
        }
        y += 17
        natureSearch = addSearchBox(filterX, y, "Nature")
        y += 15
        repeat(3) { row ->
            natureButtons += addDynamicFilterButton(filterX, y + row * 15, 100, 14) {
                natureOptions().getOrNull(row)?.let {
                    openSelect = OpenSelect.NONE
                    natureSearch.setFocused(false)
                    2000 + it.index
                } ?: -1
            }
        }

        y = topPos + 88
        abilitySelectButton = addLocalButton(filterX, y, 100, 16) {
            openSelect = if (openSelect == OpenSelect.ABILITY) OpenSelect.NONE else OpenSelect.ABILITY
            if (openSelect == OpenSelect.ABILITY) abilitySearch.setFocused(true)
        }
        y += 17
        abilitySearch = addSearchBox(filterX, y, "Ability")
        y += 15
        repeat(3) { row ->
            abilityButtons += addDynamicFilterButton(filterX, y + row * 15, 100, 14) {
                abilityOptions().getOrNull(row)?.let {
                    openSelect = OpenSelect.NONE
                    abilitySearch.setFocused(false)
                    3000 + it.index
                } ?: -1
            }
        }

        y = topPos + 130
        ivPrevButton = addLocalButton(filterX, y, 18, 16) {
            selectedIvIndex = Math.floorMod(selectedIvIndex - 1, FilterConfig.STATS.size)
        }
        ivNextButton = addLocalButton(filterX + 82, y, 18, 16) {
            selectedIvIndex = Math.floorMod(selectedIvIndex + 1, FilterConfig.STATS.size)
        }
        y += 18
        ivOperatorButton = addDynamicFilterButton(filterX, y, 30, 16) { 100 + selectedIvIndex }
        ivDownButton = addDynamicFilterButton(filterX + 34, y, 20, 16) { 110 + selectedIvIndex }
        ivUpButton = addDynamicFilterButton(filterX + 58, y, 20, 16) { 120 + selectedIvIndex }
        ivClearButton = addDynamicFilterButton(filterX + 82, y, 18, 16) { 130 + selectedIvIndex }
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
        guiGraphics.fill(left, top, left + imageWidth, top + imageHeight, 0xFF182029.toInt())
        guiGraphics.fill(left + 6, top + 6, left + 88, top + imageHeight - 6, 0xFF0F151C.toInt())
        guiGraphics.fill(left + 94, top + 6, left + 270, top + 92, 0xFF252D36.toInt())
        guiGraphics.fill(left + 94, top + 100, left + 270, top + imageHeight - 6, 0xFF11161D.toInt())
        guiGraphics.fill(left + 276, top + 6, left + imageWidth - 6, top + imageHeight - 6, 0xFF101820.toInt())

        val properties = eggProperties()
        updateModelWidget(properties)
        renderPokemonPreview(guiGraphics, mouseX, mouseY, partialTick)

        drawSlot(guiGraphics, left + 111, top + 34, 0xFF6EA8FE.toInt())
        drawSlot(guiGraphics, left + 219, top + 34, 0xFF62C073.toInt())
        drawSlot(guiGraphics, left + 159, top + 16, 0xFFE5C07B.toInt())
        drawSlot(guiGraphics, left + 179, top + 16, 0xFFE5C07B.toInt())
        drawSlot(guiGraphics, left + 159, top + 52, 0xFFE5C07B.toInt())
        drawSlot(guiGraphics, left + 179, top + 52, 0xFFE5C07B.toInt())

        val max = menu.maxTimer.coerceAtLeast(1)
        val progress = ((max - menu.remainingTimer).coerceIn(0, max) / max.toFloat() * 48).roundToInt()
        guiGraphics.fill(left + 154, top + 39, left + 202, top + 47, 0xFF0B0F14.toInt())
        guiGraphics.fill(left + 154, top + 39, left + 154 + progress, top + 47, 0xFF62C073.toInt())
    }

    override fun renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        guiGraphics.drawString(font, title, 94, 8, 0xE6EDF3, false)
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xC9D1D9, false)
        guiGraphics.drawString(font, "${menu.speedMultiplier}x", 170, 39, 0xE6EDF3, false)
        if (menu.hasPcUpgrade) guiGraphics.drawString(font, "PC", 172, 29, 0x8EEA9A, false)

        renderEggInfo(guiGraphics)
        renderFilterLabels(guiGraphics)
    }

    private fun renderPokemonPreview(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        guiGraphics.fill(leftPos + 13, topPos + 13, leftPos + 81, topPos + 82, 0xFF1B2633.toInt())
        guiGraphics.fill(leftPos + 15, topPos + 15, leftPos + 79, topPos + 80, 0xFF263645.toInt())
        modelWidget?.render(guiGraphics, mouseX, mouseY, partialTick)
    }

    private fun renderEggInfo(guiGraphics: GuiGraphics) {
        val properties = eggProperties()
        guiGraphics.drawString(font, "Egg Preview", 12, 88, 0xE6EDF3, false)
        if (properties == null) {
            guiGraphics.drawString(font, "No egg", 22, 108, 0x8B949E, false)
            return
        }

        guiGraphics.drawString(font, (properties.species ?: "Unknown").fit(13), 12, 101, 0xE6EDF3, false)
        guiGraphics.drawString(font, "Nature", 12, 114, 0x8B949E, false)
        guiGraphics.drawString(font, displayValue(properties.nature).fit(13), 12, 123, 0xC9D1D9, false)
        guiGraphics.drawString(font, "Ability", 12, 135, 0x8B949E, false)
        guiGraphics.drawString(font, displayValue(properties.ability).fit(13), 12, 144, 0xC9D1D9, false)

        val ivs = properties.ivs
        if (ivs != null) {
            renderIvChart(guiGraphics, ivs)
        } else {
            guiGraphics.drawString(font, "IVs unknown", 12, 174, 0x8B949E, false)
        }
    }

    private fun renderFilterLabels(guiGraphics: GuiGraphics) {
        guiGraphics.drawString(font, "Filter", 282, 8, 0xE6EDF3, false)
        if (menu.filterStack.isEmpty) {
            guiGraphics.drawString(font, "No upgrade", 286, 92, 0xF08888, false)
            return
        }

        val stat = FilterConfig.STATS[selectedIvIndex]
        val config = menu.filterConfig
        val rule = config.ivRules[stat.key]
        val ruleText = rule?.let { "${it.operator.label}${it.value}" } ?: "Any"
        if (openSelect == OpenSelect.NONE) {
            guiGraphics.drawString(font, "IV ${stat.label}: $ruleText", 305, 122, 0xC9D1D9, false)
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
                pX = leftPos + 13,
                pY = topPos + 14,
                pWidth = 68,
                pHeight = 68,
                pokemon = it,
                baseScale = 2.0F,
                rotationY = 35F,
                offsetY = 0.0,
                playCryOnClick = false,
                shouldFollowCursor = true
            )
        }
    }

    private fun eggProperties(): PokemonProperties? = CobbreedingCompat.extractProperties(menu.inputStack)

    private fun renderIvChart(guiGraphics: GuiGraphics, ivs: com.cobblemon.mod.common.pokemon.IVs) {
        val x = 18
        val y = 154
        val scale = 0.26F
        blitk(
            matrixStack = guiGraphics.pose(),
            texture = statsChartResource,
            x = x / scale,
            y = y / scale,
            width = 166,
            height = 192,
            scale = scale
        )

        drawIvPolygon(
            centerX = leftPos + x + 21.7F,
            centerY = topPos + y + 18.2F,
            radius = 15.6F,
            ratios = chartStats.map { (_, stat) -> ivs.getOrDefault(stat) / 31F },
            colour = Vector3f(216F / 255F, 100F / 255F, 1F)
        )

        val labelColor = 0xE6EDF3
        val mutedColor = 0x8B949E
        val points = chartPoints(x + 21.7F, y + 18.2F, 20.0F)
        chartStats.forEachIndexed { index, pair ->
            val point = points[index]
            val value = ivs.getOrDefault(pair.second)
            val labelX = when (index) {
                0, 3 -> (point.x - font.width(pair.first) / 2).roundToInt()
                1, 2 -> point.x.roundToInt()
                else -> (point.x - font.width(pair.first)).roundToInt()
            }
            val labelY = when (index) {
                0 -> point.y.roundToInt() - 5
                3 -> point.y.roundToInt() - 1
                else -> point.y.roundToInt() - 3
            }
            val label = if (index % 2 == 0) pair.first else value.toString()
            guiGraphics.drawString(font, label, labelX, labelY, labelColor, false)
        }
        guiGraphics.drawString(font, "Total ${ivs.total()}", 12, 194, mutedColor, false)
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

    private fun drawSlot(guiGraphics: GuiGraphics, x: Int, y: Int, accent: Int) {
        guiGraphics.fill(x - 1, y - 1, x + 19, y + 19, 0xFF0D1117.toInt())
        guiGraphics.fill(x, y, x + 18, y + 18, 0xFF3B424C.toInt())
        guiGraphics.fill(x, y, x + 18, y + 1, accent)
    }

    private fun addSearchBox(x: Int, y: Int, hint: String): EditBox {
        val box = EditBox(font, x, y, 100, 14, Component.literal(hint))
        box.setHint(Component.literal("Search $hint"))
        box.setMaxLength(32)
        box.setBordered(true)
        return addRenderableWidget(box)
    }

    private fun addFilterButton(x: Int, y: Int, width: Int, height: Int, id: Int): Button {
        return addDynamicFilterButton(x, y, width, height) { id }
    }

    private fun addDynamicFilterButton(x: Int, y: Int, width: Int, height: Int, idProvider: () -> Int): Button {
        val button = Button.builder(Component.empty()) {
            val id = idProvider()
            if (id >= 0) Minecraft.getInstance().gameMode?.handleInventoryButtonClick(menu.containerId, id)
        }.bounds(x, y, width, height).build()
        filterButtons.add(button)
        return addRenderableWidget(button)
    }

    private fun addLocalButton(x: Int, y: Int, width: Int, height: Int, onPress: () -> Unit): Button {
        val button = Button.builder(Component.empty()) { onPress() }.bounds(x, y, width, height).build()
        filterButtons.add(button)
        return addRenderableWidget(button)
    }

    private fun refreshDynamicControls() {
        val hasFilter = !menu.filterStack.isEmpty
        for (button in filterButtons) button.active = hasFilter

        val config = menu.filterConfig
        clearButton.message = Component.literal("Clear")
        speciesButton.message = Component.literal("Species: ${config.species ?: "Use egg"}".fit(16))
        natureSelectButton.message = Component.literal("Nature: ${config.nature ?: "Any"}".fit(16))
        abilitySelectButton.message = Component.literal("Ability: ${config.ability ?: "Any"}".fit(16))
        rejectButton.message = Component.literal(config.rejectAction.label)
        natureSelectButton.visible = hasFilter
        abilitySelectButton.visible = hasFilter && openSelect != OpenSelect.NATURE
        val ivVisible = hasFilter && openSelect == OpenSelect.NONE
        ivPrevButton.visible = ivVisible
        ivNextButton.visible = ivVisible
        ivOperatorButton.visible = ivVisible
        ivDownButton.visible = ivVisible
        ivUpButton.visible = ivVisible
        ivClearButton.visible = ivVisible

        natureSearch.visible = hasFilter && openSelect == OpenSelect.NATURE
        natureSearch.active = natureSearch.visible
        abilitySearch.visible = hasFilter && openSelect == OpenSelect.ABILITY
        abilitySearch.active = abilitySearch.visible

        val natureOptions = natureOptions()
        natureButtons.forEachIndexed { index, button ->
            val option = natureOptions.getOrNull(index)
            button.visible = hasFilter && openSelect == OpenSelect.NATURE && option != null
            button.message = Component.literal(option?.label?.fit(16) ?: "")
        }

        val abilityOptions = abilityOptions()
        abilityButtons.forEachIndexed { index, button ->
            val option = abilityOptions.getOrNull(index)
            button.visible = hasFilter && openSelect == OpenSelect.ABILITY && option != null
            button.message = Component.literal(option?.label?.fit(16) ?: "")
        }

        ivPrevButton.message = Component.literal("<")
        ivNextButton.message = Component.literal(">")
        val stat = FilterConfig.STATS[selectedIvIndex]
        val rule = config.ivRules[stat.key]
        ivOperatorButton.message = Component.literal(rule?.operator?.label ?: ">=")
        ivDownButton.message = Component.literal("-")
        ivUpButton.message = Component.literal("+")
        ivClearButton.message = Component.literal("x")
    }

    private fun natureOptions(): List<SelectOption> {
        val values = listOf<String?>(null) + Natures.all().map { it.name.path }.sorted()
        return values.mapIndexed { index, value -> SelectOption(index, value, value ?: "Any") }
            .filter { it.matches(natureSearch.value) }
            .take(3)
    }

    private fun abilityOptions(): List<SelectOption> {
        val values = listOf<String?>(null) + Abilities.all().map { it.name }.sorted()
        return values.mapIndexed { index, value -> SelectOption(index, value, value ?: "Any") }
            .filter { it.matches(abilitySearch.value) }
            .take(3)
    }

    private fun SelectOption.matches(search: String): Boolean {
        return search.isBlank() || label.contains(search, ignoreCase = true)
    }

    private fun displayValue(value: String?): String {
        return value?.substringAfter(':') ?: "Any"
    }

    private fun String.fit(max: Int): String {
        return if (length <= max) this else take(max - 1) + "."
    }
}
