package net.benwoodworth.fastcraft.bukkit.player

import io.mockk.every
import io.mockk.mockk
import net.benwoodworth.fastcraft.bukkit.util.CauseTracker
import net.benwoodworth.fastcraft.platform.player.FcPlayer
import net.benwoodworth.fastcraft.platform.server.FcTask
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginManager
import org.bukkit.scheduler.BukkitScheduler
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PhysicalWorkbenchEventTest {
    private val plugin = mockk<Plugin>()
    private val scheduler = mockk<BukkitScheduler>()
    private val tasks = mutableListOf<Runnable>()
    private val tracker = CauseTracker(scheduler, plugin)
    private val provider = mockk<FcPlayer_Bukkit.Provider>()
    private val player = mockk<Player>()
    private val wrappedPlayer = FcPlayer(player)
    private val pluginManager = mockk<PluginManager>(relaxed = true)
    private val events = FcPlayerEvents_Bukkit_Paper(
        plugin,
        provider,
        pluginManager,
        tracker,
        mockk<FcTask.Factory>(),
    )
    private val listener = events.EventListener()
    private var openings = 0

    init {
        every { scheduler.scheduleSyncDelayedTask(plugin, any<Runnable>(), 0L) } answers {
            tasks += secondArg<Runnable>()
            tasks.size
        }
        every { provider.getPlayer(player) } returns wrappedPlayer
        events.onOpenCraftingTableNaturally += { openings++ }
    }

    private fun interact(
        action: Action = Action.RIGHT_CLICK_BLOCK,
        type: Material = Material.CRAFTING_TABLE,
        result: Event.Result = Event.Result.ALLOW,
        hand: EquipmentSlot = EquipmentSlot.HAND,
    ): PlayerInteractEvent {
        val block = mockk<Block>()
        every { block.type } returns type
        val event = mockk<PlayerInteractEvent>()
        every { event.player } returns player
        every { event.action } returns action
        every { event.clickedBlock } returns block
        every { event.useInteractedBlock() } returns result
        every { event.hand } returns hand
        return event
    }

    private fun opening(
        type: InventoryType = InventoryType.WORKBENCH,
        cancelled: Boolean = false,
    ): InventoryOpenEvent {
        val inventory = mockk<Inventory>()
        every { inventory.type } returns type
        val event = mockk<InventoryOpenEvent>()
        every { event.player } returns player
        every { event.inventory } returns inventory
        every { event.isCancelled } returns cancelled
        return event
    }

    @Test
    fun onlyValidRightClickOnTableCanReplaceWorkbench() {
        listener.onPlayerInteract(interact(action = Action.LEFT_CLICK_BLOCK))
        listener.onInventoryOpen(opening())
        listener.onPlayerInteract(interact(type = Material.FURNACE))
        listener.onInventoryOpen(opening())
        listener.onPlayerInteract(interact(result = Event.Result.DENY))
        listener.onInventoryOpen(opening())
        assertEquals(0, openings)

        listener.onPlayerInteract(interact())
        listener.onInventoryOpen(opening())
        assertEquals(1, openings)
    }

    @Test
    fun twoHandsAndRepeatedInventoryOpenCauseOnlyOneReplacement() {
        listener.onPlayerInteract(interact(hand = EquipmentSlot.HAND))
        listener.onInventoryOpen(opening())
        listener.onPlayerInteract(interact(hand = EquipmentSlot.OFF_HAND))
        listener.onInventoryOpen(opening())
        assertEquals(1, openings)
    }

    @Test
    fun unrelatedOrCancelledInventoryOpeningInvalidatesTheCause() {
        listener.onPlayerInteract(interact())
        listener.onInventoryOpen(opening(type = InventoryType.CHEST))
        listener.onInventoryOpen(opening())
        assertEquals(0, openings)

        tasks.single().run()
        listener.onPlayerInteract(interact())
        listener.onInventoryOpen(opening(cancelled = true))
        listener.onInventoryOpen(opening())
        assertEquals(0, openings)
    }

    @Test
    fun virtualWorkbenchWithoutPhysicalClickIsNeverIntercepted() {
        listener.onInventoryOpen(opening())
        assertEquals(0, openings)
    }
}
