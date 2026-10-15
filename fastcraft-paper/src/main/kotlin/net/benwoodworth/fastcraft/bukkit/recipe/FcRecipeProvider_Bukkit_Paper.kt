package net.benwoodworth.fastcraft.bukkit.recipe

import net.benwoodworth.fastcraft.platform.recipe.FcCraftingRecipe
import net.benwoodworth.fastcraft.platform.recipe.FcRecipeProvider
import org.bukkit.Server
import org.bukkit.inventory.ComplexRecipe
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import javax.inject.Inject
import javax.inject.Singleton

/** Discover only recipes supported by the Paper 1.21.11 crafting adapter. */
@Singleton
class FcRecipeProvider_Bukkit_Paper @Inject constructor(
    private val server: Server,
    private val recipeFactory: FcCraftingRecipe_Bukkit.Factory,
) : FcRecipeProvider {
    override fun getCraftingRecipes(): Sequence<FcCraftingRecipe> =
        server.recipeIterator().asSequence()
            .filter { recipe ->
                recipe is ShapedRecipe || recipe is ShapelessRecipe || recipe is ComplexRecipe
            }
            .map { recipeFactory.create(it) }
}
