package net.benwoodworth.fastcraft.bukkit.world

import net.benwoodworth.fastcraft.platform.world.FcItem
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object FcItem_Bukkit {
    interface Operations : FcItem.Operations {
        val FcItem.material: Material
        fun FcItem.toItemStack(amount: Int = 1): ItemStack
    }

    interface Factory : FcItem.Factory {
        fun fromMaterial(material: Material): FcItem
    }
}

val FcItem.Operations.bukkit: FcItem_Bukkit.Operations
    get() = this as FcItem_Bukkit.Operations

fun FcItem.Factory.fromMaterial(material: Material): FcItem {
    return (this as FcItem_Bukkit.Factory).fromMaterial(material)
}

