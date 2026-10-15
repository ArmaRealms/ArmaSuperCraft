package net.benwoodworth.fastcraft.platform.text

interface FcTextConverter {
    fun toPlaintext(text: FcText): String
}
