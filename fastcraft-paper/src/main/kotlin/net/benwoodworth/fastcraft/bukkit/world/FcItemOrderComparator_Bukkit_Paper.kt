package net.benwoodworth.fastcraft.bukkit.world

import net.benwoodworth.fastcraft.platform.world.FcItem
import net.benwoodworth.fastcraft.platform.world.FcItemOrderComparator
import javax.inject.Inject
import javax.inject.Singleton

/** Material order resource files for versions 1.7–1.16 no longer exist.
 * Paper materials are ordered deterministically by their namespaced ID.
 */
@Singleton
class FcItemOrderComparator_Bukkit_Paper @Inject constructor(
    fcItemOperations: FcItem.Operations,
) : FcItemOrderComparator {
    private val comparator = fcItemOperations.bukkit.run {
        compareBy<FcItem> { it.id }
    }

    override fun compare(item0: FcItem, item1: FcItem): Int =
        comparator.compare(item0, item1)
}
