package com.nbp.cobblemon_incubator.client.screen

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.nbp.cobblemon_incubator.menu.EggPouchMenu
import com.nbp.cobblemon_incubator.network.EggPouchActionPacket
import com.nbp.cobblemon_incubator.network.ModNetwork
import com.nbp.cobblemon_incubator.util.CobbreedingCompat
import com.nbp.cobblemon_incubator.util.FilterConfig
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack

class EggPouchScreen(
    menu: EggPouchMenu,
    inventory: Inventory,
    title: Component
) : AbstractContainerScreen<EggPouchMenu>(menu, inventory, title) {

    companion object {
        private val CONTAINER_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png")
        private val SCROLLER_SPRITE = ResourceLocation.withDefaultNamespace("container/creative_inventory/scroller")
        private val SCROLLER_DISABLED_SPRITE = ResourceLocation.withDefaultNamespace("container/creative_inventory/scroller_disabled")

        private const val SCROLLER_WIDTH = 12
        private const val SCROLLER_HEIGHT = 15
        private const val TRACK_HEIGHT = 108

        private const val INFO_TAB_WIDTH = 14
        private const val INFO_TAB_HEIGHT = 12
    }

    private lateinit var searchBox: EditBox
    private lateinit var depositButton: Button
    private var isDraggingScroller = false
    private var scrollOffset = 0.0f

    private val displayTitle: Component
        get() {
            val short = Component.translatable("container.cobblemon_incubator.egg_pouch.title")
            return if (font.width(title) <= 45) title else short
        }

    init {
        imageWidth = 195
        imageHeight = 222
        inventoryLabelX = 8
        inventoryLabelY = 128
        titleLabelX = 8
        titleLabelY = 6
    }

    override fun init() {
        super.init()

        searchBox = EditBox(
            font,
            leftPos + 56,
            topPos + 4,
            61,
            11,
            Component.translatable("gui.cobblemon_incubator.pouch.search")
        ).apply {
            setMaxLength(40)
            setBordered(false)
            setTextColor(0xFFFFFF)
            setHint(Component.translatable(if (menu.isAdvanced) "gui.cobblemon_incubator.pouch.search_advanced_hint" else "gui.cobblemon_incubator.pouch.search_hint").withStyle(ChatFormatting.DARK_GRAY))
            setTooltip(Tooltip.create(Component.translatable(if (menu.isAdvanced) "gui.cobblemon_incubator.pouch.search_advanced_tooltip" else "gui.cobblemon_incubator.pouch.search_tooltip")))
            setResponder { query ->
                ModNetwork.sendToServer(EggPouchActionPacket(0, query))
            }
        }
        addRenderableWidget(searchBox)

        depositButton = Button.builder(Component.translatable("gui.cobblemon_incubator.pouch.deposit")) {
            Minecraft.getInstance().gameMode?.handleInventoryButtonClick(menu.containerId, EggPouchMenu.BUTTON_DEPOSIT_ALL)
        }.bounds(leftPos + 121, topPos + 3, 48, 12)
            .tooltip(Tooltip.create(Component.translatable("gui.cobblemon_incubator.pouch.deposit.tooltip")))
            .build()
        addRenderableWidget(depositButton)
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        renderTooltip(guiGraphics, mouseX, mouseY)
    }

    override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        val left = leftPos
        val top = topPos

        // Double chest background: top grid (176x126) and bottom player inventory (176x96)
        guiGraphics.blit(CONTAINER_BACKGROUND, left, top, 0, 0, 176, 126)
        guiGraphics.blit(CONTAINER_BACKGROUND, left, top + 126, 0, 126, 176, 96)

        // Search box inset background (left + 54 to left + 118)
        guiGraphics.fill(left + 54, top + 3, left + 118, top + 15, 0xFF373737.toInt())
        guiGraphics.fill(left + 55, top + 4, left + 117, top + 14, 0xFF8B8B8B.toInt())
        guiGraphics.fill(left + 55, top + 4, left + 116, top + 13, 0xFF000000.toInt())

        // Scrollbar track
        val trackX = left + 175
        val trackY = top + 18
        guiGraphics.fill(trackX, trackY, trackX + SCROLLER_WIDTH + 2, trackY + TRACK_HEIGHT, 0xFF373737.toInt())
        guiGraphics.fill(trackX + 1, trackY + 1, trackX + SCROLLER_WIDTH + 1, trackY + TRACK_HEIGHT - 1, 0xFF8B8B8B.toInt())
        guiGraphics.fill(trackX + 1, trackY + 1, trackX + SCROLLER_WIDTH, trackY + TRACK_HEIGHT - 2, 0xFF202020.toInt())

        // Scroller thumb
        val maxScroll = menu.maxScrollRows
        val thumbProgress = if (maxScroll > 0) menu.currentScrollRow.toFloat() / maxScroll else 0.0f
        val thumbY = trackY + (thumbProgress * (TRACK_HEIGHT - SCROLLER_HEIGHT)).toInt()
        val scrollerSprite = if (maxScroll > 0) SCROLLER_SPRITE else SCROLLER_DISABLED_SPRITE
        guiGraphics.blitSprite(scrollerSprite, trackX + 1, thumbY, SCROLLER_WIDTH, SCROLLER_HEIGHT)

        // Info guide tab above container for Advanced Pouch
        if (menu.isAdvanced) {
            val infoX = left + 157
            val infoY = (top - INFO_TAB_HEIGHT).coerceAtLeast(1)
            val infoH = top - infoY
            val hovered = isInfoHovered(mouseX.toDouble(), mouseY.toDouble())

            val bg = if (hovered) 0xFF2A3A42.toInt() else 0xFF232323.toInt()
            val border = if (hovered) 0xFF00E5FF.toInt() else 0xFF4A606A.toInt()
            val textCol = if (hovered) 0x00E5FF else 0x88B8C8

            guiGraphics.fill(infoX, infoY, infoX + INFO_TAB_WIDTH, top, bg)
            guiGraphics.fill(infoX, infoY, infoX + INFO_TAB_WIDTH, infoY + 1, border)
            guiGraphics.fill(infoX, infoY, infoX + 1, top, border)
            guiGraphics.fill(infoX + INFO_TAB_WIDTH - 1, infoY, infoX + INFO_TAB_WIDTH, top, border)

            val text = Component.literal("i").withStyle(ChatFormatting.BOLD)
            val textX = infoX + (INFO_TAB_WIDTH - font.width(text)) / 2
            val textY = infoY + (infoH - 8) / 2
            guiGraphics.drawString(font, text, textX, textY, textCol, false)
        }
    }

    override fun renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        // Short pouch title (never overlaps search box)
        guiGraphics.drawString(font, displayTitle, titleLabelX, titleLabelY, 0x404040, false)
        // Player inventory label
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false)
    }

    override fun renderTooltip(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        // Show advanced search guide tooltip when hovering over info tab
        if (isInfoHovered(mouseX.toDouble(), mouseY.toDouble())) {
            renderSearchGuideTooltip(guiGraphics, mouseX, mouseY)
            return
        }

        // Show full pouch name tooltip when hovering over title
        if (mouseX >= leftPos + 7 && mouseX <= leftPos + 53 && mouseY >= topPos + 2 && mouseY <= topPos + 16) {
            guiGraphics.renderComponentTooltip(font, listOf(title), mouseX, mouseY)
            return
        }

        val hovered = hoveredSlot
        if (hovered != null && hovered.hasItem() && hovered.index in 0 until EggPouchMenu.POUCH_SLOTS) {
            val eggStack = hovered.item
            val properties = CobbreedingCompat.extractProperties(eggStack)
            if (properties != null) {
                if (!menu.isAdvanced) {
                    renderBasicTooltip(guiGraphics, properties, mouseX, mouseY)
                    return
                } else {
                    renderAdvancedTooltip(guiGraphics, properties, eggStack, mouseX, mouseY)
                    return
                }
            }
        }
        super.renderTooltip(guiGraphics, mouseX, mouseY)
    }

    private fun renderBasicTooltip(
        guiGraphics: GuiGraphics,
        properties: com.cobblemon.mod.common.api.pokemon.PokemonProperties,
        mouseX: Int,
        mouseY: Int
    ) {
        val speciesId = properties.species
        val species = speciesId?.let { PokemonSpecies.getByName(it) }
        val isShiny = properties.shiny == true
        val name = species?.translatedName ?: Component.literal(formatName(speciesId))
        val title = if (isShiny) {
            Component.literal("★ ").withStyle(ChatFormatting.GOLD).append(name.copy().withStyle(ChatFormatting.GOLD))
        } else {
            name.copy().withStyle(ChatFormatting.YELLOW)
        }
        guiGraphics.renderComponentTooltip(font, listOf(title), mouseX, mouseY)
    }

    private fun renderAdvancedTooltip(
        guiGraphics: GuiGraphics,
        properties: com.cobblemon.mod.common.api.pokemon.PokemonProperties,
        eggStack: ItemStack,
        mouseX: Int,
        mouseY: Int
    ) {
        val lines = mutableListOf<Component>()
        val speciesId = properties.species
        val species = speciesId?.let { PokemonSpecies.getByName(it) }
        val isShiny = properties.shiny == true
        val name = species?.translatedName ?: Component.literal(formatName(speciesId))

        // Title line
        val title = if (isShiny) {
            Component.literal("★ ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(name.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
        } else {
            name.copy().withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
        }
        lines.add(title)

        // Types
        if (species != null) {
            val types = species.types.map { it.displayName }
            if (types.isNotEmpty()) {
                val typeComp = Component.translatable("gui.cobblemon_incubator.pouch.types").withStyle(ChatFormatting.GRAY).append(": ")
                types.forEachIndexed { i, t ->
                    if (i > 0) typeComp.append(Component.literal(" / ").withStyle(ChatFormatting.DARK_GRAY))
                    typeComp.append(t)
                }
                lines.add(typeComp)
            }
        }

        // Nature & Ability
        val natureName = formatName(properties.nature)
        lines.add(
            Component.translatable("tooltip.cobblemon_incubator.nature", natureName)
                .withStyle(ChatFormatting.GRAY)
        )

        val abilityName = formatName(properties.ability)
        lines.add(
            Component.translatable("tooltip.cobblemon_incubator.ability", abilityName)
                .withStyle(ChatFormatting.GRAY)
        )

        // Gender
        val gender = CobbreedingCompat.resolveGender(properties)
        if (gender != null) {
            val genderComp = Component.translatable("gui.cobblemon_incubator.filter.gender.value", gender.name.lowercase().replaceFirstChar { it.uppercase() })
            lines.add(genderComp.withStyle(when (gender.name) {
                "MALE" -> ChatFormatting.BLUE
                "FEMALE" -> ChatFormatting.LIGHT_PURPLE
                else -> ChatFormatting.GRAY
            }))
        }

        // IVs
        val ivs = properties.ivs
        if (ivs != null) {
            val hp = ivs.getOrDefault(Stats.HP)
            val atk = ivs.getOrDefault(Stats.ATTACK)
            val def = ivs.getOrDefault(Stats.DEFENCE)
            val spa = ivs.getOrDefault(Stats.SPECIAL_ATTACK)
            val spd = ivs.getOrDefault(Stats.SPECIAL_DEFENCE)
            val spe = ivs.getOrDefault(Stats.SPEED)

            lines.add(
                Component.literal("HP ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(formatIv(hp)).append(" ")
                    .append(Component.literal("Atk ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(formatIv(atk)).append(" ")
                    .append(Component.literal("Def ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(formatIv(def))
            )
            lines.add(
                Component.literal("SpA ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(formatIv(spa)).append(" ")
                    .append(Component.literal("SpD ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(formatIv(spd)).append(" ")
                    .append(Component.literal("Spe ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(formatIv(spe))
            )
        }

        // Hatch timer if available
        val timer = CobbreedingCompat.getTimer(eggStack)
        if (timer != null) {
            lines.add(
                Component.translatable("gui.cobblemon_incubator.pouch.timer", timer)
                    .withStyle(ChatFormatting.DARK_AQUA)
            )
        }

        guiGraphics.renderComponentTooltip(font, lines, mouseX, mouseY)
    }

    private fun formatIv(iv: Int): Component {
        return when (iv) {
            31 -> Component.literal("31").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
            0 -> Component.literal("0").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
            else -> Component.literal(iv.toString()).withStyle(ChatFormatting.WHITE)
        }
    }

    private fun formatName(value: String?): String {
        if (value.isNullOrBlank()) return "Unknown"
        val clean = value.substringAfter(':').substringAfterLast('/')
        return clean.split('-', '_', ' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (menu.maxScrollRows > 0) {
            val delta = if (scrollY > 0) -1 else 1
            val nextRow = (menu.currentScrollRow + delta).coerceIn(0, menu.maxScrollRows)
            if (nextRow != menu.currentScrollRow) {
                ModNetwork.sendToServer(EggPouchActionPacket(nextRow, searchBox.value))
                return true
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0) {
            if (isInfoHovered(mouseX, mouseY)) {
                Minecraft.getInstance().soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f))
                searchBox.isFocused = true
                return true
            }

            val trackX = leftPos + 175
            val trackY = topPos + 18
            if (mouseX >= trackX && mouseX <= trackX + SCROLLER_WIDTH + 2 && mouseY >= trackY && mouseY <= trackY + TRACK_HEIGHT) {
                isDraggingScroller = true
                updateScrollFromMouse(mouseY)
                return true
            }
        } else if (button == 1) {
            if (searchBox.isMouseOver(mouseX, mouseY)) {
                searchBox.value = ""
                return true
            }
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (searchBox.isFocused) {
            if (keyCode == 256) {
                onClose()
                return true
            }
            if (searchBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true
            }
            // While typing in search box, prevent keyInventory (E) or hotbar keys (1-9) from closing/interfering
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    private fun isInfoHovered(mouseX: Double, mouseY: Double): Boolean {
        if (!menu.isAdvanced) return false
        val infoX = leftPos + 157
        val infoY = (topPos - INFO_TAB_HEIGHT).coerceAtLeast(1)
        return mouseX >= infoX && mouseX < infoX + INFO_TAB_WIDTH && mouseY >= infoY && mouseY < topPos
    }

    private fun renderSearchGuideTooltip(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val lines = listOf(
            Component.translatable("gui.cobblemon_incubator.pouch.guide.title").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
            Component.translatable("gui.cobblemon_incubator.pouch.guide.subtitle").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC),
            Component.translatable("gui.cobblemon_incubator.pouch.guide.species").withStyle(ChatFormatting.WHITE),
            Component.translatable("gui.cobblemon_incubator.pouch.guide.type").withStyle(ChatFormatting.YELLOW),
            Component.translatable("gui.cobblemon_incubator.pouch.guide.ability").withStyle(ChatFormatting.GREEN),
            Component.translatable("gui.cobblemon_incubator.pouch.guide.nature").withStyle(ChatFormatting.LIGHT_PURPLE),
            Component.translatable("gui.cobblemon_incubator.pouch.guide.gender").withStyle(ChatFormatting.BLUE),
            Component.translatable("gui.cobblemon_incubator.pouch.guide.ivs_min").withStyle(ChatFormatting.GOLD),
            Component.translatable("gui.cobblemon_incubator.pouch.guide.ivs_stat").withStyle(ChatFormatting.RED),
            Component.translatable("gui.cobblemon_incubator.pouch.guide.shiny").withStyle(ChatFormatting.YELLOW),
            Component.translatable("gui.cobblemon_incubator.pouch.guide.example").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC)
        )
        guiGraphics.renderComponentTooltip(font, lines, mouseX, mouseY)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0) {
            isDraggingScroller = false
        }
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, dragX: Double, dragY: Double): Boolean {
        if (isDraggingScroller) {
            updateScrollFromMouse(mouseY)
            return true
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY)
    }

    private fun updateScrollFromMouse(mouseY: Double) {
        val maxScroll = menu.maxScrollRows
        if (maxScroll <= 0) return

        val trackY = topPos + 18
        val relativeY = (mouseY - trackY - SCROLLER_HEIGHT / 2.0).coerceIn(0.0, (TRACK_HEIGHT - SCROLLER_HEIGHT).toDouble())
        val progress = (relativeY / (TRACK_HEIGHT - SCROLLER_HEIGHT)).toFloat()
        val targetRow = (progress * maxScroll).toInt().coerceIn(0, maxScroll)
        if (targetRow != menu.currentScrollRow) {
            ModNetwork.sendToServer(EggPouchActionPacket(targetRow, searchBox.value))
        }
    }
}
