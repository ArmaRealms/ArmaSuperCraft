package net.benwoodworth.fastcraft.bukkit.world

import org.bukkit.Material
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockbukkit.mockbukkit.MockBukkitExtension
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExtendWith(MockBukkitExtension::class)
class PaperItemFactoryTest {
    private val factory = FcItem_Bukkit_1_13.Factory()

    @Test
    fun usesModernMaterialsForGuiItems() {
        assertEquals(Material.CRAFTING_TABLE, factory.craftingTable.value)
        assertEquals(Material.LIGHT_GRAY_STAINED_GLASS_PANE, factory.lightGrayStainedGlassPane.value)
    }

    @Test
    fun resolvesModernMaterialNamesWithoutLegacyData() {
        assertEquals(Material.NETHER_STAR, factory.parseOrNull("NETHER_STAR")?.value)
        assertNull(factory.parseOrNull("INVALID_FASTCRAFT_MATERIAL"))
    }
}
