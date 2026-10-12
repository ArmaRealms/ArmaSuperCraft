package net.benwoodworth.fastcraft.platform.text

import java.util.Locale

interface FcTextConverter {
    fun toPlaintext(text: FcText, locale: Locale): String
}
