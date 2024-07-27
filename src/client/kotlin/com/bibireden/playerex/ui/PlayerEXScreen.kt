package com.bibireden.playerex.ui

import com.bibireden.playerex.PlayerEX
import com.bibireden.playerex.PlayerEXClient
import com.bibireden.playerex.components.PlayerEXComponents
import com.bibireden.playerex.components.player.IPlayerDataComponent
import com.bibireden.playerex.ext.canLevelUp
import com.bibireden.playerex.ext.data
import com.bibireden.playerex.ext.level
import com.bibireden.playerex.networking.NetworkingChannels
import com.bibireden.playerex.networking.NetworkingPackets
import com.bibireden.playerex.networking.types.UpdatePacketType
import com.bibireden.playerex.registry.AttributesMenuRegistry
import com.bibireden.playerex.ui.components.MenuComponent
import com.bibireden.playerex.ui.components.MenuComponent.OnLevelUpdated
import com.bibireden.playerex.ui.components.buttons.AttributeButtonComponent
import com.bibireden.playerex.ui.util.Colors
import com.bibireden.playerex.util.PlayerEXUtil
import io.wispforest.owo.ui.base.BaseUIModelScreen
import io.wispforest.owo.ui.component.*
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.Component
import io.wispforest.owo.ui.core.Easing
import io.wispforest.owo.ui.core.ParentComponent
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.util.EventSource
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.math.MathHelper
import kotlin.reflect.KClass

// Transformers
fun <T : Component> ParentComponent.childById(clazz: KClass<T>, id: String) = this.childById(clazz.java, id)

/** Primary screen for the mod that brings everything intended together. */
class PlayerEXScreen : BaseUIModelScreen<FlowLayout>(FlowLayout::class.java, DataSource.asset(PlayerEXClient.MAIN_UI_SCREEN_ID)) {
    private var currentPage = 0

    private val pages: MutableList<MenuComponent> = mutableListOf()

    private val player by lazy { this.client!!.player!! }

    private val content by lazy { uiAdapter.rootComponent.childById(FlowLayout::class, "content")!! }
    private val footer by lazy { uiAdapter.rootComponent.childById(FlowLayout::class, "footer")!! }

    private val levelAmount by lazy { uiAdapter.rootComponent.childById(TextBoxComponent::class, "level:amount")!! }

    private val onLevelUpdatedEvents = OnLevelUpdated.stream
    private val onLevelUpdated: EventSource<OnLevelUpdated> = onLevelUpdatedEvents.source()

    override fun shouldPause(): Boolean = false

    /** Whenever the level attribute gets modified, and on initialization of the screen, this will be called. */
    fun onLevelUpdated(level: Int) {
        val root = this.uiAdapter.rootComponent

        root.childById(LabelComponent::class, "level:current")?.apply {
            text(Text.translatable("playerex.ui.current_level", player.level.toInt(), PlayerEXUtil.getRequiredXpForNextLevel(player)))
        }

        updatePointsAvailable()
        updateLevelUpButton()
        updateProgressBar()

        this.uiAdapter.rootComponent.forEachDescendant { descendant ->
            if (descendant is MenuComponent) descendant.onLevelUpdatedEvents.sink().onLevelUpdated(level)
            if (descendant is AttributeButtonComponent) descendant.refresh()
        }
    }

    /** Whenever any attribute is updated, this will be called. */
    fun onAttributeUpdated(attribute: EntityAttribute, value: Double) {
        this.uiAdapter.rootComponent.forEachDescendant { descendant ->
            if (descendant is MenuComponent) descendant.onAttributeUpdatedEvents.sink().onAttributeUpdated(attribute, value)
            if (descendant is AttributeButtonComponent) descendant.refresh()
        }
        updatePointsAvailable()
    }

