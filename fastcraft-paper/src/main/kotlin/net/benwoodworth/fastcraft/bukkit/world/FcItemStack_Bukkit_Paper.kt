package net.benwoodworth.fastcraft.bukkit.world

import net.benwoodworth.fastcraft.platform.text.FcText
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.benwoodworth.fastcraft.platform.world.FcItem
import net.benwoodworth.fastcraft.platform.world.FcItemStack
import org.bukkit.Server
import org.bukkit.inventory.ItemStack
import javax.inject.Inject
import javax.inject.Singleton

/** Paper 1.21.11 ItemStack adapter; no legacy item data/durability plumbing. */
class FcItemStack_Bukkit_Paper {
    @Singleton
    class Operations @Inject constructor(
        private val fcItemFactory: FcItem.Factory,
        private val fcTextFactory: FcText.Factory,
        fcItemOperations: FcItem.Operations,
        private val fcItemStackFactory: FcItemStack.Factory,
    ) : FcItemStack_Bukkit.Operations,
        FcItem_Bukkit.Operations by fcItemOperations.bukkit {

        override val FcItemStack.itemStack: ItemStack
            get() = value as ItemStack

        override var FcItemStack.type: FcItem
            get() = fcItemFactory.fromMaterial(itemStack.type)
            // withType() returns a new stack, but this wrapper must mutate its existing instance.
            @Suppress("DEPRECATION")
            set(value) {
                itemStack.type = value.material
            }

        override var FcItemStack.amount: Int
            get() = itemStack.amount
            set(value) {
                itemStack.amount = value
            }

        override val FcItemStack.name: FcText
            get() = itemStack
                .takeIf { it.hasItemMeta() }
                ?.itemMeta
                ?.takeIf { it.hasCustomName() }
                ?.customName()
                ?.let { fcTextFactory.create(LegacyComponentSerializer.legacySection().serialize(it)) }
                ?: type.name

        override val FcItemStack.lore: List<FcText>
            get() = itemStack
                .takeIf { it.hasItemMeta() }
                ?.itemMeta
                ?.takeIf { it.hasLore() }
                ?.lore()
                ?.map { fcTextFactory.create(LegacyComponentSerializer.legacySection().serialize(it)) }
                ?: emptyList()

        override val FcItemStack.hasMetadata: Boolean
            get() = itemStack.hasItemMeta()

        override fun FcItemStack.copy(): FcItemStack =
            fcItemStackFactory.create(itemStack.clone())
    }

    @Singleton
    class Factory @Inject constructor(
        private val fcItemFactory: FcItem.Factory,
        private val server: Server,
        fcItemOperations: FcItem.Operations,
    ) : FcItemStack_Bukkit.Factory,
        FcItem_Bukkit.Operations by fcItemOperations.bukkit {

        override fun create(item: FcItem, amount: Int): FcItemStack =
            create(item.toItemStack(amount))

        override fun parseOrNull(itemStr: String, amount: Int): FcItemStack? {
            val dataIndex = itemStr.indexOf('{')
            val materialId = if (dataIndex == -1) itemStr else itemStr.substring(0, dataIndex)
            val item = fcItemFactory.parseOrNull(materialId) ?: return null
            val itemStack = item.toItemStack(amount)

            if (dataIndex != -1) {
                // Keep the existing NBT text syntax used by saved FastCraft config.
                @Suppress("DEPRECATION")
                server.unsafe.modifyItemStack(itemStack, itemStr.substring(dataIndex))
            }
            return create(itemStack)
        }

        override fun create(itemStack: ItemStack): FcItemStack = FcItemStack(itemStack)
    }
}
