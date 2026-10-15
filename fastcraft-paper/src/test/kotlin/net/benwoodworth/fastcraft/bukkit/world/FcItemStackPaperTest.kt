package net.benwoodworth.fastcraft.bukkit.world

import io.mockk.mockk
import io.mockk.every
import net.benwoodworth.fastcraft.platform.world.FcItemStack
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.Server
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockbukkit.mockbukkit.MockBukkitExtension
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

@ExtendWith(MockBukkitExtension::class)
class FcItemStackPaperTest {
    private val itemFactory = FcItem_Bukkit_Paper.Factory()
    private val itemOperations = FcItem_Bukkit_Paper.Operations(itemFactory)
    private val stackFactory = FcItemStack_Bukkit_Paper.Factory(itemFactory, mockk<Server>(), itemOperations)
    private val operations = FcItemStack_Bukkit_Paper.Operations(
        itemFactory,
        itemOperations,
        stackFactory,
    )

    @Test
    fun preservesNativeAdventureNamesAndLore() {
        val custom = Component.text("Named", NamedTextColor.RED)
            .hoverEvent(HoverEvent.showText(Component.text("Tooltip")))
        val description = Component.text("Description", NamedTextColor.GREEN)
        // MockBukkit does not implement effectiveName; verify the native API boundary explicitly.
        val stack = mockk<ItemStack>()
        every { stack.effectiveName() } returns custom
        every { stack.lore() } returns listOf(description)

        with(operations) {
            val item = FcItemStack(stack)
            assertEquals(custom, item.name)
            assertEquals(
                listOf(description),
                item.lore,
            )
        }
    }

    @Test
    fun usesEffectiveNameForUnmodifiedItems() {
        val stack = mockk<ItemStack>()
        val name = Component.translatable("item.minecraft.diamond")
        every { stack.effectiveName() } returns name
        with(operations) {
            assertEquals(name, FcItemStack(stack).name)
        }
    }

    @Test
    fun changingTypeReturnsNewStackWithoutMutatingTheOriginal() {
        val original = ItemStack.of(Material.STONE, 3)
        original.editMeta { meta ->
            meta.customName(Component.text("Keep", NamedTextColor.GOLD))
            meta.lore(listOf(Component.text("Lore")))
        }

        with(operations) {
            val updated = FcItemStack(original).withType(itemFactory.fromMaterial(Material.DIRT))
            val newStack = updated.itemStack

            assertNotSame(original, newStack)
            assertEquals(Material.STONE, original.type)
            assertEquals(Material.DIRT, newStack.type)
            assertEquals(3, newStack.amount)
            assertEquals(original.itemMeta.customName(), newStack.itemMeta.customName())
            assertEquals(original.lore(), newStack.lore())
        }
    }
}
