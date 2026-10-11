package net.benwoodworth.fastcraft.bukkit.world

import net.benwoodworth.fastcraft.bukkit.text.BukkitLocalizer
import net.benwoodworth.fastcraft.bukkit.text.createTranslate
import net.benwoodworth.fastcraft.platform.text.FcText
import net.benwoodworth.fastcraft.platform.world.FcItem
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

object FcItem_Bukkit_Paper {
    @Singleton
    class Operations @Inject constructor(
        private val fcTextFactory: FcText.Factory,
        private val localizer: BukkitLocalizer,
        private val fcItemFactory: FcItem.Factory,
    ) : FcItem_Bukkit.Operations {
        override val FcItem.id: String
            get() = material.key.toString()

        override val FcItem.material: Material
            get() = value as Material

        private fun Material.getNameLocaleKey(prefix: String): String {
            return "$prefix.${key.namespace}.${key.key}"
        }

        override val FcItem.name: FcText
            get() {
                var localeKey = material.getNameLocaleKey("item")

                if (localizer.localize(localeKey, Locale.ENGLISH) == null) {
                    val blockKey = material.getNameLocaleKey("block")
                    if (localizer.localize(blockKey, Locale.ENGLISH) != null) {
                        localeKey = blockKey
                    }
                }

                return fcTextFactory.createTranslate(localeKey)
            }

        override val FcItem.maxAmount: Int
            get() = material.maxStackSize

        override val FcItem.craftingRemainingItem: FcItem?
            get() = material.craftingRemainingItem?.let { fcItemFactory.fromMaterial(it) }

        override fun FcItem.toItemStack(amount: Int): ItemStack {
            return ItemStack(material, amount)
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
