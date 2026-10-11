package net.benwoodworth.fastcraft.bukkit

import dagger.Component
import net.benwoodworth.fastcraft.FastCraft
import net.benwoodworth.fastcraft.FastCraftFactory
import org.bstats.bukkit.MetricsLite
import org.bukkit.plugin.java.JavaPlugin
import javax.inject.Singleton

@Suppress("unused")
open class BukkitFastCraft : JavaPlugin() {
    private var fastCraft: FastCraft? = null

    open override fun onEnable() {
        val factory = DaggerBukkitFastCraft_Factory.builder()
            .bukkitFastCraftModule(BukkitFastCraftModule(this))
            .build()
        fastCraft = factory.createFastCraft()

        MetricsLite(this)
    }

    override fun onDisable() {
        // Bukkit may call onDisable() after onEnable() fails before creating FastCraft.
        fastCraft?.disable()
        fastCraft = null
    }

    override fun reloadConfig() {
        fastCraft!!.reloadConfig()
    }

    @Singleton
    @Component(modules = [BukkitFastCraftModule::class])
    interface Factory : FastCraftFactory
}
