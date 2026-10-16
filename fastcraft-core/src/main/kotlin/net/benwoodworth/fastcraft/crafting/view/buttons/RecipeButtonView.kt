package net.benwoodworth.fastcraft.crafting.view.buttons

import net.benwoodworth.fastcraft.Strings
import net.benwoodworth.fastcraft.crafting.model.FastCraftRecipe
import net.benwoodworth.fastcraft.crafting.model.ItemAmounts
import net.benwoodworth.fastcraft.platform.gui.FcGui
import net.benwoodworth.fastcraft.platform.gui.FcGuiButton
import net.benwoodworth.fastcraft.platform.gui.FcGuiClick
import net.benwoodworth.fastcraft.platform.world.FcItemStack
import net.benwoodworth.fastcraft.text.miniMessage
import net.kyori.adventure.text.Component
import java.util.*
import javax.inject.Inject
import javax.inject.Provider

class RecipeButtonView(
    private val button: FcGuiButton,
    private val locale: Locale,
    private val itemAmountsProvider: Provider<ItemAmounts>,
    fcItemStackOperations: FcItemStack.Operations,
) : FcItemStack.Operations by fcItemStackOperations {
    var fastCraftRecipe: FastCraftRecipe? = null

    var listener: Listener = Listener.Default

    init {
        button.listener = ButtonListener()
    }

    fun update() {
        val fastCraftRecipe = fastCraftRecipe
        if (fastCraftRecipe == null || !fastCraftRecipe.canCraft()) {
            button.clear()
            return
        }

        fastCraftRecipe.preparedRecipe.let { preparedRecipe ->
            val previewItem = preparedRecipe.resultsPreview.first()
            button.copyItem(previewItem)
            button.setAmount(previewItem.amount * fastCraftRecipe.multiplier)

            val newDescription = mutableListOf<Component>()

            // Results
            if (preparedRecipe.resultsPreview.count() > 1) {
                newDescription += miniMessage(
                    Strings.guiRecipeResults(locale)
                )

                val results = itemAmountsProvider.get()
                preparedRecipe.resultsPreview.forEach { result ->
                    results += result
                }

                val primaryResult = preparedRecipe.resultsPreview.first()
                val primaryResultAmount = results[primaryResult]
                results[primaryResult] = 0

                results.asMap().entries
                    .sortedByDescending { (_, amount) -> amount }
                    .let { listOf(AbstractMap.SimpleEntry(primaryResult, primaryResultAmount)) + it }
                    .forEach { (itemStack, amount) ->
                        val itemName = Component.empty().append(itemStack.name)
                            .append(if (itemStack.hasMetadata) Component.text("*") else Component.empty())
                        newDescription += miniMessage(
                            Strings.guiRecipeResultsItem(locale),
                            unparsed = mapOf("amount" to (amount * fastCraftRecipe.multiplier).toString()),
                            components = mapOf("item" to itemName),
                        )
                    }

                newDescription += Component.empty()
            }

            // Ingredients
            run {
                newDescription += miniMessage(
                    Strings.guiRecipeIngredients(locale)
                )

                val ingredients = itemAmountsProvider.get()
                preparedRecipe.ingredients.values.forEach { ingredient ->
                    ingredients += ingredient
                }

                ingredients.asMap().entries
                    .sortedByDescending { (_, amount) -> amount }
                    .forEach { (itemStack, amount) ->
                        val itemName = Component.empty().append(itemStack.name)
                            .append(if (itemStack.hasMetadata) Component.text("*") else Component.empty())
                        newDescription += miniMessage(
                            Strings.guiRecipeIngredientsItem(locale),
                            unparsed = mapOf("amount" to (amount * fastCraftRecipe.multiplier).toString()),
                            components = mapOf("item" to itemName),
                        )
                    }
            }

            val recipeId = fastCraftRecipe.preparedRecipe.recipe.id
            newDescription += miniMessage(Strings.guiRecipeId(locale), mapOf("id" to recipeId))

            if (previewItem.lore.any()) {
                newDescription += Component.empty()
                newDescription += previewItem.lore
            }

            button.setDescription(newDescription)
        }
    }

    private companion object {
        val CLICK_CRAFT = FcGuiClick.Primary()
        val CLICK_CRAFT_DROP = FcGuiClick.Drop()
    }

    interface Listener {
        object Default : Listener

        fun onCraft(button: RecipeButtonView, recipe: FastCraftRecipe, dropResults: Boolean) {}
    }

    private inner class ButtonListener : FcGuiButton.Listener {
        override fun onClick(gui: FcGui<*>, button: FcGuiButton, click: FcGuiClick) {
            val recipe = fastCraftRecipe
            if (recipe != null) {
                val action = when (click) {
                    CLICK_CRAFT -> {
                        { listener.onCraft(this@RecipeButtonView, recipe, false) }
                    }

                    CLICK_CRAFT_DROP -> {
                        { listener.onCraft(this@RecipeButtonView, recipe, true) }
                    }

                    else -> null
                }

                action?.let {
                    action()
                }
            }
        }
    }

    class Factory @Inject constructor(
        private val itemAmountsProvider: Provider<ItemAmounts>,
        private val fcItemStackOperations: FcItemStack.Operations,
    ) {
        fun create(button: FcGuiButton, locale: Locale): RecipeButtonView {
            return RecipeButtonView(
                button = button,
                locale = locale,
                itemAmountsProvider = itemAmountsProvider,
                fcItemStackOperations = fcItemStackOperations,
            )
        }
    }
}
