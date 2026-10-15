package net.benwoodworth.fastcraft.bukkit.text

import net.benwoodworth.fastcraft.platform.text.FcTextColor
import javax.inject.Inject
import javax.inject.Singleton

object FcTextColor_Bukkit_Paper {
    @Singleton
    class Operations @Inject constructor() : FcTextColor_Bukkit.Operations {
        override val FcTextColor.id: String
            get() = value as String
    }

    @Singleton
    class Factory @Inject constructor() : FcTextColor_Bukkit.Factory {
        override val black: FcTextColor get() = FcTextColor("black")
        override val darkBlue: FcTextColor get() = FcTextColor("dark_blue")
        override val darkGreen: FcTextColor get() = FcTextColor("dark_green")
        override val darkAqua: FcTextColor get() = FcTextColor("dark_aqua")
        override val darkRed: FcTextColor get() = FcTextColor("dark_red")
        override val darkPurple: FcTextColor get() = FcTextColor("dark_purple")
        override val gold: FcTextColor get() = FcTextColor("gold")
        override val gray: FcTextColor get() = FcTextColor("gray")
        override val darkGray: FcTextColor get() = FcTextColor("dark_gray")
        override val blue: FcTextColor get() = FcTextColor("blue")
        override val green: FcTextColor get() = FcTextColor("green")
        override val aqua: FcTextColor get() = FcTextColor("aqua")
        override val red: FcTextColor get() = FcTextColor("red")
        override val lightPurple: FcTextColor get() = FcTextColor("light_purple")
        override val yellow: FcTextColor get() = FcTextColor("yellow")
        override val white: FcTextColor get() = FcTextColor("white")
        override val default: FcTextColor get() = FcTextColor("reset")

        override fun fromId(id: String): FcTextColor = when (id) {
            "black" -> black
            "dark_blue" -> darkBlue
            "dark_green" -> darkGreen
            "dark_aqua" -> darkAqua
            "dark_red" -> darkRed
            "dark_purple" -> darkPurple
            "gold" -> gold
            "gray" -> gray
            "dark_gray" -> darkGray
            "blue" -> blue
            "green" -> green
            "aqua" -> aqua
            "red" -> red
            "light_purple" -> lightPurple
            "yellow" -> yellow
            "white" -> white
            "reset" -> default
            else -> throw IllegalArgumentException("Invalid color ID: $id")
        }
    }
}
