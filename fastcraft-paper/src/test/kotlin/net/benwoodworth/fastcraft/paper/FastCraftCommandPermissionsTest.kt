package net.benwoodworth.fastcraft.paper

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.benwoodworth.fastcraft.FastCraftCommand
import net.benwoodworth.fastcraft.FastCraftConfig
import net.benwoodworth.fastcraft.Permissions
import net.benwoodworth.fastcraft.api.FastCraftPreferences
import net.benwoodworth.fastcraft.crafting.FastCraftGui
import net.benwoodworth.fastcraft.platform.command.FcCommandSource
import net.benwoodworth.fastcraft.platform.player.FcPlayer
import net.benwoodworth.fastcraft.platform.server.FcPermission
import net.benwoodworth.fastcraft.platform.text.FcText
import org.junit.jupiter.api.Test
import java.util.Locale
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FastCraftCommandPermissionsTest {
    private val player = FcPlayer(Any())
    private val playerId = UUID.randomUUID()
    private val playerOperations = mockk<FcPlayer.Operations>(relaxed = true)
    private val source = mockk<FcCommandSource>(relaxed = true)
    private val preferences = mockk<FastCraftPreferences>()
    private val playerProvider = mockk<FcPlayer.Provider>()
    private val gui = mockk<FastCraftGui>(relaxed = true)
    private val guiFactory = mockk<FastCraftGui.Factory>()
    private val fastCraftConfig = mockk<FastCraftConfig>()
    private val textFactory = mockk<FcText.Factory>(relaxed = true)

    private var enabled = true
    private var hasUsePermission = true
    private var commandPermissions = setOf("fastcraft.command.craft.fastcraft")

    private val permissions = Permissions(object : FcPermission.Factory {
        override fun create(name: String, description: String?, children: List<FcPermission>): FcPermission =
            object : FcPermission {
                override val name: String = name
            }
    })

    private val command = FastCraftCommand(
        fcTextFactory = textFactory,
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
                firstArg<FcPermission>().name == "fastcraft.use" && hasUsePermission
            }
        }
        every { source.player } returns player
        every { source.locale } returns Locale.US
        every { source.hasPermission(any()) } answers {
            firstArg<FcPermission>().name in commandPermissions
        }
        every { preferences.getEnabledOrDefault(playerId) } answers { enabled }
        every { preferences.setEnabled(any(), any()) } returns Unit
        every { guiFactory.createFastCraftGui(player) } returns gui
    }

    @Test
    fun enabledAndPermittedOpensSuperCraft() {
        command.fcCraft(source, "default")

        verify(exactly = 1) { gui.open() }
        with(playerOperations) {
            verify(exactly = 0) { player.openCraftingTable() }
        }
    }

    @Test
    fun disabledUsesVanillaGridWithoutNeedingAdditionalGridPermission() {
        enabled = false

        command.fcCraft(source, "default")

        verify(exactly = 0) { gui.open() }
        with(playerOperations) {
            verify(exactly = 1) { player.openCraftingTable() }
        }
    }

    @Test
    fun missingUsePermissionDeniesExplicitSuperCraft() {
        hasUsePermission = false

        command.fcCraft(source, "fastcraft")

        verify(exactly = 0) { gui.open() }
        with(playerOperations) {
            verify(exactly = 0) { player.openCraftingTable() }
        }
        verify(exactly = 1) { source.sendMessage(any()) }
    }

    @Test
    fun missingUsePermissionFallsBackToVanillaForDefault() {
        hasUsePermission = false

        command.fcCraft(source, "default")

        verify(exactly = 0) { gui.open() }
        with(playerOperations) {
            verify(exactly = 1) { player.openCraftingTable() }
        }
    }

    @Test
    fun noCommandPermissionDoesNotOpenEitherInterface() {
        commandPermissions = emptySet()

        command.fcCraft(source, "default")

        verify(exactly = 0) { gui.open() }
        with(playerOperations) {
            verify(exactly = 0) { player.openCraftingTable() }
        }
        verify(exactly = 1) { source.sendMessage(any()) }
    }

    @Test
    fun adminCannotForceSuperCraftOnPlayerWithoutUsePermission() {
        hasUsePermission = false
        commandPermissions = setOf("fastcraft.admin.command.craft")
        every { source.player } returns null
        every { playerProvider.getOnlinePlayers() } returns listOf(player)
        with(playerOperations) {
            every { player.username } returns "Target"
        }

        command.fcCraftAdmin(source, "fastcraft", "Target")

        verify(exactly = 0) { gui.open() }
        verify(exactly = 1) { source.sendMessage(any()) }
    }

    @Test
    fun explicitFastCraftCanOverrideDisabledPreferenceOnlyWhenAuthorized() {
        enabled = false

        command.fcCraft(source, "fastcraft")

        verify(exactly = 1) { gui.open() }
    }

    @Test
    fun adminCraftReturnsFailureWhenTargetIsMissing() {
        commandPermissions = setOf("fastcraft.admin.command.craft")
        every { playerProvider.getOnlinePlayers() } returns emptyList()

        assertFalse(command.fcCraftAdmin(source, "grid", "Missing"))
        verify(exactly = 0) { gui.open() }
        verify(exactly = 1) { source.sendMessage(any()) }
    }

    @Test
    fun adminCraftReturnsFailureWhenTargetHasNoUsePermission() {
        commandPermissions = setOf("fastcraft.admin.command.craft")
        hasUsePermission = false
        every { playerProvider.getOnlinePlayers() } returns listOf(player)
        with(playerOperations) {
            every { player.username } returns "Target"
        }

        assertFalse(command.fcCraftAdmin(source, "fastcraft", "Target"))
        verify(exactly = 0) { gui.open() }
    }

    @Test
    fun adminCraftReturnsSuccessWhenGridOpens() {
        commandPermissions = setOf("fastcraft.admin.command.craft")
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
        verify(exactly = 0) { preferences.setEnabled(any(), any()) }
    }

    @Test
    fun adminPreferenceReturnsSuccessWhenTargetExists() {
        commandPermissions = setOf("fastcraft.admin.command.set.enabled")
        every { playerProvider.getOnlinePlayers() } returns listOf(player)
        with(playerOperations) {
            every { player.username } returns "Target"
        }

        assertTrue(command.fcSetEnabledAdmin(source, "Target", enabled = false))
        verify(exactly = 1) { preferences.setEnabled(playerId, false) }
    }


    @Test
    fun reloadFailureSendsErrorInsteadOfSuccess() {
        commandPermissions = setOf("fastcraft.admin.command.reload")
        every { fastCraftConfig.load() } returns false
        val failureText = mockk<FcText>()
        val failureTemplate = net.benwoodworth.fastcraft.Strings.commandReloadFailed(Locale.US)
        every { textFactory.createMiniMessage(failureTemplate) } returns failureText

        assertFalse(command.fcReload(source))

        verify(exactly = 1) { fastCraftConfig.load() }
        verify(exactly = 1) { textFactory.createMiniMessage(failureTemplate) }
        verify(exactly = 1) { source.sendMessage(failureText) }
    }

    @Test
    fun successfulReloadAnnouncesCompletion() {
        commandPermissions = setOf("fastcraft.admin.command.reload")
        every { fastCraftConfig.load() } returns true

        assertTrue(command.fcReload(source))

        verify(exactly = 1) { fastCraftConfig.load() }
        verify(exactly = 1) { source.sendMessage(any()) }
    }

    @Test
    fun unauthorizedReloadNeverLoadsConfiguration() {
        commandPermissions = emptySet()

        assertFalse(command.fcReload(source))

        verify(exactly = 0) { fastCraftConfig.load() }
        verify(exactly = 1) { source.sendMessage(any()) }
    }

}
