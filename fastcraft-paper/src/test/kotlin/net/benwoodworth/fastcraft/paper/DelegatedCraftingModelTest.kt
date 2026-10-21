package net.benwoodworth.fastcraft.paper

import io.mockk.every
import io.mockk.just
import io.mockk.Runs
import io.mockk.mockk
import io.mockk.verify
import net.benwoodworth.fastcraft.crafting.model.CraftableRecipeFinder
import net.benwoodworth.fastcraft.crafting.model.FastCraftGuiModel
import net.benwoodworth.fastcraft.crafting.model.FastCraftRecipe
import net.benwoodworth.fastcraft.crafting.model.ItemAmounts
import net.benwoodworth.fastcraft.events.HandlerSet
import net.benwoodworth.fastcraft.platform.player.FcPlayer
import net.benwoodworth.fastcraft.platform.player.FcPlayerEvents
import net.benwoodworth.fastcraft.platform.player.FcPlayerInventory
import net.benwoodworth.fastcraft.platform.player.FcPlayerInventoryChangeEvent
import net.benwoodworth.fastcraft.platform.recipe.FcDelegatedCraftingRecipePrepared
import net.benwoodworth.fastcraft.platform.recipe.FcDelegatedCraftResult
import net.benwoodworth.fastcraft.platform.recipe.FcIngredient
import net.benwoodworth.fastcraft.platform.server.FcTask
import net.benwoodworth.fastcraft.platform.world.FcItemStack
import org.junit.jupiter.api.Test
import javax.inject.Provider
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DelegatedCraftingModelTest {
    private val player = FcPlayer(Any())
    private val output = FcItemStack(Any())
    private val input = FcItemStack(Any())
    private val ingredient = mockk<FcIngredient>()
    private val playerOps = mockk<FcPlayer.Operations>(relaxed = true)
    private val stackOps = mockk<FcItemStack.Operations>()
    private val finder = mockk<CraftableRecipeFinder>(relaxed = true)
    private val events = mockk<FcPlayerEvents>()
    private val amounts = mockk<ItemAmounts>()
    private val prepared = mockk<FcDelegatedCraftingRecipePrepared>()
    private val model: FastCraftGuiModel

    init {
        every { events.onPlayerInventoryChange } returns HandlerSet<FcPlayerInventoryChangeEvent>()
        val inventory = mockk<FcPlayerInventory>()
        every { inventory.storage } returns emptyList()
        with(playerOps) {
            every { player.inventory } returns inventory
        }
        every { amounts.clear() } just Runs
        every { amounts.copy() } returns amounts
        every { amounts[any()] } returns 64
        every { amounts[any()] = any() } just Runs
        with(stackOps) {
            every { output.amount } returns 4
            every { input.amount } returns 4
        }
        every { prepared.resultsPreview } returns listOf(output)
        every { prepared.ingredients } returns mapOf(ingredient to input)
        every { prepared.commit(1, false) } returns FcDelegatedCraftResult.SUCCESS
        model = FastCraftGuiModel(
            player, Provider { amounts }, finder, events, mockk<FcTask.Factory>(),
            playerOps, stackOps,
        )
    }

    @Test
    fun successNeitherRunsSyntheticEventNorTouchesItemsInFastCraft() {
        val recipe = FastCraftRecipe(model, prepared, stackOps)
        model.recipes += recipe

        assertTrue(model.craftRecipe(0, false, recipe))
        assertFalse(model.craftRecipe(0, false, recipe))
        verify(exactly = 1) { prepared.commit(1, false) }
        verify(exactly = 0) { prepared.craft() }
        with(playerOps) {
            verify(exactly = 0) { player.giveItems(any(), any()) }
        }
    }

    @Test
    fun staleRecipeReferenceCannotCraftAReplacement() {
        val old = FastCraftRecipe(model, prepared, stackOps)
        val replacement = FastCraftRecipe(model, prepared, stackOps)
        model.recipes += replacement

        assertFalse(model.craftRecipe(0, false, old))
        verify(exactly = 0) { prepared.commit(any(), any()) }
    }

    @Test
    fun providerFailureDoesNotFallBackToVanilla() {
        every { prepared.commit(1, false) } returns FcDelegatedCraftResult.REJECTED
        val recipe = FastCraftRecipe(model, prepared, stackOps)
        model.recipes += recipe

        assertFalse(model.craftRecipe(0, false, recipe))
        verify(exactly = 0) { prepared.craft() }
        with(playerOps) {
            verify(exactly = 0) { player.giveItems(any(), any()) }
        }
    }
}
