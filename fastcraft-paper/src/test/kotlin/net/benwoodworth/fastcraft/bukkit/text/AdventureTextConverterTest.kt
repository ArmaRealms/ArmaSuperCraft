package net.benwoodworth.fastcraft.bukkit.text

import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.ParsingException
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class AdventureTextConverterTest {
    private val converter = FcTextConverter_Bukkit_Paper(FcTextColor_Bukkit_Paper.Operations())

    @Test
    fun preservesAdventureComponentsWithoutLegacySerialization() {
        val component = net.kyori.adventure.text.Component
            .text("Custom", net.kyori.adventure.text.format.TextColor.color(0xAABBCC))
            .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                net.kyori.adventure.text.Component.text("Tooltip")
            ))
            .append(net.kyori.adventure.text.Component.translatable("item.minecraft.diamond"))

        val wrapped = FcText_Bukkit.AdventureText(component)
        assertEquals(component, converter.toComponent(wrapped))
    }

    @Test
    fun miniMessageUsesNativeComponentsForRecipeItems() {
        val name = FcText_Bukkit.AdventureText(
            net.kyori.adventure.text.Component.translatable("item.minecraft.diamond")
                .color(net.kyori.adventure.text.format.NamedTextColor.GOLD)
        )
        val formatted = FcText_Bukkit.MiniMessageText(
            template = "<item> x<amount>",
            unparsed = mapOf("amount" to "2"),
            components = mapOf("item" to name),
        )

        val rendered = converter.toComponent(formatted)
        val translated = rendered.children()
            .filterIsInstance<TranslatableComponent>()
            .firstOrNull()
        assertEquals("item.minecraft.diamond", translated?.key())
        assertEquals(net.kyori.adventure.text.format.NamedTextColor.GOLD, translated?.color())
    }

    @Test
    fun convertsCustomCommandsToPlainTextWithoutMinecraftLanguageDownloads() {
        val command = FcText_Bukkit.MiniMessageText(
            "say <player> <gold>ready</gold>",
            mapOf("player" to "<red>Player"),
            emptyMap(),
        )

        assertEquals("say <red>Player ready", converter.toPlaintext(command))
    }

    @Test
    fun supportsNamedColorsAndResetWithoutBukkitChatColor() {
        val factory = FcTextColor_Bukkit_Paper.Factory()
        val colorOperations = FcTextColor_Bukkit_Paper.Operations()
        val colors = listOf(
            "black", "dark_blue", "dark_green", "dark_aqua", "dark_red",
            "dark_purple", "gold", "gray", "dark_gray", "blue", "green",
            "aqua", "red", "light_purple", "yellow", "white", "reset",
        )

        with(colorOperations) {
            for (id in colors) {
                assertEquals(id, factory.fromId(id).id)
            }
        }
        assertFailsWith<IllegalArgumentException> { factory.fromId("invalid") }

        val colored = converter.toComponent(
            FcText_Bukkit.Component.Text("Colored", color = factory.darkBlue),
        )
        assertEquals(net.kyori.adventure.text.format.NamedTextColor.DARK_BLUE, colored.color())

        val reset = converter.toComponent(
            FcText_Bukkit.Component.Text("Default", color = factory.default),
        )
        assertEquals(null, reset.color())
    }

    @Test
    fun preservesTranslationForTheMinecraftClient() {
        val component = converter.toComponent(FcText_Bukkit.Component.Translate("block.minecraft.stone"))
        assertEquals("block.minecraft.stone", assertIs<TranslatableComponent>(component).key())
    }

    @Test
    fun retainsNestedComponents() {
        val component = converter.toComponent(
            FcText_Bukkit.Component.Text(
                "Item: ",
                extra = listOf(FcText_Bukkit.Component.Translate("item.minecraft.diamond")),
            )
        )
        assertEquals("item.minecraft.diamond", assertIs<TranslatableComponent>(component.children().single()).key())
    }

    @Test
    fun parsesMiniMessageFormatting() {
        val component =
            converter.toComponent(FcText_Bukkit.MiniMessageText("<green>Super Craft ativado", emptyMap(), emptyMap()))
        assertEquals("Super Craft ativado", PlainTextComponentSerializer.plainText().serialize(component))
    }

    @Test
    fun disablesItalicOnlyWhenAbsentInMiniMessage() {
        val normal = converter.toComponent(
            FcText_Bukkit.MiniMessageText("<green>Texto normal", emptyMap(), emptyMap())
        )
        assertEquals(TextDecoration.State.FALSE, normal.decoration(TextDecoration.ITALIC))

        val italic = converter.toComponent(
            FcText_Bukkit.MiniMessageText("<italic>Texto em itálico</italic>", emptyMap(), emptyMap())
        )
        assertEquals(TextDecoration.State.TRUE, italic.decoration(TextDecoration.ITALIC))
    }

    @Test
    fun doesNotInterpretUserInputAsMiniMessage() {
        val value = FcText_Bukkit.MiniMessageText(
            "<green>Olá <player>",
            mapOf("player" to "<red>Jogador"),
            emptyMap(),
        )
        assertEquals(
            "Olá <red>Jogador",
            PlainTextComponentSerializer.plainText().serialize(converter.toComponent(value))
        )
    }

    @Test
    fun rejectsLegacySectionCodesInMiniMessage() {
        val exception = assertFailsWith<ParsingException> {
            converter.toComponent(FcText_Bukkit.MiniMessageText("§aAtivado", emptyMap(), emptyMap()))
        }
        assertContains(exception.message.orEmpty(), "Legacy formatting codes")
    }
}
