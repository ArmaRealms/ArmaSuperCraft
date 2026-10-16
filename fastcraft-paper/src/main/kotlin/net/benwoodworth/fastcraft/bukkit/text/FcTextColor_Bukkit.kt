package net.benwoodworth.fastcraft.bukkit.text

import net.benwoodworth.fastcraft.platform.text.FcTextColor

object FcTextColor_Bukkit {
    interface Operations : FcTextColor.Operations {
        val FcTextColor.id: String
    }

    interface Factory : FcTextColor.Factory {
        fun fromId(id: String): FcTextColor
    }
}

val FcTextColor.Operations.bukkit: FcTextColor_Bukkit.Operations
    get() = this as FcTextColor_Bukkit.Operations

fun FcTextColor.Factory.fromId(id: String): FcTextColor {
    return (this as FcTextColor_Bukkit.Factory).fromId(id)
}
