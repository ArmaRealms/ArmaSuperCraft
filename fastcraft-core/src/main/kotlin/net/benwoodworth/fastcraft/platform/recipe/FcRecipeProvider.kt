package net.benwoodworth.fastcraft.platform.recipe

import net.benwoodworth.fastcraft.platform.player.FcPlayer

interface FcRecipeProvider {
    fun getCraftingRecipes(): Sequence<FcCraftingRecipe>

    /** Already-prepared provider recipes; never execute them as vanilla crafts. */
    fun getDelegatedRecipes(player: FcPlayer): List<FcCraftingRecipePrepared> = emptyList()

    /** Whether inventory changes must recalculate player-specific offers. */
    fun hasDelegatedProviders(): Boolean = false
}
