package net.benwoodworth.fastcraft.paper

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.benwoodworth.fastcraft.FastCraft
import net.benwoodworth.fastcraft.FastCraftConfig
import net.benwoodworth.fastcraft.Permissions
import net.benwoodworth.fastcraft.api.FastCraftApi
import net.benwoodworth.fastcraft.api.FastCraftPreferences
import net.benwoodworth.fastcraft.crafting.FastCraftGui
import net.benwoodworth.fastcraft.events.HandlerSet
import net.benwoodworth.fastcraft.platform.player.FcOpenCraftingTableNaturallyEvent
import net.benwoodworth.fastcraft.platform.player.FcPlayer
import net.benwoodworth.fastcraft.platform.player.FcPlayerEvents
import net.benwoodworth.fastcraft.platform.server.FcPermission
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PhysicalWorkbenchPermissionTest {
    private val player = FcPlayer(Any())
    private val playerId = UUID.randomUUID()
    private val playerOperations = mockk<FcPlayer.Operations>()
    private val handlers = HandlerSet<FcOpenCraftingTableNaturallyEvent>()
    private val playerEvents = mockk<FcPlayerEvents>()
    private val preferences = mockk<FastCraftPreferences>()
    private val gui = mockk<FastCraftGui>(relaxed = true)
    private val guiFactory = mockk<FastCraftGui.Factory>()
    private var usePermission = true
    private var enabled = true

    private val permissions = Permissions(object : FcPermission.Factory {
        override fun create(name: String, description: String?, children: List<FcPermission>): FcPermission =
            object : FcPermission { override val name: String = name }
    })

    init {
        every { playerEvents.onOpenCraftingTableNaturally } returns handlers
        with(playerOperations) {
            every { player.uuid } returns playerId
            every { player.hasPermission(any()) } answers {
                secondArg<FcPermission>().name == "armamc.supercraft.craft.use" && usePermission
            }
        }
        every { preferences.getEnabledOrDefault(playerId) } answers { enabled }
        every { guiFactory.createFastCraftGui(player) } returns gui
        FastCraft(
            fcPlayerEvents = playerEvents,
            fastCraftGuiFactory = guiFactory,
            fastCraftPreferences = preferences,
            permissions = permissions,
            config = mockk<FastCraftConfig>(),
            fcPlayerOperations = playerOperations,
            api = mockk<FastCraftApi>(),
            disableHandlers = FastCraft.DisableHandlers(),
        )
    }

    private fun physicalOpen(): Boolean {
        var cancelled = false
        handlers.notifyHandlers(object : FcOpenCraftingTableNaturallyEvent {
            override val player = this@PhysicalWorkbenchPermissionTest.player
            override fun cancel() { cancelled = true }
        })
        return cancelled
    }

    @Test
    fun enabledAuthorizedPlayerReplacesPhysicalWorkbench() {
        assertTrue(physicalOpen())
        verify(exactly = 1) { gui.open() }
    }

    @Test
    fun missingUsePermissionLeavesVanillaWorkbenchUntouched() {
        usePermission = false
        assertFalse(physicalOpen())
        verify(exactly = 0) { gui.open() }
    }

    @Test
    fun disablingThenReenablingPreferenceTogglesPhysicalReplacement() {
        enabled = false
        assertFalse(physicalOpen())
        enabled = true
        assertTrue(physicalOpen())
        verify(exactly = 1) { gui.open() }
    }
}
