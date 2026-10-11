package net.benwoodworth.fastcraft.bukkit.server

import org.bukkit.Server
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcServer_Bukkit_Paper @Inject constructor(
    val server: Server,
) : FcServer_Bukkit {
    // Asset downloads need the Minecraft data version, never the server brand.
    // Read the official API value instead of parsing Paper/Leaf version banners.
    override val minecraftVersion: String
        get() = server.minecraftVersion

    override fun executeCommand(command: String) {
        server.dispatchCommand(server.consoleSender, command)
    }
}
