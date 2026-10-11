package net.benwoodworth.fastcraft.bukkit.recipe

import net.benwoodworth.fastcraft.bukkit.world.create
import net.benwoodworth.fastcraft.platform.recipe.FcCraftingRecipe
import net.benwoodworth.fastcraft.platform.recipe.FcCraftingRecipePrepared
import net.benwoodworth.fastcraft.platform.recipe.FcIngredient
import net.benwoodworth.fastcraft.platform.world.FcItemStack
import net.benwoodworth.fastcraft.util.CancellableResult
import org.bukkit.Material
import org.bukkit.Server
import org.bukkit.Statistic
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.CraftingInventory
import org.bukkit.inventory.InventoryView
import javax.inject.Inject
import javax.inject.Singleton

class FcCraftingRecipePrepared_Bukkit_Paper(
    private val player: Player,
    override val recipe: FcCraftingRecipe,
    override val ingredients: Map<FcIngredient, FcItemStack>,
    private val remnants: List<FcItemStack>,
    override val resultsPreview: List<FcItemStack>,
    private val view: InventoryView,
    private val stackFactory: FcItemStack.Factory,
    private val server: Server,
) : FcCraftingRecipePrepared_Bukkit {
    private var crafted = false

    override fun craft(): CancellableResult<List<FcItemStack>> {
        check(!crafted) { "Only callable once" }
        crafted = true

        val inventory = view.topInventory as CraftingInventory
        val craftedRecipe = inventory.recipe ?: return CancellableResult.Cancelled

        // Paper 1.21.11 uses raw slot 0 for the workbench result.
        val event = CraftItemEvent(
            craftedRecipe,
            view,
            InventoryType.SlotType.RESULT,
            0,
            ClickType.SHIFT_LEFT,
            InventoryAction.MOVE_TO_OTHER_INVENTORY,
        )
        server.pluginManager.callEvent(event)

        val result = inventory.result
        if (event.isCancelled || result == null || result.type == Material.AIR || result.amount < 1) {
            return CancellableResult.Cancelled
        }

        try {
            player.incrementStatistic(Statistic.CRAFT_ITEM, result.type, result.amount)
        } catch (_: IllegalArgumentException) {
            // Custom recipe results may not be tracked by vanilla statistics.
        }

        return CancellableResult(listOf(stackFactory.create(result)) + remnants)
    }

    @Singleton
    class Factory @Inject constructor(
        private val stackFactory: FcItemStack.Factory,
        private val server: Server,
    ) : FcCraftingRecipePrepared_Bukkit.Factory {
        override fun create(
            player: Player,
            recipe: FcCraftingRecipe,
            ingredients: Map<FcIngredient, FcItemStack>,
            ingredientRemnants: List<FcItemStack>,
            resultsPreview: List<FcItemStack>,
            preparedCraftingView: InventoryView,
        ): FcCraftingRecipePrepared = FcCraftingRecipePrepared_Bukkit_Paper(
            player,
            recipe,
            ingredients,
            ingredientRemnants,
            resultsPreview,
            preparedCraftingView,
            stackFactory,
            server,
        )
    }
}
