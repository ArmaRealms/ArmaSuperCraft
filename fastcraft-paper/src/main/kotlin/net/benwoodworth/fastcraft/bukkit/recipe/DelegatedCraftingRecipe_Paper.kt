package net.benwoodworth.fastcraft.bukkit.recipe

import net.benwoodworth.fastcraft.bukkit.api.DelegatedRecipeOffer
import net.benwoodworth.fastcraft.bukkit.world.FcItemStack_Bukkit
import net.benwoodworth.fastcraft.bukkit.world.bukkit
import net.benwoodworth.fastcraft.bukkit.world.create
import net.benwoodworth.fastcraft.platform.player.FcPlayer
import net.benwoodworth.fastcraft.platform.recipe.FcCraftingRecipe
import net.benwoodworth.fastcraft.platform.recipe.FcCraftingRecipePrepared
import net.benwoodworth.fastcraft.platform.recipe.FcDelegatedCraftingRecipePrepared
import net.benwoodworth.fastcraft.platform.recipe.FcDelegatedCraftResult
import net.benwoodworth.fastcraft.platform.recipe.FcIngredient
import net.benwoodworth.fastcraft.platform.world.FcItemStack
import net.benwoodworth.fastcraft.util.CancellableResult
import org.bukkit.inventory.ItemStack

/** An offer is already prepared: unlike vanilla recipes, no synthetic Bukkit event is created. */
internal class DelegatedCraftingRecipe_Paper(
    offer: DelegatedRecipeOffer,
    private val stackFactory: FcItemStack.Factory,
    stackOperations: FcItemStack.Operations,
    private val execute: (Int, Boolean) -> FcDelegatedCraftResult,
) : FcCraftingRecipe {
    private val requirements = offer.ingredients.map { it.clone() }
    private val output = stackFactory.create(offer.result.clone())
    private val bukkitStacks = stackOperations.bukkit

    override val id: String = offer.id.toString()
    override val group: String? = null
    override val ingredients: List<FcIngredient> = requirements.mapIndexed { index, item ->
        ExactIngredient(index, item, bukkitStacks)
    }
    override val exemplaryResult: FcItemStack get() = output

    fun prepared(): FcCraftingRecipePrepared = Prepared(
        this,
        ingredients.zip(requirements).associate { (ingredient, item) ->
            ingredient to stackFactory.create(item.clone())
        },
        listOf(output),
        execute,
    )

    // A delegated recipe cannot be prepared or crafted through the vanilla pipeline.
    override fun prepare(player: FcPlayer, ingredients: Map<FcIngredient, FcItemStack>): CancellableResult<FcCraftingRecipePrepared> =
        CancellableResult.Cancelled

    override fun equals(other: Any?): Boolean = other is DelegatedCraftingRecipe_Paper && id == other.id
    override fun hashCode(): Int = id.hashCode()

    private class ExactIngredient(
        private val position: Int,
        private val template: ItemStack,
        private val stackOperations: FcItemStack_Bukkit.Operations,
    ) : FcIngredient {
        override fun matches(itemStack: FcItemStack): Boolean = with(stackOperations) {
            itemStack.itemStack.isSimilar(template)
        }

        override fun equals(other: Any?): Boolean =
            other is ExactIngredient && position == other.position && template.isSimilar(other.template)

        override fun hashCode(): Int = 31 * position + template.type.hashCode()
    }

    private class Prepared(
        override val recipe: FcCraftingRecipe,
        override val ingredients: Map<FcIngredient, FcItemStack>,
        override val resultsPreview: List<FcItemStack>,
        private val execute: (Int, Boolean) -> FcDelegatedCraftResult,
    ) : FcDelegatedCraftingRecipePrepared {
        private var attempted = false

        // Fail closed if an older caller accidentally routes this to the synthetic CraftItemEvent.
        override fun craft(): CancellableResult<List<FcItemStack>> = CancellableResult.Cancelled

        override fun commit(repetitions: Int, dropResults: Boolean): FcDelegatedCraftResult {
            if (attempted || repetitions !in 1..64) return FcDelegatedCraftResult.UNAVAILABLE
            attempted = true
            return execute(repetitions, dropResults)
        }
    }
}
