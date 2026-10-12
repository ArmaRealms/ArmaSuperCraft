package net.benwoodworth.fastcraft.bukkit.text

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NativeItemTemplateTest {
    private val converter = FcTextConverter_Bukkit_Paper(FcTextColor_Bukkit_Paper.Operations())
    private val factory = FcText_Bukkit_Paper.Factory(converter)

    @Test
    fun preservesVanillaTranslationsAsComponentsInsideMiniMessageLore() {
        val item = FcText_Bukkit.Adventure(Component.translatable("item.minecraft.diamond"))
        val formatted = factory.createTemplate("<green>2x ${'$'}item", mapOf("item" to item))
        val result = converter.toComponent(formatted)
        val translatable = result.children().filterIsInstance<TranslatableComponent>().single()
        assertEquals("item.minecraft.diamond", translatable.key())
    }

    @Test
    fun treatsPlayerControlledNamesAsLiteralComponents() {
        val item = FcText_Bukkit.Adventure(Component.text("<red>não é tag"))
        val formatted = factory.createTemplate("<yellow>1x ${'$'}item", mapOf("item" to item))
        val json = converter.toRaw(formatted)
        assertEquals(true, json.contains("<red>não é tag"))
    }
}
