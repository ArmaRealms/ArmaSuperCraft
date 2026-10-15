package net.benwoodworth.fastcraft.bukkit.world

import io.mockk.mockk
import net.benwoodworth.fastcraft.bukkit.text.FcText_Bukkit
import net.benwoodworth.fastcraft.bukkit.text.FcText_Bukkit_Paper
import net.benwoodworth.fastcraft.platform.world.FcItemStack
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockbukkit.mockbukkit.MockBukkitExtension
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

@ExtendWith(MockBukkitExtension::class)
class FcItemStackPaperTest {
    private val textFactory = FcText_Bukkit_Paper.Factory()
    private val itemFactory = FcItem_Bukkit_Paper.Factory()
    private val operations = FcItemStack_Bukkit_Paper.Operations(
        itemFactory,
        textFactory,
        FcItem_Bukkit_Paper.Operations(textFactory, itemFactory),
        mockk(),
    )

    @Test
    fun readsAdventureNameAndLoreWithLegacyColorCodes() {
        val stack = ItemStack(Material.STONE)
        stack.editMeta { meta ->
            meta.customName(Component.text("Named", NamedTextColor.RED))
            meta.lore(listOf(Component.text("Description", NamedTextColor.GREEN)))
        }

        with(operations) {
            val item = FcItemStack(stack)
            assertEquals("§cNamed", assertIs<FcText_Bukkit.Component.Text>(item.name).text)
            assertEquals(
                "§aDescription",
                assertIs<FcText_Bukkit.Component.Text>(item.lore.single()).text,
            )
        }
    }

    @Test
    fun changingMaterialKeepsTheOriginalStackReference() {
        val stack = ItemStack(Material.STONE)
        val item = FcItemStack(stack)

        with(operations) {
            item.type = itemFactory.fromMaterial(Material.DIAMOND)
        }

        assertSame(stack, item.value)
        assertEquals(Material.DIAMOND, stack.type)
    }
}