    private fun updatePointsAvailable() {
        this.uiAdapter.rootComponent.childById(LabelComponent::class, "points_available")?.apply {
            text(Text.translatable("playerex.ui.main.skill_points_header").append(": [").append(
                Text.literal("${player.data.skillPoints}").styled {
                    it.withColor(when (player.data.skillPoints) {
                        0 -> Colors.GRAY else -> Colors.SATURATED_BLUE
                    })
                }).append("]")
            )
        }
    }

    private fun onPagesUpdated() {
        val root = this.uiAdapter.rootComponent
        val pageCounter = root.childById(LabelComponent::class, "counter")!!
        val content = root.childById(FlowLayout::class, "content")!!

        pageCounter.text(Text.of("${currentPage + 1}/${pages.size}"))
        content.clearChildren()
        content.child(pages[currentPage])
    }

    private fun updateLevelUpButton() {
        val amount = levelAmount.text.toIntOrNull() ?: return
        val result = player.level + amount

        this.uiAdapter.rootComponent.childById(ButtonComponent::class, "level:button")!!
            .active(player.canLevelUp())
            .tooltip(Text.translatable("playerex.ui.level_button", PlayerEXUtil.getRequiredXpForLevel(player, result), amount, player.experienceLevel))
    }

    private fun updateProgressBar() {
        var result = 0.0
        if (player.experienceLevel > 0) {
            val required = PlayerEXUtil.getRequiredXpForNextLevel(player)
            result = MathHelper.clamp((player.experienceLevel.toDouble() / required) * 100, 0.0, 100.0)
        }
       footer.childById(BoxComponent::class, "progress")!!
            .horizontalSizing().animate(1000, Easing.CUBIC, Sizing.fill(result.toInt())).forwards()
    }

    override fun build(rootComponent: FlowLayout) {
        val player = client?.player ?: return

        val levelUpButton = rootComponent.childById(ButtonComponent::class, "level:button")!!

        updateLevelUpButton()
        levelAmount.onChanged().subscribe { updateLevelUpButton() }

        val previousPage = rootComponent.childById(ButtonComponent::class, "previous")!!
        val pageCounter = rootComponent.childById(LabelComponent::class, "counter")!!
        val nextPage = rootComponent.childById(ButtonComponent::class, "next")!!
        val exit = rootComponent.childById(ButtonComponent::class, "exit")!!

        AttributesMenuRegistry.get().forEach {
            val instance = it.getDeclaredConstructor().newInstance()
            instance.build(player, this.uiAdapter, player.data)
            pages.add(instance)
        }

        this.onLevelUpdated(player.level.toInt())
        this.onPagesUpdated()

        pageCounter.text(Text.of("${currentPage + 1}/${pages.size}"))

        content.clearChildren()
        content.child(pages[currentPage])

        previousPage.onPress {
            if (currentPage > 0) {
                currentPage--
                this.onPagesUpdated()
            }
        }
        nextPage.onPress {
            if (currentPage < pages.lastIndex) {
                currentPage++
                this.onPagesUpdated()
            }
        }

        levelUpButton.onPress {
            levelAmount.text.toIntOrNull()?.let { NetworkingChannels.MODIFY.clientHandle().send(NetworkingPackets.Level(it)) }
        }

        onLevelUpdated.subscribe { this.updateLevelUpButton() }

        exit.onPress { this.close() }
    }

    /** Whenever the player's experience is changed, refreshing the current status of experience-tied ui elements. */
    fun onExperienceUpdated() {
        updateLevelUpButton()
        updateProgressBar()
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (PlayerEXClient.KEYBINDING_MAIN_SCREEN.matchesKey(keyCode, scanCode)) {
            this.close()
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    enum class AttributeButtonComponentType {
        Add,
        Remove;

        fun getPointsFromComponent(component: IPlayerDataComponent): Int = if (this == Add) component.skillPoints else component.refundablePoints

        val symbol: String
            get() = if (this == Add) "+" else "-"

        val packet: UpdatePacketType
            get() = when (this) {
                Add -> UpdatePacketType.Skill
                Remove -> UpdatePacketType.Refund
            }
    }
}