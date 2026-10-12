package net.benwoodworth.fastcraft.bukkit.text

import net.benwoodworth.fastcraft.platform.text.FcText
import net.benwoodworth.fastcraft.platform.text.FcTextColor
import net.kyori.adventure.text.Component
import javax.inject.Inject
import javax.inject.Singleton

object FcText_Bukkit_Paper {
    @Singleton
    class Factory @Inject constructor(
    ) : FcText_Bukkit.Factory {
        override fun createComponent(component: Component): FcText = FcText_Bukkit.Adventure(component)

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

        override fun createMiniMessage(
            template: String,
            unparsed: Map<String, String>,
            components: Map<String, FcText>,
        ): FcText = FcText_Bukkit.MiniMessageText(template, unparsed, components)
    }
}
