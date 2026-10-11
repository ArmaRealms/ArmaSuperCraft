package net.benwoodworth.fastcraft.bukkit.recipe

import io.mockk.every
import io.mockk.mockk
import org.bukkit.Material
import org.bukkit.Server
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.CraftingInventory
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockbukkit.mockbukkit.MockBukkitExtension
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

// Paper 1.21.11 requires RegistryAccess even for InventoryType and ItemStack.
// MockBukkit installs the service-backed registry before each test.
@ExtendWith(MockBukkitExtension::class)
class CraftingInventoryViewFactory12111Test {
    @Test
    fun `uses correct matrix and result slots`() {
        val slots = arrayOfNulls<ItemStack>(10)
        val base = mockk<Inventory>()
        every { base.getItem(any()) } answers { slots[firstArg()] }
        every { base.setItem(any(), any()) } answers {
            slots[firstArg<Int>()] = secondArg()
        }
        every { base.setItem(any(), null) } answers {
            slots[firstArg<Int>()] = null
        }
        every { base.size } returns 10
        val server = mockk<Server>()
        every { server.createInventory(null, InventoryType.WORKBENCH) } returns base
        val player = mockk<Player>()
        every { player.inventory } returns mockk()

        val view = CraftingInventoryViewFactory_1_21_11(server).create(player, null, null)
        val inventory = view.topInventory as CraftingInventory

        inventory.matrix = arrayOf(ItemStack(Material.DIAMOND), ItemStack(Material.STICK))
        inventory.result = ItemStack(Material.DIAMOND_SWORD)

        assertEquals(Material.DIAMOND_SWORD, slots[0]?.type)
        assertEquals(Material.DIAMOND, slots[1]?.type)
        assertEquals(Material.STICK, slots[2]?.type)
        assertNull(slots[3])
        assertEquals(9, inventory.matrix.size)
        assertEquals(Material.DIAMOND_SWORD, inventory.result?.type)
        assertEquals(InventoryType.WORKBENCH, view.type)
        assertEquals(InventoryType.SlotType.RESULT, view.getSlotType(0))
        assertEquals(InventoryType.SlotType.CRAFTING, view.getSlotType(1))
        assertEquals(9, view.convertSlot(10))
        assertEquals(0, view.convertSlot(37))
    }

    @Test
    fun `delegates inventory iterators`() {
        val entries = mutableListOf(ItemStack(Material.STONE))
        val base = mockk<Inventory>()
        every { base.iterator(0) } returns entries.listIterator()
        val server = mockk<Server>()
        every { server.createInventory(null, InventoryType.WORKBENCH) } returns base

        val view = CraftingInventoryViewFactory_1_21_11(server).create(mockk(), null, null)
        val inventory = view.topInventory as CraftingInventory

        assertEquals(Material.STONE, inventory.iterator(0).next().type)
    }

    @Test
    fun `delegates Paper holder snapshots`() {
        val snapshot = mockk<InventoryHolder>()
        val live = mockk<InventoryHolder>()
        val base = mockk<Inventory>()
        every { base.getHolder(true) } returns snapshot
        every { base.getHolder(false) } returns live
        val server = mockk<Server>()
        every { server.createInventory(null, InventoryType.WORKBENCH) } returns base

        val view = CraftingInventoryViewFactory_1_21_11(server).create(mockk(), null, null)
        val inventory = view.topInventory as CraftingInventory

        assertSame(snapshot, inventory.getHolder(true))
        assertSame(live, inventory.getHolder(false))
    }
}
