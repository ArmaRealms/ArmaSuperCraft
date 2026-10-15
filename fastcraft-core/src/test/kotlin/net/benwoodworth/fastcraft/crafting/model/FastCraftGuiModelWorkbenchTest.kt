package net.benwoodworth.fastcraft.crafting.model

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.benwoodworth.fastcraft.platform.player.FcPlayer
import net.benwoodworth.fastcraft.platform.player.FcPlayerEvents
import net.benwoodworth.fastcraft.platform.player.FcPlayerInventoryChangeEvent
import net.benwoodworth.fastcraft.platform.server.FcTask
import net.benwoodworth.fastcraft.platform.world.FcItemStack
import net.benwoodworth.fastcraft.events.HandlerSet
import org.junit.jupiter.api.Test
import javax.inject.Provider
import kotlin.test.assertEquals

class FastCraftGuiModelWorkbenchTest {
    private val player = FcPlayer(Any())
    private val playerOperations = mockk<FcPlayer.Operations>(relaxed = true)
    private val taskFactory = mockk<FcTask.Factory>()
    private val playerEvents = mockk<FcPlayerEvents>()
    private val callbacks = mutableListOf<(FcTask) -> Unit>()
    private var online = true

    private val model: FastCraftGuiModel

    init {
        every { playerEvents.onPlayerInventoryChange } returns HandlerSet<FcPlayerInventoryChangeEvent>()
        with(playerOperations) {
            every { player.isOnline } answers { online }
        }
        every { taskFactory.startTask(any(), any(), any(), any()) } answers {
            callbacks += arg<(FcTask) -> Unit>(3)
            mockk<FcTask>()
        }
        model = FastCraftGuiModel(
            player = player,
            itemAmountsProvider = Provider { mockk<ItemAmounts>() },
            craftableRecipeFinder = mockk(),
            playerEvents = playerEvents,
            taskFactory = taskFactory,
            fcPlayerOperations = playerOperations,
            fcItemStackOperations = mockk<FcItemStack.Operations>(),
        )
    }

    @Test
    fun defersWorkbenchOpeningAndIgnoresRepeatedClicks() {
        model.openCraftingTable()
        model.openCraftingTable()

        assertEquals(1, callbacks.size)
        verify(exactly = 1) { taskFactory.startTask(false, 1L, 0L, any()) }
        with(playerOperations) {
            verify(exactly = 0) { player.openCraftingTable() }
            callbacks.single()(mockk())
            verify(exactly = 1) { player.openCraftingTable() }
        }
        model.openCraftingTable()
        assertEquals(1, callbacks.size)
    }

    @Test
    fun closingTheGuiBeforeNextTickCancelsPendingOpening() {
        model.openCraftingTable()
        model.close()
        callbacks.single()(mockk())

        with(playerOperations) {
            verify(exactly = 0) { player.openCraftingTable() }
        }
    }

    @Test
    fun offlinePlayerNeverOpensWorkbench() {
        model.openCraftingTable()
        online = false
        callbacks.single()(mockk())

        with(playerOperations) {
            verify(exactly = 0) { player.openCraftingTable() }
        }
    }
}
