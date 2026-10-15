package net.benwoodworth.fastcraft.bukkit.world

import net.benwoodworth.fastcraft.bukkit.text.createTranslate
import net.benwoodworth.fastcraft.platform.text.FcText
import net.benwoodworth.fastcraft.platform.world.FcItem
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import javax.inject.Inject
import javax.inject.Singleton

object FcItem_Bukkit_Paper {
    @Singleton
    class Operations @Inject constructor(
        private val fcTextFactory: FcText.Factory,
        private val fcItemFactory: FcItem.Factory,
    ) : FcItem_Bukkit.Operations {
        override val FcItem.id: String
            get() = material.key.toString()

        override val FcItem.material: Material
            get() = value as Material

        override val FcItem.name: FcText
            get() = fcTextFactory.createTranslate(material.translationKey())

        override val FcItem.maxAmount: Int
            get() = material.maxStackSize

        override val FcItem.craftingRemainingItem: FcItem?
            get() = material.craftingRemainingItem?.let { fcItemFactory.fromMaterial(it) }

        override fun FcItem.toItemStack(amount: Int): ItemStack {
            return if (material.isAir) ItemStack.empty() else ItemStack.of(material, amount)
        }
    }

    @Singleton
    class Factory @Inject constructor() : FcItem_Bukkit.Factory {
        override val air: FcItem
            get() = fromMaterial(Material.AIR)

        override val ironSword: FcItem
            get() = fromMaterial(Material.IRON_SWORD)

        override val anvil: FcItem
            get() = fromMaterial(Material.ANVIL)

        override val netherStar: FcItem
            get() = fromMaterial(Material.NETHER_STAR)

        override val stoneButton: FcItem
            get() = fromMaterial(Material.STONE_BUTTON)

        override fun fromMaterial(material: Material): FcItem = FcItem(material)

        override val craftingTable: FcItem
            get() = fromMaterial(Material.CRAFTING_TABLE)

        override val lightGrayStainedGlassPane: FcItem
            get() = fromMaterial(Material.LIGHT_GRAY_STAINED_GLASS_PANE)

        override fun parseOrNull(id: String): FcItem? {
            return Material.matchMaterial(id)
                ?.let { fromMaterial(it) }
        }
    }
}
