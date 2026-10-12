package net.benwoodworth.fastcraft.bukkit.text

import net.benwoodworth.fastcraft.platform.text.FcText
import net.benwoodworth.fastcraft.platform.text.FcTextColor
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcTextConverter_Bukkit_Paper @Inject constructor(
    private val localizer: BukkitLocalizer,
    fcTextColorOperations: FcTextColor.Operations,
) : FcTextConverter_Bukkit,
    FcTextColor_Bukkit.Operations by fcTextColorOperations.bukkit {

    override fun toComponent(text: FcText): Component = when (val value = text as FcText_Bukkit) {
        is FcText_Bukkit.MiniMessageText -> {
            val unparsed = value.unparsed.map { (name, literal) ->
                require(name.matches(Regex("[a-z][a-z0-9_-]*"))) { "Invalid MiniMessage placeholder: $name" }
                Placeholder.unparsed(name, literal)
            }
            val components = value.components.map { (name, component) ->
                require(name.matches(Regex("[a-z][a-z0-9_-]*"))) { "Invalid MiniMessage placeholder: $name" }
                require(name !in value.unparsed) { "Duplicate MiniMessage placeholder: $name" }
                Placeholder.component(name, toComponent(component))
            }
            MiniMessage.miniMessage().deserialize(value.template, *(unparsed + components).toTypedArray())
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

    override fun toPlaintext(text: FcText, locale: Locale): String {
        val value = text as FcText_Bukkit
        return when (value) {
            is FcText_Bukkit.MiniMessageText ->
                PlainTextComponentSerializer.plainText().serialize(toComponent(value))
            is FcText_Bukkit.Component -> {
                val label = when (value) {
                    is FcText_Bukkit.Component.Text -> value.text
                    is FcText_Bukkit.Component.Translate ->
                        localizer.localize(value.translate, locale) ?: value.translate
                }
                label + value.extra.joinToString("") { toPlaintext(it, locale) }
            }
        }
    }
}
