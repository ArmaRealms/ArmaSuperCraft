package net.benwoodworth.fastcraft.bukkit.recipe

import net.benwoodworth.fastcraft.platform.player.FcPlayer
import net.benwoodworth.fastcraft.platform.recipe.FcCraftingRecipe
import net.benwoodworth.fastcraft.platform.recipe.FcCraftingRecipePrepared
import net.benwoodworth.fastcraft.platform.recipe.FcRecipeProvider
import org.bukkit.Server
import org.bukkit.Keyed
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
    private val delegated: DelegatedRecipeBridge_Paper,
) : FcRecipeProvider {
    override fun getCraftingRecipes(): Sequence<FcCraftingRecipe> {
        val claimedIds = delegated.claimedIds()
        return server.recipeIterator().asSequence()
            .filter { recipe ->
                recipe is ShapedRecipe || recipe is ShapelessRecipe || recipe is ComplexRecipe
            }
            .filterNot { (it as Keyed).key.toString() in claimedIds }
            .map { recipeFactory.create(it) }
    }

    override fun getDelegatedRecipes(player: FcPlayer): List<FcCraftingRecipePrepared> =
        delegated.getOffers(player.value as org.bukkit.entity.Player)

    override fun hasDelegatedProviders(): Boolean = delegated.hasActiveProviders()
}
