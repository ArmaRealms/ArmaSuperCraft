package net.benwoodworth.fastcraft.bukkit.text

import net.benwoodworth.fastcraft.platform.text.FcText
import net.benwoodworth.fastcraft.platform.text.FcTextConverter
import net.kyori.adventure.text.Component

interface FcTextConverter_Bukkit : FcTextConverter {
    fun toComponent(text: FcText): Component
    fun toRaw(text: FcText): String
}

fun FcTextConverter.toRaw(text: FcText): String {
    return (this as FcTextConverter_Bukkit).toRaw(text)
}

/** Bridge for core FcText until the platform-neutral layer is removed. */
fun FcTextConverter.toComponent(text: FcText): Component =
    (this as FcTextConverter_Bukkit).toComponent(text)
