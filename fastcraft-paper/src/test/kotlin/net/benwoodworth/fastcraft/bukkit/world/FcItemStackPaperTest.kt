package net.benwoodworth.fastcraft.bukkit.world

import io.mockk.mockk
import net.benwoodworth.fastcraft.bukkit.text.FcText_Bukkit
import net.benwoodworth.fastcraft.bukkit.text.FcText_Bukkit_Paper
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
import kotlin.test.assertIs
import kotlin.test.assertNotSame

@ExtendWith(MockBukkitExtension::class)
class FcItemStackPaperTest {
    private val textFactory = FcText_Bukkit_Paper.Factory()
    private val itemFactory = FcItem_Bukkit_Paper.Factory()
    private val itemOperations = FcItem_Bukkit_Paper.Operations(textFactory, itemFactory)
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
        val stack = ItemStack.of(Material.STONE)
        stack.editMeta { meta ->
            meta.customName(custom)
            meta.lore(listOf(description))
        }

        with(operations) {
            val item = FcItemStack(stack)
            assertEquals(stack.effectiveName(), assertIs<FcText_Bukkit.AdventureText>(item.name).component)
            assertEquals(
                listOf(description),
                item.lore.map { assertIs<FcText_Bukkit.AdventureText>(it).component },
            )
        }
    }

    @Test
    fun usesEffectiveNameForUnmodifiedItems() {
        val stack = ItemStack.of(Material.DIAMOND)
        with(operations) {
            assertEquals(stack.effectiveName(), assertIs<FcText_Bukkit.AdventureText>(FcItemStack(stack).name).component)
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
