package net.benwoodworth.fastcraft.paper

import dagger.Component
import net.benwoodworth.fastcraft.FastCraft
import net.benwoodworth.fastcraft.FastCraftFactory
import org.bstats.bukkit.MetricsLite
import org.bukkit.plugin.java.JavaPlugin
import javax.inject.Singleton

/** The only supported server entry point, for Paper 1.21.11. */
class FastCraftPaper : JavaPlugin() {
    private var fastCraft: FastCraft? = null

    override fun onEnable() {
        check(server.name.equals("Paper", ignoreCase = true)) {
            "FastCraft requires Paper 1.21.11 (found ${server.name})"
        }
        check(server.bukkitVersion.substringBefore("-") == "1.21.11") {
            "FastCraft requires Paper 1.21.11 (found ${server.bukkitVersion})"
        }

        val factory = DaggerFastCraftPaper_Factory.builder()
            .fastCraftPaperModule(FastCraftPaperModule(this))
            .build()
        fastCraft = factory.createFastCraft()
        MetricsLite(this)
    }

    override fun onDisable() {
        fastCraft?.disable()
        fastCraft = null
    }

    override fun reloadConfig() {
        fastCraft?.reloadConfig() ?: super.reloadConfig()
    }

    @Singleton
    @Component(modules = [FastCraftPaperModule::class])
    interface Factory : FastCraftFactory
}
