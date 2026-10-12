package net.benwoodworth.fastcraft.bukkit.text

import io.mockk.mockk
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
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
        val component = converter.toComponent(FcText_Bukkit.MiniMessageText("<green>FastCraft ativado", emptyMap(), emptyMap()))
        assertEquals("FastCraft ativado", PlainTextComponentSerializer.plainText().serialize(component))
    }

    @Test
    fun doesNotInterpretUserInputAsMiniMessage() {
        val value = FcText_Bukkit.MiniMessageText(
            "<green>Olá <player>",
            mapOf("player" to "<red>Jogador"),
            emptyMap(),
        )
        assertEquals("Olá <red>Jogador", PlainTextComponentSerializer.plainText().serialize(converter.toComponent(value)))
    }

    @Test
    fun leavesSectionCodesUnformatted() {
        val component = converter.toComponent(FcText_Bukkit.MiniMessageText("§aAtivado", emptyMap(), emptyMap()))
        assertEquals("§aAtivado", PlainTextComponentSerializer.plainText().serialize(component))
    }
}
