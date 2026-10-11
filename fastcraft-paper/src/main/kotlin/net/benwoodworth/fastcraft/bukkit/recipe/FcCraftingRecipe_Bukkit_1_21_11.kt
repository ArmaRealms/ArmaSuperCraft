package net.benwoodworth.fastcraft.bukkit.recipe

import net.benwoodworth.fastcraft.bukkit.player.FcPlayer_Bukkit
import net.benwoodworth.fastcraft.bukkit.player.bukkit
import net.benwoodworth.fastcraft.bukkit.world.FcItem_Bukkit
import net.benwoodworth.fastcraft.bukkit.world.FcItemStack_Bukkit
import net.benwoodworth.fastcraft.bukkit.world.bukkit
import net.benwoodworth.fastcraft.bukkit.world.create
import net.benwoodworth.fastcraft.platform.player.FcPlayer
import net.benwoodworth.fastcraft.platform.recipe.FcCraftingRecipe
import net.benwoodworth.fastcraft.platform.recipe.FcCraftingRecipePrepared
import net.benwoodworth.fastcraft.platform.recipe.FcIngredient
import net.benwoodworth.fastcraft.platform.world.FcItem
import net.benwoodworth.fastcraft.platform.world.FcItemStack
import net.benwoodworth.fastcraft.util.CancellableResult
import org.bukkit.Keyed
import org.bukkit.Material
import org.bukkit.Server
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.inventory.ComplexRecipe
import org.bukkit.inventory.CraftingInventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single Paper 1.21.11 recipe adapter.
 *
 * Recipes are identified by namespaced keys; only shaped and shapeless recipes
 * can be crafted automatically. Complex recipes are listed but never consumed.
 */
class FcCraftingRecipe_Bukkit_1_21_11(
    val recipe: Recipe,
    private val server: Server,
    private val preparedFactory: FcCraftingRecipePrepared_Bukkit.Factory,
    private val stackFactory: FcItemStack.Factory,
    private val viewFactory: CraftingInventoryViewFactory,
    fcPlayerOperations: FcPlayer.Operations,
    fcItemOperations: FcItem.Operations,
    stackOperations: FcItemStack.Operations,
) : FcCraftingRecipe_Bukkit,
    FcPlayer_Bukkit.Operations by fcPlayerOperations.bukkit,
    FcItem_Bukkit.Operations by fcItemOperations.bukkit,
    FcItemStack_Bukkit.Operations by stackOperations.bukkit {

    override val id: String
        get() = (recipe as Keyed).key.toString()

    override val group: String?
        get() = when (val currentRecipe = recipe) {
            is ShapedRecipe -> currentRecipe.group.takeUnless { it.isEmpty() }
            is ShapelessRecipe -> currentRecipe.group.takeUnless { it.isEmpty() }
            else -> null
        }

    override val ingredients: List<FcIngredient> by lazy {
        when (val currentRecipe = recipe) {
            is ShapedRecipe -> currentRecipe.shape.flatMapIndexed { row, line ->
                line.mapIndexedNotNull { column, symbol ->
                    currentRecipe.choiceMap[symbol]?.let { choice ->
                        FcIngredient_Bukkit_1_13(row * 3 + column, choice, stackOperations)
                    }
                }
            }

            is ShapelessRecipe -> currentRecipe.choiceList.mapIndexed { index, choice ->
                FcIngredient_Bukkit_1_13(index, choice, stackOperations)
            }

            is ComplexRecipe -> emptyList()
            else -> error("Unsupported crafting recipe: ${currentRecipe.javaClass.name}")
        }
    }

    override val exemplaryResult: FcItemStack
        get() = stackFactory.create(recipe.result?.clone() ?: ItemStack(Material.AIR))

    override fun prepare(
        player: FcPlayer,
        ingredients: Map<FcIngredient, FcItemStack>,
    ): CancellableResult<FcCraftingRecipePrepared> {
        if (recipe is ComplexRecipe) return CancellableResult.Cancelled

        val view = viewFactory.create(player.player, null, recipe)
        val craftingInventory = view.topInventory as CraftingInventory

        // Workbench raw slot 0 is the result, and the 3x3 matrix uses slots 1–9.
        val matrix = arrayOfNulls<ItemStack>(9)
        for ((ingredient, stack) in ingredients) {
            matrix[ingredient.slotIndex] = stack.itemStack.clone()
        }
        craftingInventory.matrix = matrix

        if (ingredients.all { (ingredient, stack) -> ingredient.matches(stack) }) {
            craftingInventory.result = recipe.result.clone()
        }

        server.pluginManager.callEvent(PrepareItemCraftEvent(craftingInventory, view, false))

        val result = craftingInventory.result
        if (result == null || result.type == Material.AIR || result.amount < 1) {
            return CancellableResult.Cancelled
        }

        val remnants = ingredients.values.mapNotNull { stack ->
            stack.type.craftingRemainingItem?.let { remainder ->
                stackFactory.create(ItemStack(remainder.material, stack.amount))
            }
        }
        val preview = listOf(stackFactory.create(result)) + remnants
        return CancellableResult(
            preparedFactory.create(player.player, this, ingredients, remnants, preview, view)
        )
    }

    override fun equals(other: Any?): Boolean =
        other is FcCraftingRecipe_Bukkit_1_21_11 && (recipe as Keyed).key == (other.recipe as Keyed).key

    override fun hashCode(): Int = (recipe as Keyed).key.hashCode()

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
                recipe, server, preparedFactory, stackFactory, viewFactory,
                playerOperations, itemOperations, stackOperations
            )
    }
}
