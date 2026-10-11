package net.benwoodworth.fastcraft.bukkit.recipe

import org.bukkit.Server
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.CraftingInventory
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.InventoryView
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.MenuType
import org.bukkit.inventory.Recipe
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Paper 1.21 uses an interface for InventoryView. Older factories extend an
 * abstract class and cannot be linked on this API.
 *
 * Views built here are synthetic: they are only used to dispatch crafting
 * events; they must never open a real workbench window for the player.
 */
@Singleton
class CraftingInventoryViewFactory_1_21_11 @Inject constructor(
    private val server: Server,
) : CraftingInventoryViewFactory {
    override fun create(
        player: Player,
        inventoryHolder: InventoryHolder?,
        recipe: Recipe?,
    ): InventoryView = SyntheticView(
        player,
        SyntheticCraftingInventory(
            server.createInventory(inventoryHolder, InventoryType.WORKBENCH),
            recipe,
        ),
    )

    private class SyntheticCraftingInventory(
        private val delegate: Inventory,
        private val craftingRecipe: Recipe?,
    ) : CraftingInventory, Inventory by delegate {
        // Workbench slot 0 is the result. Slots 1-9 contain the 3x3 matrix.
        override fun getRecipe(): Recipe? = craftingRecipe

        override fun getResult(): ItemStack? = delegate.getItem(0)

        override fun setResult(newResult: ItemStack?) {
            delegate.setItem(0, newResult)
        }

        override fun getMatrix(): Array<ItemStack?> =
            Array(9) { index -> delegate.getItem(index + 1) }

        override fun setMatrix(contents: Array<out ItemStack?>) {
            require(contents.size <= 9) { "matrix contents too large" }
            for (index in 0 until 9) {
                delegate.setItem(index + 1, contents.getOrNull(index))
            }
        }

        override fun getHolder(useSnapshot: Boolean): InventoryHolder? =
            delegate.getHolder(useSnapshot)
    }

    private class SyntheticView(
        private val viewer: Player,
        private val craftingInventory: CraftingInventory,
    ) : InventoryView {
        override fun getPlayer(): HumanEntity = viewer
        override fun getTopInventory(): Inventory = craftingInventory
        override fun getBottomInventory(): Inventory = viewer.inventory
        override fun getType(): InventoryType = InventoryType.WORKBENCH

        override fun getTitle(): String = "Crafting"
        override fun getOriginalTitle(): String = "Crafting"

        override fun setTitle(title: String) {
            throw UnsupportedOperationException("Synthetic crafting view cannot change its title")
        }

        override fun getMenuType(): MenuType = MenuType.CRAFTING

        override fun countSlots(): Int = craftingInventory.size + 36

        override fun getInventory(rawSlot: Int): Inventory? = when (rawSlot) {
            in 0 until craftingInventory.size -> craftingInventory
            in craftingInventory.size until countSlots() -> viewer.inventory
            else -> null
        }

        override fun convertSlot(rawSlot: Int): Int {
            if (rawSlot < craftingInventory.size) return rawSlot
            val bottomSlot = rawSlot - craftingInventory.size
            return if (bottomSlot < 27) bottomSlot + 9 else bottomSlot - 27
        }

        override fun getSlotType(slot: Int): InventoryType.SlotType = when {
            slot == 0 -> InventoryType.SlotType.RESULT
            slot in 1 until craftingInventory.size -> InventoryType.SlotType.CRAFTING
            slot in craftingInventory.size + 27 until countSlots() -> InventoryType.SlotType.QUICKBAR
            else -> InventoryType.SlotType.CONTAINER
        }

        override fun getItem(slot: Int): ItemStack? =
            getInventory(slot)?.getItem(convertSlot(slot))

        override fun setItem(slot: Int, item: ItemStack?) {
            getInventory(slot)?.setItem(convertSlot(slot), item)
        }

        override fun getCursor(): ItemStack = viewer.itemOnCursor

        override fun setCursor(item: ItemStack?) {
            viewer.setItemOnCursor(item)
        }

        override fun setProperty(prop: InventoryView.Property, value: Int): Boolean = false

        // A synthetic view must never open or close the player's real GUI.
        override fun open() {}
        override fun close() {}
    }
}
