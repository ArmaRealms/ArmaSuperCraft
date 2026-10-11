package net.benwoodworth.fastcraft.bukkit.command

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.benwoodworth.fastcraft.platform.command.FcCommand
import net.benwoodworth.fastcraft.platform.server.FcLogger
import org.bukkit.Server
import org.bukkit.command.PluginCommand
import org.bukkit.plugin.Plugin
import org.junit.jupiter.api.Test

class FcCommandRegistryPaperTest {
    private val plugin = mockk<Plugin>()
    private val server = mockk<Server>()
    private val logger = mockk<FcLogger>(relaxed = true)
    private val adapterFactory = mockk<FcCommandAdapter_Bukkit.Factory>()
    private val registry = FcCommandRegistry_Bukkit_Paper(plugin, server, logger, adapterFactory)

    @Test
    fun registersDeclaredCommand() {
        val command = mockk<FcCommand>()
        val adapter = mockk<FcCommandAdapter_Bukkit>()
        val pluginCommand = mockk<PluginCommand>(relaxed = true)

        every { plugin.name } returns "FastCraft"
        every { server.getPluginCommand("FastCraft:fastcraft") } returns pluginCommand
        every { adapterFactory.create(command) } returns adapter
        every { command.description } returns "FastCraft crafting menu"
        every { command.usage } returns "/fastcraft"

        registry.register(command, "fastcraft", "fc")

        verify(exactly = 1) { pluginCommand.setAliases(listOf("fc")) }
        verify(exactly = 1) { pluginCommand.setExecutor(adapter) }
        verify(exactly = 1) { pluginCommand.setTabCompleter(adapter) }
        verify(exactly = 1) { pluginCommand.setDescription("FastCraft crafting menu") }
        verify(exactly = 1) { pluginCommand.setUsage("/fastcraft") }
    }

    @Test
    fun logsMissingPluginCommandAndSkipsRegistration() {
        val command = mockk<FcCommand>()
        every { plugin.name } returns "FastCraft"
        every { server.getPluginCommand("FastCraft:missing") } returns null

        registry.register(command, "missing")

        verify(exactly = 1) { logger.error("Command not registered in plugin.yml: missing") }
        verify(exactly = 0) { adapterFactory.create(any()) }
    }
}
