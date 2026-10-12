package net.benwoodworth.fastcraft.bukkit.text

import net.benwoodworth.fastcraft.platform.text.FcText
import net.benwoodworth.fastcraft.platform.text.FcTextConverter
import net.kyori.adventure.text.Component

interface FcTextConverter_Bukkit : FcTextConverter {
    fun toComponent(text: FcText): Component
}

fun FcTextConverter.toComponent(text: FcText): Component =
    (this as FcTextConverter_Bukkit).toComponent(text)
