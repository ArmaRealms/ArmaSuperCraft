package net.benwoodworth.fastcraft.platform.gui

import net.benwoodworth.fastcraft.platform.player.FcPlayer
import net.kyori.adventure.text.Component

interface FcGui<TLayout : FcGuiLayout> {
    val layout: TLayout
    val player: FcPlayer
    var listener: Listener

    fun open()

    fun close()

    interface Listener {
        object Default : Listener

        fun onClose() {}
    }

    interface Factory {
        fun createChestGui(
            player: FcPlayer,
            title: Component? = null,
            height: Int = 3,
        ): FcGui<FcGuiLayout.Grid>
    }
}
