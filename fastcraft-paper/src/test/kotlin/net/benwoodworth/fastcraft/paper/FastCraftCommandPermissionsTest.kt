package net.benwoodworth.fastcraft.paper

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.benwoodworth.fastcraft.FastCraftCommand
import net.benwoodworth.fastcraft.FastCraftConfig
import net.benwoodworth.fastcraft.Permissions
import net.benwoodworth.fastcraft.api.FastCraftPreferences
import net.benwoodworth.fastcraft.api.FastCraftPreferencesImpl
import net.benwoodworth.fastcraft.crafting.FastCraftGui
import net.benwoodworth.fastcraft.crafting.model.FastCraftGuiModel
import net.benwoodworth.fastcraft.crafting.presenter.FastCraftGuiPresenter
import net.benwoodworth.fastcraft.crafting.view.FastCraftGuiView
import net.benwoodworth.fastcraft.platform.command.FcCommandSource
import net.benwoodworth.fastcraft.platform.player.FcPlayer
import net.benwoodworth.fastcraft.platform.server.FcPermission
import net.benwoodworth.fastcraft.platform.server.FcPluginData
import net.benwoodworth.fastcraft.FastCraft
import net.benwoodworth.fastcraft.text.miniMessage
import net.kyori.adventure.text.Component
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import org.junit.jupiter.api.Test
import java.util.Locale
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FastCraftCommandPermissionsTest {
    // Use a real command source here: MockK cannot reliably return the
    // nullable Kotlin value class FcPlayer? from a relaxed mock.
    private class TestCommandSource(
        override var player: FcPlayer?,
        private val permittedNodes: () -> Set<String>,
    ) : FcCommandSource {
        override val isConsole: Boolean get() = player == null
        override val locale: Locale = Locale.US
        val sentMessages = mutableListOf<Component>()

        override fun hasPermission(permission: FcPermission): Boolean =
            permission.name in permittedNodes()

        override fun sendMessage(message: Component) {
            sentMessages.add(message)
        }
    }

    private val player = FcPlayer(Any())
    private val playerId = UUID.randomUUID()
    private val playerOperations = mockk<FcPlayer.Operations>(relaxed = true)
    private val preferences = mockk<FastCraftPreferences>()
    private val playerProvider = mockk<FcPlayer.Provider>()
    // Hand-written fake: MockK cannot reliably record inline FcPlayer arguments.
    private val guiPresenter = mockk<FastCraftGuiPresenter>(relaxed = true)
    private val gui = FastCraftGui(guiPresenter)
    private val guiFactory = object : FastCraftGui.Factory(
        fastCraftGuiModelFactory = mockk<FastCraftGuiModel.Factory>(),
        fastCraftGuiViewFactory = mockk<FastCraftGuiView.Factory>(),
        fastCraftGuiPresenterFactory = mockk<FastCraftGuiPresenter.Factory>(),
    ) {
        var creations = 0

        override fun createFastCraftGui(player: FcPlayer): FastCraftGui {
            assertEquals(this@FastCraftCommandPermissionsTest.player, player)
            creations++
            return gui
        }
    }
    private val fastCraftConfig = mockk<FastCraftConfig>()

    private var enabled = true
    private var hasUsePermission = true
    private var commandPermissions = setOf("armamc.supercraft.craft.use")
    private val source = TestCommandSource(player) { commandPermissions }

    private val permissions = Permissions(object : FcPermission.Factory {
        override fun create(name: String, description: String?, children: List<FcPermission>): FcPermission =
            object : FcPermission {
                override val name: String = name
            }
    })

    private val command = FastCraftCommand(
        fcPlayerProvider = playerProvider,
        fastCraftPreferences = preferences,
        fastCraftGuiFactory = guiFactory,
        permissions = permissions,
        fastCraftConfig = fastCraftConfig,
        fcPlayerOperations = playerOperations,
    )

    init {
        with(playerOperations) {
            every { player.uuid } returns playerId
            every { player.hasPermission(any()) } answers {
                secondArg<FcPermission>().name == "armamc.supercraft.craft.use" && hasUsePermission
            }
        }
        every { preferences.getEnabledOrDefault(playerId) } answers { enabled }
        every { preferences.setEnabled(any(), any()) } returns Unit
    }

    @TempDir
    lateinit var dataFolder: Path

    @Test
    fun registersOnlyTheTwoSupportedPermissionNodes() {
        val registered = mutableListOf<String>()
        Permissions(object : FcPermission.Factory {
            override fun create(name: String, description: String?, children: List<FcPermission>): FcPermission {
                registered += name
                return object : FcPermission { override val name: String = name }
            }
        })
        assertEquals(listOf("armamc.supercraft.craft.use", "armamc.supercraft.admin"), registered)
    }

    @Test
    fun preferenceVariantsAreIdempotentAndSurviveReopeningTheFile() {
        val data = mockk<FcPluginData>()
        every { data.dataFolder } returns dataFolder
        every { fastCraftConfig.playerDefaults.enabled } returns true
        val shutdown = FastCraft.DisableHandlers()
        val persisted = FastCraftPreferencesImpl(fastCraftConfig, data, shutdown)
        val service = FastCraftCommand(playerProvider, persisted, guiFactory, permissions, fastCraftConfig, playerOperations)
        commandPermissions = setOf("armamc.supercraft.craft.use")
        try {
            repeat(2) {
                assertTrue(service.fcSetEnabled(source, enabled = false))
                assertFalse(persisted.getEnabledOrDefault(playerId))
            }
            repeat(2) {
                assertTrue(service.fcSetEnabled(source, enabled = true))
                assertTrue(persisted.getEnabledOrDefault(playerId))
            }
            assertTrue(service.fcSetEnabled(source, toggle = true))
            assertFalse(persisted.getEnabledOrDefault(playerId))
            assertTrue(service.fcSetEnabled(source, toggle = true))
            assertTrue(persisted.getEnabledOrDefault(playerId))
        } finally {
            shutdown.handlers.notifyHandlers(Unit)
        }
        val reopenedShutdown = FastCraft.DisableHandlers()
        val reopened = FastCraftPreferencesImpl(fastCraftConfig, data, reopenedShutdown)
        try {
            assertEquals(true, reopened.getEnabled(playerId))
        } finally {
            reopenedShutdown.handlers.notifyHandlers(Unit)
        }
    }

    @Test
    fun rejectsConsoleAndUnauthorizedPreferenceChanges() {
        source.player = null
        assertFalse(command.fcCraft(source, "default"))
        assertFalse(command.fcSetEnabled(source, toggle = true))
        source.player = player
        commandPermissions = emptySet()
        assertFalse(command.fcSetEnabled(source, enabled = true))
        verify(exactly = 0) { preferences.setEnabled(any(), any()) }
        assertEquals(3, source.sentMessages.size)
    }

    @Test
    fun enabledAndPermittedOpensSuperCraft() {
        command.fcCraft(source, "default")

        assertEquals(1, guiFactory.creations)
        verify(exactly = 1) { guiPresenter.openGui() }
        with(playerOperations) {
            verify(exactly = 0) { player.openCraftingTable() }
        }
    }

    @Test
    fun disabledUsesVanillaGridWithoutNeedingAdditionalGridPermission() {
        enabled = false

        command.fcCraft(source, "default")

        assertEquals(0, guiFactory.creations)
        verify(exactly = 0) { guiPresenter.openGui() }
        with(playerOperations) {
            verify(exactly = 1) { player.openCraftingTable() }
        }
    }

    @Test
    fun missingUsePermissionDeniesExplicitSuperCraft() {
        hasUsePermission = false

        command.fcCraft(source, "fastcraft")

        assertEquals(0, guiFactory.creations)
        verify(exactly = 0) { guiPresenter.openGui() }
        with(playerOperations) {
            verify(exactly = 0) { player.openCraftingTable() }
        }
        assertEquals(1, source.sentMessages.size)
    }

    @Test
    fun missingUsePermissionFallsBackToVanillaForDefault() {
        hasUsePermission = false

        command.fcCraft(source, "default")

        assertEquals(0, guiFactory.creations)
        verify(exactly = 0) { guiPresenter.openGui() }
        with(playerOperations) {
            verify(exactly = 1) { player.openCraftingTable() }
        }
    }

    @Test
    fun noCommandPermissionDoesNotOpenEitherInterface() {
        commandPermissions = emptySet()

        command.fcCraft(source, "default")

        assertEquals(0, guiFactory.creations)
        verify(exactly = 0) { guiPresenter.openGui() }
        with(playerOperations) {
            verify(exactly = 0) { player.openCraftingTable() }
        }
        assertEquals(1, source.sentMessages.size)
    }

    @Test
    fun adminCannotForceSuperCraftOnPlayerWithoutUsePermission() {
        hasUsePermission = false
        commandPermissions = setOf("armamc.supercraft.admin")
        source.player = null
        every { playerProvider.getOnlinePlayers() } returns listOf(player)
        with(playerOperations) {
            every { player.username } returns "Target"
        }

        command.fcCraftAdmin(source, "fastcraft", "Target")

        assertEquals(0, guiFactory.creations)
        verify(exactly = 0) { guiPresenter.openGui() }
        assertEquals(1, source.sentMessages.size)
    }

    @Test
    fun explicitFastCraftCanOverrideDisabledPreferenceOnlyWhenAuthorized() {
        enabled = false

        command.fcCraft(source, "fastcraft")

        assertEquals(1, guiFactory.creations)
        verify(exactly = 1) { guiPresenter.openGui() }
    }

    @Test
    fun unauthorizedAdminCraftNeverLooksUpTargetPlayers() {
        commandPermissions = emptySet()

        assertFalse(command.fcCraftAdmin(source, "grid", "Target"))

        verify(exactly = 0) { playerProvider.getOnlinePlayers() }
        assertEquals(0, guiFactory.creations)
        verify(exactly = 0) { guiPresenter.openGui() }
        assertEquals(1, source.sentMessages.size)
    }

    @Test
    fun adminCraftReturnsFailureWhenTargetIsMissing() {
        commandPermissions = setOf("armamc.supercraft.admin")
        every { playerProvider.getOnlinePlayers() } returns emptyList()

        assertFalse(command.fcCraftAdmin(source, "grid", "Missing"))
        assertEquals(0, guiFactory.creations)
        verify(exactly = 0) { guiPresenter.openGui() }
        assertEquals(1, source.sentMessages.size)
    }

    @Test
    fun adminCraftReturnsFailureWhenTargetHasNoUsePermission() {
        commandPermissions = setOf("armamc.supercraft.admin")
        hasUsePermission = false
        every { playerProvider.getOnlinePlayers() } returns listOf(player)
        with(playerOperations) {
            every { player.username } returns "Target"
        }

        assertFalse(command.fcCraftAdmin(source, "fastcraft", "Target"))
        assertEquals(0, guiFactory.creations)
        verify(exactly = 0) { guiPresenter.openGui() }
    }

    @Test
    fun adminCraftReturnsSuccessWhenGridOpens() {
        commandPermissions = setOf("armamc.supercraft.admin")
        every { playerProvider.getOnlinePlayers() } returns listOf(player)
        with(playerOperations) {
            every { player.username } returns "Target"
        }

        assertTrue(command.fcCraftAdmin(source, "grid", "Target"))
        with(playerOperations) {
            verify(exactly = 1) { player.openCraftingTable() }
        }
    }

    @Test
    fun adminPreferenceReturnsFailureWithoutPermission() {
        commandPermissions = emptySet()

        assertFalse(command.fcSetEnabledAdmin(source, "Target", enabled = true))
        verify(exactly = 0) { playerProvider.getOnlinePlayers() }
        verify(exactly = 0) { preferences.setEnabled(any(), any()) }
    }

    @Test
    fun adminPreferenceReturnsSuccessWhenTargetExists() {
        commandPermissions = setOf("armamc.supercraft.admin")
        every { playerProvider.getOnlinePlayers() } returns listOf(player)
        with(playerOperations) {
            every { player.username } returns "Target"
        }

        assertTrue(command.fcSetEnabledAdmin(source, "Target", enabled = false))
        verify(exactly = 1) { preferences.setEnabled(playerId, false) }
    }

    @Test
    fun reloadFailureSendsErrorInsteadOfSuccess() {
        commandPermissions = setOf("armamc.supercraft.admin")
        every { fastCraftConfig.load() } returns false
        val failureTemplate = net.benwoodworth.fastcraft.Strings.commandReloadFailed(Locale.US)
        val failureText = miniMessage(failureTemplate)

        assertFalse(command.fcReload(source))

        verify(exactly = 1) { fastCraftConfig.load() }
        assertEquals(listOf(failureText), source.sentMessages)
    }

    @Test
    fun successfulReloadAnnouncesCompletion() {
        commandPermissions = setOf("armamc.supercraft.admin")
        every { fastCraftConfig.load() } returns true

        assertTrue(command.fcReload(source))

        verify(exactly = 1) { fastCraftConfig.load() }
        assertEquals(1, source.sentMessages.size)
    }

    @Test
    fun unauthorizedReloadNeverLoadsConfiguration() {
        commandPermissions = emptySet()

        assertFalse(command.fcReload(source))

        verify(exactly = 0) { fastCraftConfig.load() }
        assertEquals(1, source.sentMessages.size)
    }

}
