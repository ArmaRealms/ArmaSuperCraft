package net.benwoodworth.fastcraft.bukkit.recipe

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.benwoodworth.fastcraft.bukkit.api.DelegatedCraftResult
import net.benwoodworth.fastcraft.bukkit.api.DelegatedRecipeOffer
import net.benwoodworth.fastcraft.bukkit.api.DelegatedRecipeProvider
import net.benwoodworth.fastcraft.bukkit.world.FcItemStack_Bukkit
import net.benwoodworth.fastcraft.platform.recipe.FcDelegatedCraftingRecipePrepared
import net.benwoodworth.fastcraft.platform.recipe.FcDelegatedCraftResult
import net.benwoodworth.fastcraft.platform.world.FcItemStack
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Server
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.RegisteredServiceProvider
import org.bukkit.plugin.ServicesManager
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockbukkit.mockbukkit.MockBukkitExtension
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@ExtendWith(MockBukkitExtension::class)
class DelegatedRecipeBridgeTest {
    private val id = NamespacedKey("mcmmoaddon", "alchemy_test")
    private val player = mockk<Player>()
    private val plugin = mockk<Plugin>()
    private val server = mockk<Server>()
    private val services = mockk<ServicesManager>()
    private val provider = mockk<DelegatedRecipeProvider>()
    private val registration = mockk<RegisteredServiceProvider<DelegatedRecipeProvider>>()
    private val factory = mockk<FcItemStack_Bukkit.Factory>()
    private val stacks = mockk<FcItemStack_Bukkit.Operations>()
    private val bridge = DelegatedRecipeBridge_Paper(server, factory, stacks)

    private val offer = DelegatedRecipeOffer(
        id,
        ItemStack(Material.POTION, 8),
        listOf(ItemStack(Material.POTION, 4), ItemStack(Material.REDSTONE, 2)),
    )

    init {
        every { server.isPrimaryThread } returns true
        every { server.servicesManager } returns services
        every { services.getRegistrations(DelegatedRecipeProvider::class.java) } returns listOf(registration)
        every { registration.plugin } returns plugin
        every { registration.provider } returns provider
        every { plugin.isEnabled } returns true
        every { provider.claimedRecipeIds } returns setOf(id)
        every { provider.getOffers(player) } returns listOf(offer)
        every { player.isOnline } returns true
        every { factory.create(any<ItemStack>()) } answers { FcItemStack(firstArg<ItemStack>()) }
    }

    @Test
    fun previewPreservesQuantitiesAndDelegatesExactlyOnce() {
        val prepared = bridge.getOffers(player).single() as FcDelegatedCraftingRecipePrepared
        assertEquals("mcmmoaddon:alchemy_test", prepared.recipe.id)
        assertEquals(listOf(4, 2), prepared.ingredients.values.map { (it.value as ItemStack).amount })
        assertEquals(8, (prepared.resultsPreview.single().value as ItemStack).amount)

        every { provider.craft(player, id, 2, false) } returns DelegatedCraftResult.SUCCESS
        assertEquals(FcDelegatedCraftResult.SUCCESS, prepared.commit(2, false))
        assertEquals(FcDelegatedCraftResult.UNAVAILABLE, prepared.commit(2, false))
        verify(exactly = 1) { provider.craft(player, id, 2, false) }
        assertSame(net.benwoodworth.fastcraft.util.CancellableResult.Cancelled, prepared.craft())
    }

    @Test
    fun changedOrMissingPreviewRejectsWithoutCrafting() {
        val prepared = bridge.getOffers(player).single() as FcDelegatedCraftingRecipePrepared
        every { provider.getOffers(player) } returns listOf(offer.copy(result = ItemStack(Material.POTION, 16)))

        assertEquals(FcDelegatedCraftResult.REJECTED, prepared.commit(1, false))
        verify(exactly = 0) { provider.craft(any(), any(), any(), any()) }
    }

    @Test
    fun disabledProviderCannotCommitAndItsIdStaysReserved() {
        val prepared = bridge.getOffers(player).single() as FcDelegatedCraftingRecipePrepared
        every { plugin.isEnabled } returns false

        assertEquals(FcDelegatedCraftResult.UNAVAILABLE, prepared.commit(1, true))
        assertTrue("mcmmoaddon:alchemy_test" in bridge.claimedIds())
        assertFalse(bridge.hasActiveProviders())
        verify(exactly = 0) { provider.craft(any(), any(), any(), any()) }
    }

    @Test
    fun duplicateClaimIsHiddenRatherThanSelectingAnArbitraryProvider() {
        val otherProvider = mockk<DelegatedRecipeProvider>()
        val otherPlugin = mockk<Plugin>()
        val otherRegistration = mockk<RegisteredServiceProvider<DelegatedRecipeProvider>>()
        every { otherRegistration.provider } returns otherProvider
        every { otherRegistration.plugin } returns otherPlugin
        every { otherPlugin.isEnabled } returns true
        every { otherProvider.claimedRecipeIds } returns setOf(id)
        every { otherProvider.getOffers(any()) } returns listOf(offer)
        every { services.getRegistrations(DelegatedRecipeProvider::class.java) } returns listOf(registration, otherRegistration)

        assertTrue(bridge.getOffers(player).isEmpty())
        assertTrue("mcmmoaddon:alchemy_test" in bridge.claimedIds())
    }

    @Test
    fun invalidOffersAreNotShown() {
        every { provider.getOffers(player) } returns listOf(
            offer.copy(ingredients = emptyList()),
            offer.copy(result = ItemStack(Material.AIR)),
        )
        assertTrue(bridge.getOffers(player).isEmpty())
    }
}
