package net.benwoodworth.fastcraft.bukkit.api

import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Optional, Paper-facing FastCraft recipe service.
 *
 * Register this service through Bukkit's ServicesManager with the providing Plugin as owner.
 * All methods run on the main server thread. The provider owns validation and the entire
 * inventory transaction: consume inputs, verify the destination and deliver outputs.
 * FastCraft never mutates items for delegated recipes or emits synthetic craft events.
 *
 * Providers must return REJECTED/UNAVAILABLE without mutations, and SUCCESS only after the
 * complete transaction commits. Exceptions after partial mutations violate this contract.
 * The implementation must revalidate permissions, progression and exact ItemMeta/PDC
 * at commit time; getOffers() is a preview, not authorization.
 */
interface DelegatedRecipeProvider {
    /** All vanilla recipe IDs owned by this provider, even if hidden for a particular player. */
    val claimedRecipeIds: Set<NamespacedKey>

    /** Offers currently visible to this player; results and ingredients are snapshots. */
    fun getOffers(player: Player): Collection<DelegatedRecipeOffer>

    /** Execute repetitions in one atomic operation (1..64), not individual partial crafts. */
    fun craft(player: Player, id: NamespacedKey, repetitions: Int, dropResults: Boolean): DelegatedCraftResult
}

/**
 * A single craft's preview. ItemStack amounts represent exact required quantities
 * (including amounts greater than one); items must carry their complete metadata.
 */
data class DelegatedRecipeOffer(
    val id: NamespacedKey,
    val result: ItemStack,
    val ingredients: List<ItemStack>,
)

enum class DelegatedCraftResult {
    SUCCESS,
    REJECTED,
    UNAVAILABLE,
}
