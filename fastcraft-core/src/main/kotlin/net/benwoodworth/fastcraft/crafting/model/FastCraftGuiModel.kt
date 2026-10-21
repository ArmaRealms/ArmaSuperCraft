package net.benwoodworth.fastcraft.crafting.model

import net.benwoodworth.fastcraft.platform.player.FcPlayer
import net.benwoodworth.fastcraft.platform.player.FcPlayerEvents
import net.benwoodworth.fastcraft.platform.player.FcPlayerInventoryChangeEvent
import net.benwoodworth.fastcraft.platform.recipe.FcCraftingRecipePrepared
import net.benwoodworth.fastcraft.platform.recipe.FcDelegatedCraftingRecipePrepared
import net.benwoodworth.fastcraft.platform.recipe.FcDelegatedCraftResult
import net.benwoodworth.fastcraft.platform.server.FcTask
import net.benwoodworth.fastcraft.platform.world.FcItemStack
import net.benwoodworth.fastcraft.util.CancellableResult
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

class FastCraftGuiModel(
    val player: FcPlayer,
    private val itemAmountsProvider: Provider<ItemAmounts>,
    private val craftableRecipeFinder: CraftableRecipeFinder,
    private val playerEvents: FcPlayerEvents,
    private val taskFactory: FcTask.Factory,
    fcPlayerOperations: FcPlayer.Operations,
    private val fcItemStackOperations: FcItemStack.Operations,
) : FcPlayer.Operations by fcPlayerOperations,
    FcItemStack.Operations by fcItemStackOperations {

    var craftAmount: Int? = null
    val recipes: MutableList<FastCraftRecipe?> = mutableListOf()

    val inventoryItemAmounts: ItemAmounts = itemAmountsProvider.get()
    var listener: Listener? = null

    private val craftableRecipeFinderListener = CraftableRecipeFinderListener()
    private var closed = false
    private var openingVanilla = false
    private var crafting = false

    init {
        playerEvents.onPlayerInventoryChange += ::onPlayerInventoryChange
    }

    fun close() {
        if (closed) return
        closed = true
        playerEvents.onPlayerInventoryChange -= ::onPlayerInventoryChange
    }

    fun updateInventoryItemAmounts() {
        inventoryItemAmounts.clear()

        player.inventory.storage.forEach { slot ->
            slot.itemStack?.let { itemStack -> inventoryItemAmounts += itemStack }
        }
    }

    fun updateCraftAmounts() {
        recipes.forEach { recipe ->
            recipe?.setCraftAmount(craftAmount)
        }
    }

    fun refreshRecipes() {
        updateInventoryItemAmounts()

        craftableRecipeFinder.cancel(player)
        recipes.clear()
        listener?.onRecipesChange(recipes)
        if (!closed) craftableRecipeFinder.loadRecipes(player, craftableRecipeFinderListener)
    }

    private fun onPlayerInventoryChange(event: FcPlayerInventoryChangeEvent) {
        if (event.player == player && !closed && !crafting) {
            if (craftableRecipeFinder.hasDelegatedProviders()) {
                refreshRecipes() // Player-specific offers can change with inventory and progression.
            } else {
                updateInventoryItemAmounts()
                listener?.onRecipesChange(recipes)
            }
        }
    }

    private inner class CraftableRecipeFinderListener : CraftableRecipeFinder.Listener {
        override fun onNewRecipesLoaded(newRecipes: List<FcCraftingRecipePrepared>) {
            if (closed) return
            newRecipes
//                .uniqueBy { it.ingredients.values.toSet() to it.resultsPreview.toSet() }
                .map { FastCraftRecipe(this@FastCraftGuiModel, it, fcItemStackOperations) }
                .forEach { recipes += it }

            listener?.onRecipesChange(recipes)
        }
    }

    /**
     * @return `true` iff successful.
     */
    fun craftRecipe(recipeIndex: Int, dropItems: Boolean, expected: FastCraftRecipe? = null): Boolean {
        if (closed || crafting) return false
        val recipe = recipes.getOrNull(recipeIndex) ?: return false
        if (expected != null && recipe !== expected) return false

        updateInventoryItemAmounts()
        if (!recipe.canCraft()) {
            if (recipe.preparedRecipe is FcDelegatedCraftingRecipePrepared) refreshRecipes()
            return false
        }

        val delegated = recipe.preparedRecipe as? FcDelegatedCraftingRecipePrepared
        if (delegated != null) {
            crafting = true
            recipes[recipeIndex] = null // Block reentrancy and stale button clicks.
            return try {
                delegated.commit(recipe.multiplier, dropItems) == FcDelegatedCraftResult.SUCCESS
            } finally {
                crafting = false
                if (!closed) refreshRecipes()
            }
        }

        recipes[recipeIndex] = null // Prevent preparedRecipe.craft() from being crafted twice
        val craftedItems = when (val craftResult = recipe.preparedRecipe.craft()) {
            is CancellableResult.Cancelled -> return false
            is CancellableResult.Result -> craftResult.result
        }

        removeItems(recipe.preparedRecipe.ingredients.values, recipe.multiplier)

        repeat(recipe.multiplier) {
            val itemsToGive = craftedItems.map { it.copy() }
            player.giveItems(itemsToGive, dropItems)
        }

        val updatedRecipe = recipe.preparedRecipe.recipe.prepare(player, recipe.preparedRecipe.ingredients)
        recipes[recipeIndex] = when (updatedRecipe) {
            is CancellableResult.Cancelled -> null
            is CancellableResult.Result -> FastCraftRecipe(this, updatedRecipe.result, fcItemStackOperations)
        }

        return true
    }

    private fun removeItems(items: Collection<FcItemStack>, multiplier: Int) {
        val removeAmounts = itemAmountsProvider.get()
        items.forEach { itemStack ->
            removeAmounts[itemStack] += itemStack.amount * multiplier
        }

        if (removeAmounts.isEmpty()) {
            return
        }

        val removeFromSlots = player.inventory.storage.asSequence()
            .filter { it.itemStack != null && it.itemStack!!.amount > 0 }
            .sortedBy { it.itemStack!!.amount }

        for (slot in removeFromSlots) {
            val itemStack = slot.itemStack!!
            val removeAmount = removeAmounts[itemStack]

            when {
                itemStack.amount <= 0 -> Unit
                removeAmount <= 0 -> Unit
                removeAmount >= itemStack.amount -> {
                    removeAmounts[itemStack] = removeAmount - itemStack.amount
                    slot.itemStack = null
                }

                removeAmount < itemStack.amount -> {
                    removeAmounts[itemStack] = 0
                    itemStack.amount -= removeAmount
                }

                else -> throw IllegalStateException()
            }
        }
    }

    fun openCraftingTable() {
        if (closed || openingVanilla) return
        openingVanilla = true

        // InventoryClickEvent forbids opening another inventory synchronously.
        taskFactory.startTask(delayTicks = 1L) {
            openingVanilla = false
            if (closed || !player.isOnline) return@startTask
            close()
            player.openCraftingTable()
        }
    }

    interface Listener {
        fun onRecipesChange(recipes: List<FastCraftRecipe?>) {}
    }

    @Singleton
    class Factory @Inject constructor(
        private val itemAmountsProvider: Provider<ItemAmounts>,
        private val craftableRecipeFinder: CraftableRecipeFinder,
        private val playerEvents: FcPlayerEvents,
        private val taskFactory: FcTask.Factory,
        private val fcPlayerOperations: FcPlayer.Operations,
        private val fcItemStackOperations: FcItemStack.Operations,
    ) {
        fun create(player: FcPlayer): FastCraftGuiModel {
            return FastCraftGuiModel(
                player = player,
                itemAmountsProvider = itemAmountsProvider,
                craftableRecipeFinder = craftableRecipeFinder,
                playerEvents = playerEvents,
                taskFactory = taskFactory,
                fcPlayerOperations = fcPlayerOperations,
                fcItemStackOperations = fcItemStackOperations,
            )
        }
    }
}
