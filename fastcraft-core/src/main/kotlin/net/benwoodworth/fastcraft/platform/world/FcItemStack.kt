package net.benwoodworth.fastcraft.platform.world

import net.kyori.adventure.text.Component

@JvmInline
value class FcItemStack(val value: Any) {
    interface Operations {
        val FcItemStack.type: FcItem
        fun FcItemStack.withType(type: FcItem): FcItemStack
        var FcItemStack.amount: Int
        val FcItemStack.name: Component
        val FcItemStack.lore: List<Component>

        val FcItemStack.hasMetadata: Boolean

        fun FcItemStack.copy(): FcItemStack
    }

    interface Factory {
        fun create(
            item: FcItem,
            amount: Int = 1,
        ): FcItemStack

        fun parseOrNull(itemStr: String, amount: Int = 1): FcItemStack?
    }
}
