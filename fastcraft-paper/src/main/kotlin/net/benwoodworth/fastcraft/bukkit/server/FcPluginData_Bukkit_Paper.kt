package net.benwoodworth.fastcraft.bukkit.server

import net.benwoodworth.fastcraft.platform.server.FcPluginData
import org.bukkit.plugin.Plugin
import java.nio.file.Path
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcPluginData_Bukkit_Paper @Inject constructor(
    plugin: Plugin,
) : FcPluginData {
    override val dataFolder: Path = plugin.dataFolder.toPath()

    override val configFile: Path = dataFolder.resolve("config.yml")
}
