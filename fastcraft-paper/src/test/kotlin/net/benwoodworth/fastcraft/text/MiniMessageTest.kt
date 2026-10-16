package net.benwoodworth.fastcraft.text

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.ParsingException
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MiniMessageTest {
    @Test
    fun preservesNativeItemNameStyleAndHover() {
        val item = Component.translatable("item.minecraft.diamond").color(NamedTextColor.GOLD)
            .hoverEvent(Component.text("Tooltip"))
        val result = miniMessage("<item> x<amount>", mapOf("amount" to "2"), mapOf("item" to item))
        val translated = result.children().filterIsInstance<TranslatableComponent>().single()
        assertEquals(item, translated)
        assertEquals(" x2", PlainTextComponentSerializer.plainText().serialize(result.children().last()))
    }

    @Test
    fun keepsUntrustedValuesLiteralAndEscapesTags() {
        val result = miniMessage("<green>Olá <player> \\<red>", mapOf("player" to "<click:run_command:'/op me'>Name"))
        assertEquals("Olá <click:run_command:'/op me'>Name <red>", PlainTextComponentSerializer.plainText().serialize(result))
        assertEquals(NamedTextColor.GREEN, result.color())
    }

    @Test
    fun disablesDefaultItalicsButHonorsExplicitItalics() {
        assertEquals(TextDecoration.State.FALSE, miniMessage("<green>Normal").decoration(TextDecoration.ITALIC))
        assertEquals(TextDecoration.State.TRUE, miniMessage("<italic>Italic</italic>").decoration(TextDecoration.ITALIC))
    }

    @Test
    fun rejectsInvalidAndAmbiguousPlaceholders() {
        assertFailsWith<IllegalArgumentException> { miniMessage("<bad.name>", mapOf("bad.name" to "value")) }
        assertFailsWith<IllegalArgumentException> {
            miniMessage("<item>", mapOf("item" to "value"), mapOf("item" to Component.text("Other")))
        }
    }

    @Test
    fun rejectsLegacySectionCodes() {
        assertFailsWith<ParsingException> { miniMessage("§aAtivado") }
    }
}
