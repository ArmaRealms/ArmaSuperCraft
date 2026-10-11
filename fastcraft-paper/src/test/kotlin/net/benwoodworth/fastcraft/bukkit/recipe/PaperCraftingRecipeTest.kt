package net.benwoodworth.fastcraft.bukkit.recipe

import io.mockk.mockk
import net.benwoodworth.fastcraft.bukkit.player.FcPlayer_Bukkit
import net.benwoodworth.fastcraft.bukkit.world.FcItem_Bukkit
import net.benwoodworth.fastcraft.bukkit.world.FcItemStack_Bukkit
import net.benwoodworth.fastcraft.platform.player.FcPlayer
import net.benwoodworth.fastcraft.platform.world.FcItemStack
import net.benwoodworth.fastcraft.util.CancellableResult
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Server
import org.bukkit.entity.Player
import org.bukkit.inventory.ComplexRecipe
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockbukkit.mockbukkit.MockBukkitExtension
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

@ExtendWith(MockBukkitExtension::class)
class PaperCraftingRecipeTest {
    private val stackOperations = mockk<FcItemStack_Bukkit.Operations>()

    private fun wrap(recipe: Recipe) = FcCraftingRecipe_Bukkit_Paper(
        recipe = recipe,
        server = mockk<Server>(),
        preparedFactory = mockk(),
        stackFactory = mockk(),
        viewFactory = mockk(),
        fcPlayerOperations = mockk<FcPlayer_Bukkit.Operations>(),
        fcItemOperations = mockk<FcItem_Bukkit.Operations>(),
        stackOperations = stackOperations,
    )

    @Test
    fun shapedRecipePreservesGridPositionsAndGroup() {
        val recipe = ShapedRecipe(
            NamespacedKey.minecraft("fastcraft_test_shaped"),
            ItemStack(Material.DIAMOND),
        )
        recipe.shape("A ", " B")
        recipe.setIngredient('A', Material.STONE)
        recipe.setIngredient('B', Material.STICK)
        recipe.group = "fastcraft_test"

        val wrapped = wrap(recipe)
        assertEquals("minecraft:fastcraft_test_shaped", wrapped.id)
        assertEquals("fastcraft_test", wrapped.group)
        assertEquals(listOf(0, 4), wrapped.ingredients.map { it.slotIndex })
    }

    @Test
    fun shapelessRecipeUsesSequentialSlots() {
        val recipe = ShapelessRecipe(
            NamespacedKey.minecraft("fastcraft_test_shapeless"),
            ItemStack(Material.DIAMOND),
        )
        recipe.addIngredient(Material.STONE)
        recipe.addIngredient(Material.STICK)

        val wrapped = wrap(recipe)
        assertEquals(listOf(0, 1), wrapped.ingredients.map { it.slotIndex })
        assertEquals(null, wrapped.group)

        val wrappedAgain = wrap(recipe)
        assertEquals(wrapped, wrappedAgain)
        assertEquals(wrapped.hashCode(), wrappedAgain.hashCode())
    }

    @Test
    fun complexRecipesCannotBeAutomaticallyCrafted() {
        val recipe = mockk<ComplexRecipe>()
        io.mockk.every { recipe.key } returns NamespacedKey.minecraft("fastcraft_test_complex")
        val wrapped = wrap(recipe)

        assertTrue(wrapped.ingredients.isEmpty())
        assertEquals(null, wrapped.group)
        val player = FcPlayer(mockk<Player>())
        val result = wrapped.prepare(player, emptyMap())
        assertSame(CancellableResult.Cancelled, result)
    }
}
