package net.benwoodworth.fastcraft.bukkit.text

import net.benwoodworth.fastcraft.platform.text.FcText
import net.benwoodworth.fastcraft.platform.text.FcTextColor
import net.kyori.adventure.text.Component as AdventureComponent
import java.util.*

sealed class FcText_Bukkit : FcText {
    data class AdventureText(val component: AdventureComponent) : FcText_Bukkit()

    /** MiniMessage template with typed literal and component parameters. */
    data class MiniMessageText(
        val template: String,
        val unparsed: Map<String, String>,
        val components: Map<String, FcText>,
    ) : FcText_Bukkit()

    sealed class Component : FcText_Bukkit() {
        abstract val color: FcTextColor?
        abstract val bold: Boolean?
        abstract val italic: Boolean?
        abstract val underline: Boolean?
        abstract val strikethrough: Boolean?
        abstract val obfuscate: Boolean?
        abstract val extra: List<FcText>

        class Text(
            val text: String,
            override val color: FcTextColor? = null,
            override val bold: Boolean? = null,
            override val italic: Boolean? = null,
            override val underline: Boolean? = null,
            override val strikethrough: Boolean? = null,
            override val obfuscate: Boolean? = null,
            override val extra: List<FcText> = emptyList(),
        ) : Component() {
            override fun equals(other: Any?): Boolean {
                return other is Text &&
                        text == other.text &&
                        color == other.color &&
                        bold == other.bold &&
                        italic == other.italic &&
                        underline == other.underline &&
                        strikethrough == other.strikethrough &&
                        obfuscate == other.obfuscate &&
                        extra == other.extra
            }

            override fun hashCode(): Int {
                return Objects.hash(
                    text,
                    color,
                    bold,
                    italic,
                    underline,
                    strikethrough,
                    obfuscate,
                )
            }
        }

        class Translate(
            val translate: String,
            override val color: FcTextColor? = null,
            override val bold: Boolean? = null,
            override val italic: Boolean? = null,
            override val underline: Boolean? = null,
            override val strikethrough: Boolean? = null,
            override val obfuscate: Boolean? = null,
            override val extra: List<FcText> = emptyList(),
        ) : Component() {
            override fun equals(other: Any?): Boolean {
                return other is Translate &&
                        translate == other.translate &&
                        color == other.color &&
                        bold == other.bold &&
                        italic == other.italic &&
                        underline == other.underline &&
                        strikethrough == other.strikethrough &&
                        obfuscate == other.obfuscate &&
                        extra == other.extra
            }

            override fun hashCode(): Int {
                return Objects.hash(
                    translate,
                    color,
                    bold,
                    italic,
                    underline,
                    strikethrough,
                    obfuscate,
                )
            }
        }
    }

    interface Factory : FcText.Factory {
        fun createTranslate(
            translate: String,
            color: FcTextColor? = null,
            bold: Boolean? = null,
            italic: Boolean? = null,
            underline: Boolean? = null,
            strikethrough: Boolean? = null,
            obfuscate: Boolean? = null,
            extra: List<FcText> = emptyList(),
        ): FcText
    }
}

fun FcText.Factory.createTranslate(
    translate: String,
    color: FcTextColor? = null,
    bold: Boolean? = null,
    italic: Boolean? = null,
    underline: Boolean? = null,
    strikethrough: Boolean? = null,
    obfuscate: Boolean? = null,
    extra: List<FcText> = emptyList(),
): FcText {
    return (this as FcText_Bukkit.Factory).createTranslate(
        translate, color, bold, italic, underline, strikethrough, obfuscate, extra
    )
}
