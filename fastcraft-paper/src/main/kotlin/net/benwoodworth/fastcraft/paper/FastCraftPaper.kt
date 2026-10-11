package net.benwoodworth.fastcraft.paper

import net.benwoodworth.fastcraft.bukkit.BukkitFastCraft

/**
 * Only Paper 1.21.11 is supported. The existing FastCraft engine and
 * dependency injection are reused while its legacy adapters are consolidated.
 */
class FastCraftPaper : BukkitFastCraft() {
    override fun onEnable() {
        check(server.name.equals("Paper", ignoreCase = true)) {
            "FastCraft requires Paper 1.21.11 (found ${server.name})"
        }
        check(server.bukkitVersion.substringBefore("-") == "1.21.11") {
            "FastCraft requires Paper 1.21.11 (found ${server.bukkitVersion})"
        }
        super.onEnable()
    }
}
