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
        val factory = DaggerFastCraftPaper_Factory.builder()
            .fastCraftPaperModule(FastCraftPaperModule(this))
            .build()
        fastCraft = factory.createFastCraft()
        factory.createCraftCommand().register(this)
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
    interface Factory : FastCraftFactory {
        fun createCraftCommand(): CraftCommand
    }
}
