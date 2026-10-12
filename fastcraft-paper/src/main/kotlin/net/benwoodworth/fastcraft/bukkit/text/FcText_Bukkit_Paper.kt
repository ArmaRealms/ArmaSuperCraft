package net.benwoodworth.fastcraft.bukkit.text

import net.benwoodworth.fastcraft.platform.text.FcText
import net.benwoodworth.fastcraft.platform.text.FcTextColor
import net.benwoodworth.fastcraft.platform.text.FcTextConverter
import net.benwoodworth.fastcraft.bukkit.text.toComponent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import javax.inject.Inject
import javax.inject.Singleton

object FcText_Bukkit_Paper {
    @Singleton
    class Factory @Inject constructor(
        private val converter: FcTextConverter,
    ) : FcText_Bukkit.Factory {
        override fun createComponent(component: Component): FcText = FcText_Bukkit.Adventure(component)

        override fun createTemplate(template: String, placeholders: Map<String, FcText>): FcText {
            val resolvers = placeholders.map { (name, value) ->
                require(name.matches(Regex("[a-z][a-z0-9_-]*"))) { "Invalid component placeholder: $name" }
                Placeholder.component("fc_" + name, converter.toComponent(value))
            }
            // Match complete $item / \${item} tokens, never prefixes such as $itemName.
            // The untrusted component is injected with Placeholder.component, not parsed as markup.
            val tokenPattern = Regex("""\$(?:\{([a-z][a-z0-9_-]*)}|([a-z][a-z0-9_-]*))""")
            val markup = tokenPattern.replace(template) { match ->
                val name = match.groupValues[1].ifEmpty { match.groupValues[2] }
                if (name in placeholders) "<fc_" + name + ">" else match.value
            }
            return FcText_Bukkit.Adventure(MiniMessage.miniMessage().deserialize(markup, *resolvers.toTypedArray()))
        }

        override fun create(
            text: String,
            color: FcTextColor?,
            bold: Boolean?,
            italic: Boolean?,
            underline: Boolean?,
            strikethrough: Boolean?,
            obfuscate: Boolean?,
            extra: List<FcText>,
        ): FcText {
            return FcText_Bukkit.Component.Text(
                text = text,
                color = color,
                bold = bold,
                italic = italic,
                underline = underline,
                strikethrough = strikethrough,
                obfuscate = obfuscate,
                extra = extra
            )
        }

        override fun createTranslate(
            translate: String,
            color: FcTextColor?,
            bold: Boolean?,
            italic: Boolean?,
            underline: Boolean?,
            strikethrough: Boolean?,
            obfuscate: Boolean?,
            extra: List<FcText>,
        ): FcText {
            return FcText_Bukkit.Component.Translate(
                translate = translate,
                color = color,
                bold = bold,
                italic = italic,
                underline = underline,
                strikethrough = strikethrough,
                obfuscate = obfuscate,
                extra = extra
            )
        }

        override fun createLegacy(legacyText: String): FcText {
            return FcText_Bukkit.Legacy(legacyText)
        }
    }
}
