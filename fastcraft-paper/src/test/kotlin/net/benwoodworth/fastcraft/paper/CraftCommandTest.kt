package net.benwoodworth.fastcraft.paper

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.benwoodworth.fastcraft.FastCraftCommand
import net.benwoodworth.fastcraft.bukkit.command.FcCommandSource_Bukkit
import net.benwoodworth.fastcraft.platform.command.FcCommandSource
import org.bukkit.entity.Player
import org.bukkit.Server
import org.bukkit.command.CommandSender
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CraftCommandTest {
    private val core = mockk<FastCraftCommand>()
    private val sources = mockk<FcCommandSource_Bukkit.Factory>()
    private val server = mockk<Server>()
    private val command = CraftCommand(core, sources, server)

    @Test
    fun definesNativeLiteralSubcommands() {
        val root = command.createTree()
        assertEquals("craft", root.name)
        assertEquals(setOf("alternar", "on", "off"), root.children.map { it.name }.toSet())
    }

    @Test
    fun opensPreferredCraftingInterfaceThroughExistingPermissionAwareService() {
        val sender = mockk<Player>()
        val source = mockk<FcCommandSource>()
        val stack = mockk<CommandSourceStack>()
        val context = mockk<CommandContext<CommandSourceStack>>()
        every { context.source } returns stack
        every { stack.sender } returns sender
        every { sources.create(sender) } returns source
        every { sender.hasPermission("fastcraft.command.craft.fastcraft") } returns true
        every { core.fcCraft(source, "default") } returns true

        assertEquals(1, command.createTree().command.run(context))
        verify(exactly = 1) { core.fcCraft(source, "default") }
    }

    @Test
    fun allowsVanillaCraftingWhenOnlyGridPermissionIsGranted() {
        val sender = mockk<Player>()
        val source = mockk<FcCommandSource>()
        val stack = mockk<CommandSourceStack>()
        val context = mockk<CommandContext<CommandSourceStack>>()
        every { context.source } returns stack
        every { stack.sender } returns sender
        every { sources.create(sender) } returns source
        every { sender.hasPermission("fastcraft.command.craft.fastcraft") } returns false
        every { sender.hasPermission("fastcraft.command.craft.grid") } returns true
        every { core.fcCraft(source, "default") } returns true

        assertEquals(1, command.createTree().command.run(context))
        verify(exactly = 1) { core.fcCraft(source, "default") }
    }

    @Test
    fun offIsIdempotentAndUsesTheExistingPreferencesService() {
        val sender = mockk<Player>()
        val source = mockk<FcCommandSource>()
        val stack = mockk<CommandSourceStack>()
        val context = mockk<CommandContext<CommandSourceStack>>()
        every { context.source } returns stack
        every { stack.sender } returns sender
        every { sources.create(sender) } returns source
        every { sender.hasPermission("fastcraft.command.set.enabled") } returns true
        every { core.fcSetEnabled(source, false, false) } returns true

        val off = checkNotNull(command.createTree().getChild("off")).command
        assertEquals(1, off.run(context))
        verify(exactly = 1) { core.fcSetEnabled(source, false, false) }
    }

    @Test
    fun definesAdministrativeBrigadierTreeWithRequiredTarget() {
        val root = command.createAdminTree()

        assertEquals("fastcraft", root.name)
        assertEquals(setOf("set", "craft", "reload"), root.children.map { it.name }.toSet())
        assertEquals(
            setOf("fastcraft", "grid", "default"),
            root.getChild("craft").children.map { it.name }.toSet(),
        )
        assertEquals(
            setOf("true", "false", "toggle"),
            root.getChild("set").getChild("enabled").children.map { it.name }.toSet(),
        )
        assertNull(root.getChild("craft").getChild("fastcraft").command)
        assertNotNull(root.getChild("craft").getChild("fastcraft").getChild("player").command)
        assertNull(root.getChild("set").getChild("enabled").getChild("true").command)
        assertNotNull(root.getChild("set").getChild("enabled").getChild("true").getChild("player").command)
    }

    @Test
    fun hidesAdminSubcommandsWithoutMatchingPermissions() {
        val sender = mockk<CommandSender>()
        val stack = mockk<CommandSourceStack>()
        every { stack.sender } returns sender
        every { sender.hasPermission(any<String>()) } returns false

        val root = command.createAdminTree()
        assertFalse(root.requirement.test(stack))

        every { sender.hasPermission("fastcraft.admin.command.reload") } returns true
        assertTrue(root.requirement.test(stack))
        assertTrue(root.getChild("reload").requirement.test(stack))
        assertFalse(root.getChild("set").requirement.test(stack))
        assertFalse(root.getChild("craft").requirement.test(stack))
    }

    @Test
    fun executesAdminCraftWithParsedPlayerArgument() {
        val sender = mockk<CommandSender>()
        val source = mockk<FcCommandSource>()
        val stack = mockk<CommandSourceStack>()
        val context = mockk<CommandContext<CommandSourceStack>>()
        every { context.source } returns stack
        every { context.getArgument("player", String::class.java) } returns "Target"
        every { stack.sender } returns sender
        every { sources.create(sender) } returns source
        every { core.fcCraftAdmin(source, "fastcraft", "Target") } returns true

        val node = command.createAdminTree().getChild("craft").getChild("fastcraft").getChild("player")
        assertEquals(1, checkNotNull(node.command).run(context))
        verify(exactly = 1) { core.fcCraftAdmin(source, "fastcraft", "Target") }
    }

    @Test
    fun executesAdminPreferenceWithParsedPlayerArgument() {
        val sender = mockk<CommandSender>()
        val source = mockk<FcCommandSource>()
        val stack = mockk<CommandSourceStack>()
        val context = mockk<CommandContext<CommandSourceStack>>()
        every { context.source } returns stack
        every { context.getArgument("player", String::class.java) } returns "Target"
        every { stack.sender } returns sender
        every { sources.create(sender) } returns source
        every { core.fcSetEnabledAdmin(source, "Target", false, false) } returns true

        val node = command.createAdminTree().getChild("set").getChild("enabled").getChild("false").getChild("player")
        assertEquals(1, checkNotNull(node.command).run(context))
        verify(exactly = 1) { core.fcSetEnabledAdmin(source, "Target", false, false) }
    }

    @Test
    fun suggestsOnlinePlayersForAdminTargets() {
        val target = mockk<Player>()
        every { target.name } returns "Target"
        every { server.onlinePlayers } returns mutableListOf(target)
        val context = mockk<CommandContext<CommandSourceStack>>()

        val node = command.createAdminTree().getChild("craft").getChild("grid").getChild("player")
        val suggestions = node.listSuggestions(context, SuggestionsBuilder("Ta", 0)).get()
        assertEquals(listOf("Target"), suggestions.list.map { it.text })
    }

    @Test
    fun failedAdminCraftReportsBrigadierFailure() {
        val sender = mockk<CommandSender>()
        val source = mockk<FcCommandSource>()
        val stack = mockk<CommandSourceStack>()
        val context = mockk<CommandContext<CommandSourceStack>>()
        every { context.source } returns stack
        every { context.getArgument("player", String::class.java) } returns "Missing"
        every { stack.sender } returns sender
        every { sources.create(sender) } returns source
        every { core.fcCraftAdmin(source, "grid", "Missing") } returns false

        val node = command.createAdminTree().getChild("craft").getChild("grid").getChild("player")
        assertEquals(0, checkNotNull(node.command).run(context))
        verify(exactly = 1) { core.fcCraftAdmin(source, "grid", "Missing") }
    }

    @Test
    fun failedAdminPreferenceReportsBrigadierFailure() {
        val sender = mockk<CommandSender>()
        val source = mockk<FcCommandSource>()
        val stack = mockk<CommandSourceStack>()
        val context = mockk<CommandContext<CommandSourceStack>>()
        every { context.source } returns stack
        every { context.getArgument("player", String::class.java) } returns "Missing"
        every { stack.sender } returns sender
        every { sources.create(sender) } returns source
        every { core.fcSetEnabledAdmin(source, "Missing", true, false) } returns false

        val node = command.createAdminTree().getChild("set").getChild("enabled").getChild("true").getChild("player")
        assertEquals(0, checkNotNull(node.command).run(context))
        verify(exactly = 1) { core.fcSetEnabledAdmin(source, "Missing", true, false) }
    }

    @Test
    fun reloadReportsFailureOnPermissionDenial() {
        val sender = mockk<CommandSender>()
        val source = mockk<FcCommandSource>()
        val stack = mockk<CommandSourceStack>()
        val context = mockk<CommandContext<CommandSourceStack>>()
        every { context.source } returns stack
        every { stack.sender } returns sender
        every { sources.create(sender) } returns source
        every { core.fcReload(source) } returns false

        assertEquals(0, checkNotNull(command.createAdminTree().getChild("reload").command).run(context))
    }

    @Test
    fun personalCraftReportsFailureFromService() {
        val sender = mockk<Player>()
        val source = mockk<FcCommandSource>()
        val stack = mockk<CommandSourceStack>()
        val context = mockk<CommandContext<CommandSourceStack>>()
        every { context.source } returns stack
        every { stack.sender } returns sender
        every { sources.create(sender) } returns source
        every { core.fcCraft(source, "default") } returns false

        assertEquals(0, checkNotNull(command.createTree().command).run(context))
    }


}
