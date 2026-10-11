package net.benwoodworth.fastcraft.bukkit.command

import net.benwoodworth.fastcraft.platform.command.FcCommand
import net.benwoodworth.fastcraft.platform.server.FcLogger
import org.bukkit.Server
import org.bukkit.plugin.Plugin
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcCommandRegistry_Bukkit_Paper @Inject constructor(
    private val plugin: Plugin,
    private val server: Server,
    private val fcLogger: FcLogger,
    private val fcCommandAdapterFactory: FcCommandAdapter_Bukkit.Factory,
) : FcCommandRegistry_Bukkit {
    override fun register(command: FcCommand, name: String, vararg aliases: String) {
        val pluginCommand = server.getPluginCommand("${plugin.name}:$name")
            ?: run {
                fcLogger.error("Command not registered in plugin.yml: $name")
                return
            }

        val wrapper = fcCommandAdapterFactory.create(command)

        // Bukkit Command setters return Command, so Kotlin cannot always expose mutable properties.
        pluginCommand.setAliases(aliases.toList())
        pluginCommand.setExecutor(wrapper)
        pluginCommand.setTabCompleter(wrapper)
        pluginCommand.setDescription(command.description)
        pluginCommand.setUsage(command.usage)
    }
}
