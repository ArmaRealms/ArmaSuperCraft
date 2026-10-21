package net.benwoodworth.fastcraft.bukkit.recipe

import net.benwoodworth.fastcraft.bukkit.api.DelegatedCraftResult
import net.benwoodworth.fastcraft.bukkit.api.DelegatedRecipeOffer
import net.benwoodworth.fastcraft.bukkit.api.DelegatedRecipeProvider
import net.benwoodworth.fastcraft.platform.recipe.FcCraftingRecipePrepared
import net.benwoodworth.fastcraft.platform.recipe.FcDelegatedCraftResult
import net.benwoodworth.fastcraft.platform.world.FcItemStack
import org.bukkit.Material
import org.bukkit.Server
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.RegisteredServiceProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Paper service boundary. Once a plugin claims a vanilla key, it stays reserved for the
 * server lifetime, even after the service unloads: no silent fallback to synthetic crafting.
 */
@Singleton
class DelegatedRecipeBridge_Paper @Inject constructor(
    private val server: Server,
    private val stackFactory: FcItemStack.Factory,
    private val stackOperations: FcItemStack.Operations,
) {
    private val reservedIds = mutableSetOf<String>()

    private fun registrations(): List<RegisteredServiceProvider<DelegatedRecipeProvider>> =
        server.servicesManager.getRegistrations(DelegatedRecipeProvider::class.java)
            .filter { it.plugin.isEnabled }

    fun hasActiveProviders(): Boolean = registrations().isNotEmpty()

    fun claimedIds(): Set<String> {
        check(server.isPrimaryThread) { "Delegated recipes must be queried on the server thread" }
        for (registration in registrations()) {
            try {
                reservedIds += registration.provider.claimedRecipeIds.map { it.toString() }
            } catch (e: Exception) {
                registration.plugin.logger.warning("SuperCraft failed to read delegated claims: ${e.message}")
            }
        }
        return reservedIds.toSet()
    }

    fun getOffers(player: Player): List<FcCraftingRecipePrepared> {
        check(server.isPrimaryThread) { "Delegated recipes must be queried on the server thread" }

        val registered = registrations()
        val owner = mutableMapOf<String, RegisteredServiceProvider<DelegatedRecipeProvider>>()
        val conflicts = mutableSetOf<String>()
        for (entry in registered) {
            val ids = try {
                entry.provider.claimedRecipeIds.map { it.toString() }.toSet()
            } catch (e: Exception) {
                entry.plugin.logger.warning("SuperCraft failed to read delegated claims: ${e.message}")
                emptySet()
            }
            reservedIds += ids
            for (id in ids) {
                if (owner.putIfAbsent(id, entry) != null) conflicts += id
            }
        }

        val seen = mutableSetOf<String>()
        val prepared = mutableListOf<FcCraftingRecipePrepared>()
        for (entry in registered) {
            val offers = try {
                entry.provider.getOffers(player).toList()
            } catch (e: Exception) {
                entry.plugin.logger.warning("SuperCraft failed to list delegated offers: ${e.message}")
                emptyList()
            }
            for (offer in offers) {
                val id = offer.id.toString()
                if (id in conflicts || owner[id] !== entry || !seen.add(id) || !valid(offer)) continue
                // Freeze ItemStack metadata so the preview cannot be modified by a provider later.
                val snapshot = offer.copy(
                    result = offer.result.clone(),
                    ingredients = offer.ingredients.map { it.clone() },
                )
                prepared += DelegatedCraftingRecipe_Paper(snapshot, stackFactory, stackOperations) { count, drop ->
                    commit(entry, player, snapshot, count, drop)
                }.prepared()
            }
        }
        return prepared
    }

    private fun valid(offer: DelegatedRecipeOffer): Boolean =
        offer.result.type != Material.AIR && offer.result.amount in 1..64 &&
            offer.ingredients.size in 1..9 && offer.ingredients.all {
                it.type != Material.AIR && it.amount > 0
            }

    private fun sameItems(a: ItemStack, b: ItemStack): Boolean =
        a.amount == b.amount && a.isSimilar(b)

    private fun sameOffer(a: DelegatedRecipeOffer, b: DelegatedRecipeOffer): Boolean =
        a.id == b.id && sameItems(a.result, b.result) &&
            a.ingredients.size == b.ingredients.size &&
            a.ingredients.zip(b.ingredients).all { (x, y) -> sameItems(x, y) }

    private fun commit(
        registration: RegisteredServiceProvider<DelegatedRecipeProvider>,
        player: Player,
        preview: DelegatedRecipeOffer,
        count: Int,
        dropResults: Boolean,
    ): FcDelegatedCraftResult {
        if (!server.isPrimaryThread || !player.isOnline || count !in 1..64) {
            return FcDelegatedCraftResult.UNAVAILABLE
        }
        if (registrations().none { it === registration }) return FcDelegatedCraftResult.UNAVAILABLE
        val provider = registration.provider
        return try {
            if (preview.id !in provider.claimedRecipeIds) return FcDelegatedCraftResult.UNAVAILABLE
            // A preview is not proof of eligibility. The provider must also revalidate
            // permissions, exact inventory items and output capacity inside craft().
            if (provider.getOffers(player).none { sameOffer(preview, it) }) {
                return FcDelegatedCraftResult.REJECTED
            }
            when (provider.craft(player, preview.id, count, dropResults)) {
                DelegatedCraftResult.SUCCESS -> FcDelegatedCraftResult.SUCCESS
                DelegatedCraftResult.REJECTED -> FcDelegatedCraftResult.REJECTED
                DelegatedCraftResult.UNAVAILABLE -> FcDelegatedCraftResult.UNAVAILABLE
            }
        } catch (e: Exception) {
            registration.plugin.logger.warning("SuperCraft delegated craft failed for ${preview.id}: ${e.message}")
            FcDelegatedCraftResult.UNAVAILABLE
        }
    }
}
