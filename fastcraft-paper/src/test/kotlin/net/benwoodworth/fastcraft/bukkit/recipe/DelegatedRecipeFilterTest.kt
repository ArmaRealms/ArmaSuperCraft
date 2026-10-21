package net.benwoodworth.fastcraft.bukkit.recipe

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.benwoodworth.fastcraft.platform.recipe.FcCraftingRecipe
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Server
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapelessRecipe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockbukkit.mockbukkit.MockBukkitExtension
import kotlin.test.assertEquals

@ExtendWith(MockBukkitExtension::class)
class DelegatedRecipeFilterTest {
    @Test
    fun claimedVanillaRecipeDoesNotReachSyntheticPreparation() {
        val managed = ShapelessRecipe(NamespacedKey("mcmmoaddon", "alchemy_test"), ItemStack(Material.POTION))
        managed.addIngredient(Material.POTION)
        val vanilla = ShapelessRecipe(NamespacedKey.minecraft("safe_recipe"), ItemStack(Material.STONE))
        vanilla.addIngredient(Material.COBBLESTONE)

        val server = mockk<Server>()
        val bridge = mockk<DelegatedRecipeBridge_Paper>()
        val factory = mockk<FcCraftingRecipe_Bukkit.Factory>()
        val wrapped = mockk<FcCraftingRecipe>()
        every { bridge.claimedIds() } returns setOf("mcmmoaddon:alchemy_test")
        every { server.recipeIterator() } answers { listOf(managed, vanilla).iterator() }
        every { factory.create(vanilla) } returns wrapped

        val provider = FcRecipeProvider_Bukkit_Paper(server, factory, bridge)
        assertEquals(listOf(wrapped), provider.getCraftingRecipes().toList())
        verify(exactly = 0) { factory.create(managed) }
        verify(exactly = 1) { factory.create(vanilla) }
    }

    @Test
    fun unclaimedVanillaRecipeKeepsTheExistingAdapter() {
        val vanilla = ShapelessRecipe(NamespacedKey.minecraft("plain_recipe"), ItemStack(Material.STONE))
        vanilla.addIngredient(Material.COBBLESTONE)
        val server = mockk<Server>()
        val bridge = mockk<DelegatedRecipeBridge_Paper>()
        val factory = mockk<FcCraftingRecipe_Bukkit.Factory>()
        val wrapped = mockk<FcCraftingRecipe>()
        every { bridge.claimedIds() } returns emptySet()
        every { server.recipeIterator() } answers { listOf(vanilla).iterator() }
        every { factory.create(vanilla) } returns wrapped

        assertEquals(listOf(wrapped), FcRecipeProvider_Bukkit_Paper(server, factory, bridge).getCraftingRecipes().toList())
    }
}
