package net.benwoodworth.fastcraft.paper

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.benwoodworth.fastcraft.FastCraftCommand
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
import kotlin.test.assertEquals

class FastCraftCommandPermissionsTest {
    private val player = FcPlayer(Any())
    private val playerId = UUID.randomUUID()
    private val playerOperations = mockk<FcPlayer.Operations>(relaxed = true)
    private val source = mockk<FcCommandSource>(relaxed = true)
    private val preferences = mockk<FastCraftPreferences>()
    private val playerProvider = mockk<FcPlayer.Provider>()
    private val gui = mockk<FastCraftGui>(relaxed = true)
    private val guiFactory = mockk<FastCraftGui.Factory>()

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
        fcCommandRegistry = mockk(),
        fcTextFactory = mockk<FcText.Factory>(relaxed = true),
        fcPlayerProvider = playerProvider,
        fastCraftPreferences = preferences,
        fastCraftGuiFactory = guiFactory,
        permissions = permissions,
        fastCraftConfig = mockk(),
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
    fun rejectsLegacyPersonalCraftCommandsWithoutTarget() {
        command.process(source, "craft")
        command.process(source, "craft fastcraft")
        command.process(source, "craft grid")
        command.process(source, "craft default")

        verify(exactly = 0) { gui.open() }
        with(playerOperations) {
            verify(exactly = 0) { player.openCraftingTable() }
        }
        verify(exactly = 4) { source.sendMessage(any()) }
    }

    @Test
    fun rejectsLegacyPersonalPreferenceCommandsWithoutTarget() {
        command.process(source, "set enabled true")
        command.process(source, "set enabled false")
        command.process(source, "set enabled toggle")

        verify(exactly = 0) { preferences.setEnabled(any(), any()) }
        verify(exactly = 3) { source.sendMessage(any()) }
    }

    @Test
    fun administratorCanChangeOwnPreferencesOnlyViaTargetedAdminCommand() {
        commandPermissions = setOf("fastcraft.admin.command.set.enabled")
        every { playerProvider.getOnlinePlayers() } returns listOf(player)
        with(playerOperations) {
            every { player.username } returns "Target"
        }

        command.process(source, "set enabled false Target")

        verify(exactly = 1) { preferences.setEnabled(playerId, false) }
    }

    @Test
    fun administratorCanOpenOwnSuperCraftViaTargetedAdminCommand() {
        commandPermissions = setOf("fastcraft.admin.command.craft")
        every { playerProvider.getOnlinePlayers() } returns listOf(player)
        with(playerOperations) {
            every { player.username } returns "Target"
        }

        command.process(source, "craft fastcraft Target")

        verify(exactly = 1) { gui.open() }
    }

    @Test
    fun legacySuggestionsOnlyExposeAuthorizedAdminCommands() {
        commandPermissions = emptySet()
        assertEquals(emptyList(), command.getSuggestions(source, ""))
        assertEquals(emptyList(), command.getSuggestions(source, "craft "))
        assertEquals(emptyList(), command.getSuggestions(source, "set "))

        commandPermissions = setOf("fastcraft.admin.command.reload")
        assertEquals(listOf("reload"), command.getSuggestions(source, ""))

        commandPermissions = setOf("fastcraft.admin.command.set.enabled", "fastcraft.admin.command.craft")
        assertEquals(listOf("set", "craft"), command.getSuggestions(source, ""))
        assertEquals(listOf("enabled"), command.getSuggestions(source, "set "))
        assertEquals(listOf("fastcraft", "grid", "default"), command.getSuggestions(source, "craft "))
    }

}
