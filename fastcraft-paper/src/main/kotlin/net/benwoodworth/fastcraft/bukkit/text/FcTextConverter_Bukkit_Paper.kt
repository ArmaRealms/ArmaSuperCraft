package net.benwoodworth.fastcraft.bukkit.text

import net.benwoodworth.fastcraft.platform.text.FcText
import net.benwoodworth.fastcraft.platform.text.FcTextColor
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcTextConverter_Bukkit_Paper @Inject constructor(
    fcTextColorOperations: FcTextColor.Operations,
) : FcTextConverter_Bukkit,
    FcTextColor_Bukkit.Operations by fcTextColorOperations.bukkit {

    override fun toComponent(text: FcText): Component = when (val value = text as FcText_Bukkit) {
        is FcText_Bukkit.Adventure -> value.component
        is FcText_Bukkit.Legacy -> when {
            '§' in value.legacyText -> LegacyComponentSerializer.legacySection().deserialize(value.legacyText)
            else -> MiniMessage.miniMessage().deserialize(value.legacyText)
        }
        is FcText_Bukkit.Component -> {
            var component: Component = when (value) {
                is FcText_Bukkit.Component.Text -> Component.text(value.text)
                is FcText_Bukkit.Component.Translate -> Component.translatable(value.translate)
            }
            value.color?.let { color ->
                NamedTextColor.NAMES.value(color.id)?.let { component = component.color(it) }
            }
            value.bold?.let { component = component.decoration(TextDecoration.BOLD, it) }
            value.italic?.let { component = component.decoration(TextDecoration.ITALIC, it) }
            value.underline?.let { component = component.decoration(TextDecoration.UNDERLINED, it) }
            value.strikethrough?.let { component = component.decoration(TextDecoration.STRIKETHROUGH, it) }
            value.obfuscate?.let { component = component.decoration(TextDecoration.OBFUSCATED, it) }
            value.extra.forEach { component = component.append(toComponent(it)) }
            component
        }
    }

    // Compatibility methods retained until FcText itself is removed.
    override fun toRaw(text: FcText): String = GsonComponentSerializer.gson().serialize(toComponent(text))
    override fun toLegacy(text: FcText, locale: Locale): String =
        LegacyComponentSerializer.legacySection().serialize(toComponent(text))
    override fun toPlaintext(text: FcText, locale: Locale): String =
        PlainTextComponentSerializer.plainText().serialize(toComponent(text))
}
