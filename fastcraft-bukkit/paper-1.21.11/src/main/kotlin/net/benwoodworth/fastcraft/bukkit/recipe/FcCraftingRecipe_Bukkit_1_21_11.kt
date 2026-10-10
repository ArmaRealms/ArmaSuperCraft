package net.benwoodworth.fastcraft.bukkit.recipe

import net.benwoodworth.fastcraft.bukkit.player.player
import net.benwoodworth.fastcraft.bukkit.world.create
import net.benwoodworth.fastcraft.platform.player.FcPlayer
import net.benwoodworth.fastcraft.platform.recipe.FcCraftingRecipe
import net.benwoodworth.fastcraft.platform.recipe.FcCraftingRecipePrepared
import net.benwoodworth.fastcraft.platform.recipe.FcIngredient
import net.benwoodworth.fastcraft.platform.world.FcItem
import net.benwoodworth.fastcraft.platform.world.FcItemStack
import net.benwoodworth.fastcraft.util.CancellableResult
import org.bukkit.Material
import org.bukkit.Server
import org.bukkit.inventory.ComplexRecipe
import org.bukkit.inventory.CraftingInventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Paper 1.21 uses an InventoryView interface. The inherited prepare() from
 * Bukkit 1.7 invokes methods on the old abstract class and cannot run here.
 */
class FcCraftingRecipe_Bukkit_1_21_11(
    recipe: Recipe,
    private val paperServer: Server,
    private val paperPreparedFactory: FcCraftingRecipePrepared_Bukkit.Factory,
    private val paperStackFactory: FcItemStack.Factory,
    private val paperViewFactory: CraftingInventoryViewFactory,
    fcPlayerOperations: FcPlayer.Operations,
    fcItemOperations: FcItem.Operations,
    fcItemStackOperations: FcItemStack.Operations,
) : FcCraftingRecipe_Bukkit_1_15(
    recipe,
    paperServer,
    paperPreparedFactory,
    paperStackFactory,
    paperViewFactory,
    fcPlayerOperations,
    fcItemOperations,
    fcItemStackOperations,
) {
    override val group: String?
        get() = if (recipe is ComplexRecipe) null else super.group

    override fun hashCode(): Int =
        if (recipe is ComplexRecipe) recipe.key.hashCode() else super.hashCode()

    override fun equals(other: Any?): Boolean =
        if (recipe is ComplexRecipe) {
            other is FcCraftingRecipe_Bukkit_1_21_11 &&
                other.recipe is ComplexRecipe && recipe.key == other.recipe.key
        } else super.equals(other)

    override fun prepare(
        player: FcPlayer,
        ingredients: Map<FcIngredient, FcItemStack>,
    ): CancellableResult<FcCraftingRecipePrepared> {
        if (recipe is ComplexRecipe) return CancellableResult.Cancelled

        val view = paperViewFactory.create(player.player, null, recipe)
        val craftingInventory = view.topInventory as CraftingInventory

        // Matrix indices are 0..8; the view's result occupies raw slot 0.
        val matrix = arrayOfNulls<ItemStack>(9)
        for ((ingredient, stack) in ingredients) {
            matrix[ingredient.slotIndex] = stack.itemStack.clone()
        }
        craftingInventory.matrix = matrix

        if (ingredients.all { (ingredient, stack) -> ingredient.matches(stack) }) {
            craftingInventory.result = recipe.result.clone()
        }

        paperServer.pluginManager.callEvent(
            org.bukkit.event.inventory.PrepareItemCraftEvent(craftingInventory, view, false)
        )

        val result = craftingInventory.result
        if (result == null || result.type == Material.AIR || result.amount < 1) {
            return CancellableResult.Cancelled
        }

        val remnants = ingredients.values.mapNotNull { ingredient ->
            ingredient.type.craftingRemainingItem?.let {
                paperStackFactory.create(ItemStack(it.material, ingredient.amount))
            }
        }
        val preview = listOf(paperStackFactory.create(result)) + remnants

        return CancellableResult(
            paperPreparedFactory.create(
                player.player,
                this,
                ingredients,
                remnants,
                preview,
                view,
            )
        )
    }

    @Singleton
    class Factory @Inject constructor(
        private val server: Server,
        private val preparedFactory: FcCraftingRecipePrepared_Bukkit.Factory,
        private val stackFactory: FcItemStack.Factory,
        private val viewFactory: CraftingInventoryViewFactory,
        private val playerOperations: FcPlayer.Operations,
        private val itemOperations: FcItem.Operations,
        private val stackOperations: FcItemStack.Operations,
    ) : FcCraftingRecipe_Bukkit.Factory {
        override fun create(recipe: Recipe): FcCraftingRecipe =
            FcCraftingRecipe_Bukkit_1_21_11(
                recipe,
                server,
                preparedFactory,
                stackFactory,
                viewFactory,
                playerOperations,
                itemOperations,
                stackOperations,
            )
    }
}
