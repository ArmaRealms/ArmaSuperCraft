package net.benwoodworth.fastcraft.platform.gui

import net.benwoodworth.fastcraft.platform.world.FcItem
import net.benwoodworth.fastcraft.platform.world.FcItemStack
import net.kyori.adventure.text.Component

interface FcGuiButton {
    var listener: Listener

    fun setItem(item: FcItem)
    fun setAmount(amount: Int)
    fun setText(text: Component)
    fun setDescription(description: List<Component>)
    fun setProgress(progress: Double?)

    fun clear()

    fun copyItem(itemStack: FcItemStack)

    fun hideItemDetails()

    interface Listener {
        object Default : Listener

        fun onClick(gui: FcGui<*>, button: FcGuiButton, click: FcGuiClick) {}
    }
}
