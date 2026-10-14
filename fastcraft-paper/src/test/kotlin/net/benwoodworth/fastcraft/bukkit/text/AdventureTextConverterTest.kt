package net.benwoodworth.fastcraft.bukkit.text

import io.mockk.mockk
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
    private val converter = FcTextConverter_Bukkit_Paper(
        mockk(),
        FcTextColor_Bukkit_Paper.Operations(),
    )

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
