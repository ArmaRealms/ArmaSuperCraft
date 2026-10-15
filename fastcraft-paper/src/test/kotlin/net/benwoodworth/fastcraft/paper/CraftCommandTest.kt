package net.benwoodworth.fastcraft.paper

import com.mojang.brigadier.context.CommandContext
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.verify
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.benwoodworth.fastcraft.FastCraftCommand
import net.benwoodworth.fastcraft.bukkit.command.FcCommandSource_Bukkit
import net.benwoodworth.fastcraft.platform.command.FcCommandSource
import org.bukkit.entity.Player
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CraftCommandTest {
    private val core = mockk<FastCraftCommand>()
    private val sources = mockk<FcCommandSource_Bukkit.Factory>()
    private val command = CraftCommand(core, sources)

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
        every { core.fcCraft(source, "default") } just Runs

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
        every { core.fcCraft(source, "default") } just Runs

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
        every { core.fcSetEnabled(source, false, false) } just Runs

        val off = checkNotNull(command.createTree().getChild("off")).command
        assertEquals(1, off.run(context))
        verify(exactly = 1) { core.fcSetEnabled(source, false, false) }
    }
}
