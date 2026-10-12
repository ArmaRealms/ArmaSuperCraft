package net.benwoodworth.fastcraft.bukkit.text

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NativeItemTemplateTest {
    private val converter = FcTextConverter_Bukkit_Paper(FcTextColor_Bukkit_Paper.Operations())
    private val factory = FcText_Bukkit_Paper.Factory()

    @Test
    fun preservesTranslatedItemsAsMiniMessageComponents() {
        val item = FcText_Bukkit.Adventure(Component.translatable("item.minecraft.diamond"))
        val formatted = factory.createMiniMessage("<green><amount>x <item>",
            unparsed = mapOf("amount" to "2"), components = mapOf("item" to item))
        val result = converter.toComponent(formatted)
        val translatable = result.children().filterIsInstance<TranslatableComponent>().single()
        assertEquals("item.minecraft.diamond", translatable.key())
    }

    @Test
    fun oldDollarTokensAreNotSubstituted() {
        val item = FcText_Bukkit.Adventure(Component.text("Diamante"))
        val formatted = factory.createMiniMessage("<yellow>\$item <item>",
            components = mapOf("item" to item))
        val plain = PlainTextComponentSerializer.plainText().serialize(converter.toComponent(formatted))
        assertEquals("\$item Diamante", plain)
    }

    @Test
    fun retainsLiteralUserContent() {
        val item = FcText_Bukkit.Adventure(Component.text("<red>não é tag"))
        val formatted = factory.createMiniMessage("<yellow>1x <item>",
            components = mapOf("item" to item))
        val plain = PlainTextComponentSerializer.plainText().serialize(converter.toComponent(formatted))
        assertEquals("1x <red>não é tag", plain)
    }
}
