package net.benwoodworth.fastcraft.bukkit.gui

import net.benwoodworth.fastcraft.bukkit.util.updateMeta
import net.benwoodworth.fastcraft.bukkit.world.FcItem_Bukkit
import net.benwoodworth.fastcraft.bukkit.world.FcItemStack_Bukkit
import net.benwoodworth.fastcraft.bukkit.world.bukkit
import net.benwoodworth.fastcraft.platform.gui.FcGuiButton
import net.benwoodworth.fastcraft.platform.world.FcItem
import net.benwoodworth.fastcraft.platform.world.FcItemStack
import net.kyori.adventure.text.Component
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.ItemStack
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlin.properties.Delegates.observable

class FcGuiButton_Bukkit_Paper(
    private val inventory: Inventory,
    private val slotIndex: Int,
    locale: Locale,
    fcItemOperations: FcItem.Operations,
    fcItemStackOperations: FcItemStack.Operations,
) : FcGuiButton_Bukkit,
    FcItem_Bukkit.Operations by fcItemOperations.bukkit,
    FcItemStack_Bukkit.Operations by fcItemStackOperations.bukkit {

    protected var hideItemDetails: Boolean = false
    protected var _text: Component? = null
    protected var _description: List<Component>? = null

    protected var isProgressSet = false
    protected var _progress: Double? = null

    protected var itemStack: ItemStack by observable(ItemStack.empty()) { _, _, _ ->
        updateSlot()
    }

    override var listener: FcGuiButton.Listener = FcGuiButton.Listener.Default

    override fun setItem(item: FcItem) {
        // Empty slots have a count of zero; a visible button needs one item.
        itemStack = when {
            item.material.isAir -> ItemStack.empty()
            itemStack.amount == 0 -> ItemStack.of(item.material)
            else -> itemStack.withType(item.material)
        }

        updateDisplayName()
        updateLore()
        updateItemDetails()
        updateSlot()
    }

    override fun setAmount(amount: Int) {
        itemStack.amount = amount
        updateSlot()
    }

    override fun setText(text: Component) {
        this._text = text

        updateDisplayName()
        updateSlot()
    }

    override fun setDescription(description: List<Component>) {
        this._description = description

        updateLore()
        updateSlot()
    }

    override fun setProgress(progress: Double?) {
        isProgressSet = true
        this._progress = progress

        updateDamage()
        updateSlot()
    }

    override var locale: Locale by observable(locale) { _, _, _ ->
        updateDisplayName()
        updateLore()
        updateSlot()
    }

    override fun copyItem(itemStack: FcItemStack) {
        this@FcGuiButton_Bukkit_Paper.itemStack = itemStack.itemStack.clone()

        _text = itemStack.name
        _description = itemStack.lore
        hideItemDetails = false
    }

    override fun clear() {
        itemStack = ItemStack.empty()
        _text = Component.empty()
        _description = emptyList()
        _progress = null
        hideItemDetails = false
    }

    override fun hideItemDetails() {
        hideItemDetails = true
        updateItemDetails()
        updateSlot()
    }

    private fun updateItemDetails() {
        if (hideItemDetails) {
            itemStack.updateMeta {
                addItemFlags(
                    ItemFlag.HIDE_ENCHANTS,
                    ItemFlag.HIDE_ATTRIBUTES,
                    ItemFlag.HIDE_UNBREAKABLE,
                    ItemFlag.HIDE_DESTROYS,
                    ItemFlag.HIDE_PLACED_ON,
                )
            }
        }
    }

    protected fun calculateDurability(progress: Double?, maxDurability: Int): Int {
        return when (progress) {
            null -> 0
            in 0.0..1.0 -> ((1.0 - progress) * (maxDurability - 1) + 1).roundToInt()
            else -> 0
        }
    }

    private fun updateDamage() {
        if (!isProgressSet) return

        val maxDurability = itemStack.type.maxDurability.toInt()

        itemStack.updateMeta {
            if (this is Damageable) {
                damage = calculateDurability(_progress, maxDurability)
            }
        }
    }

    private fun updateSlot() {
        inventory.setItem(slotIndex, itemStack)
    }

    private fun updateDisplayName() {
        _text?.let { text ->
            itemStack.updateMeta {
                customName(text)
            }
        }
    }

    private fun updateLore() {
        _description?.let { description ->
            itemStack.updateMeta {
                lore(description)
            }
        }
    }

    @Singleton
    class Factory @Inject constructor(
        private val fcItemOperations: FcItem.Operations,
        private val fcItemStackOperations: FcItemStack.Operations,
    ) : FcGuiButton_Bukkit.Factory {
        override fun create(inventory: Inventory, slotIndex: Int, locale: Locale): FcGuiButton {
            return FcGuiButton_Bukkit_Paper(
                inventory = inventory,
                slotIndex = slotIndex,
                locale = locale,
                fcItemOperations = fcItemOperations,
                fcItemStackOperations = fcItemStackOperations,
            )
        }
    }
}
