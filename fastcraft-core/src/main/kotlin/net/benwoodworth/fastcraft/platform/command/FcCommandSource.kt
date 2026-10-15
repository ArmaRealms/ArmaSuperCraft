package net.benwoodworth.fastcraft.platform.command

import net.benwoodworth.fastcraft.platform.player.FcPlayer
import net.benwoodworth.fastcraft.platform.server.FcPermission
import net.kyori.adventure.text.Component
import java.util.*

interface FcCommandSource {
    val player: FcPlayer?
    val isConsole: Boolean
    val locale: Locale

    fun hasPermission(permission: FcPermission): Boolean

    fun sendMessage(message: Component)
}
