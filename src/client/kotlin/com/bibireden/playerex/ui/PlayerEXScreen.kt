package com.bibireden.playerex.ui

import com.bibireden.data_attributes.api.DataAttributesAPI
import com.bibireden.playerex.PlayerEX
import com.bibireden.playerex.PlayerEXClient
import com.bibireden.playerex.api.attribute.PlayerEXAttributes
import com.bibireden.playerex.components.PlayerEXComponents
import com.bibireden.playerex.util.PlayerEXUtil
import io.wispforest.owo.ui.base.BaseUIModelScreen
import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.component.LabelComponent
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.Component
import io.wispforest.owo.ui.core.ParentComponent
import kotlinx.atomicfu.atomicArrayOfNulls
import net.minecraft.text.Text
import kotlin.reflect.KClass

// Transformers
fun <T : Component> ParentComponent.childById(clazz: KClass<T>, id: String) = this.childById(clazz.java, id)

/** Primary screen for the mod that brings everything intended together. */
class PlayerEXScreen : BaseUIModelScreen<FlowLayout>(FlowLayout::class.java, DataSource.asset(PlayerEXClient.MAIN_UI_SCREEN_ID)) {
    private var currentPage = 0

    override fun build(rootComponent: FlowLayout) {
        val player = client?.player ?: return
        val playerLevel = DataAttributesAPI.getValue(PlayerEXAttributes.LEVEL, player).map(Double::toInt).orElse(0)
        val playerData = PlayerEXComponents.PLAYER_DATA.get(player)
        // TODO: Make registry based?
        val pages = arrayOf<List<Component>>(testLayout(), testLayout2()) // Temp, just to help myself make code - prob will change

        val pointsAvailable = rootComponent.childById(LabelComponent::class, "points_available")!!
        val currentLevel = rootComponent.childById(LabelComponent::class, "current_level")!!
        val previousPage = rootComponent.childById(ButtonComponent::class, "previous")!!
        val pageCounter = rootComponent.childById(LabelComponent::class, "counter")!!
        val nextPage = rootComponent.childById(ButtonComponent::class, "next")!!
        val exit = rootComponent.childById(ButtonComponent::class, "exit")!!
        val content = rootComponent.childById(FlowLayout::class, "content")!!

        currentLevel.text(Text.translatable("playerex.ui.current_level", playerLevel, PlayerEXUtil.getRequiredXp(player)))
        pointsAvailable.text(Text.of(playerData.skillPoints.toString()))
        pageCounter.text(Text.of("${currentPage + 1}/${pages.size}"))
        content.clearChildren()
        content.children(pages[currentPage])

        previousPage.onPress {
            if (currentPage > 0) {
                currentPage--
                pageCounter.text(Text.of("${currentPage + 1}/${pages.size}"))
                content.clearChildren()
                content.children(pages[currentPage])
            }
        }
        nextPage.onPress {
            if (currentPage < pages.lastIndex) {
                currentPage++
                pageCounter.text(Text.of("${currentPage + 1}/${pages.size}"))
                content.clearChildren()
                content.children(pages[currentPage])
            }
        }
        exit.onPress { this.close() }
    }

    // TODO: Remove
    private fun testLayout(): List<Component> {
        return listOf(
            Components.button(
                Text.literal("Test Button"),
            ) {
                System.out.println("Test Button")
            }
        )
    }

    // TODO: Remove
    private fun testLayout2(): List<Component> {
        return listOf(
            Components.button(
                Text.literal("Test Button 2"),
            ) {
                System.out.println("Test Button 2")
            }
        )
    }
}