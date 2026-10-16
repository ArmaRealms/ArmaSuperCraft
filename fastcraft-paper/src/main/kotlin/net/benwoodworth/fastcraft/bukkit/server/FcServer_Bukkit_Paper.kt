package net.benwoodworth.fastcraft.bukkit.server

import net.benwoodworth.fastcraft.platform.server.FcServer
import org.bukkit.Server
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcServer_Bukkit_Paper @Inject constructor(
    private val server: Server,
) : FcServer {
    override fun executeCommand(command: String) {
        server.dispatchCommand(server.consoleSender, command)
    }
}
