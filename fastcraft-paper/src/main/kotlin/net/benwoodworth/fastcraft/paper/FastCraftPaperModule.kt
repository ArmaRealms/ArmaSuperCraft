package net.benwoodworth.fastcraft.paper

import dagger.Module
import dagger.Provides
import net.benwoodworth.fastcraft.api.FastCraftApi
import net.benwoodworth.fastcraft.api.FastCraftPreferences
import net.benwoodworth.fastcraft.bukkit.api.BukkitFastCraftApiImpl
import net.benwoodworth.fastcraft.bukkit.api.BukkitFastCraftPreferences
import net.benwoodworth.fastcraft.bukkit.api.BukkitFastCraftPreferencesImpl
import net.benwoodworth.fastcraft.bukkit.command.*
import net.benwoodworth.fastcraft.bukkit.config.FcConfig_Bukkit_Paper
import net.benwoodworth.fastcraft.bukkit.gui.*
import net.benwoodworth.fastcraft.bukkit.player.*
import net.benwoodworth.fastcraft.bukkit.recipe.*
import net.benwoodworth.fastcraft.bukkit.server.*
import net.benwoodworth.fastcraft.bukkit.text.*
import net.benwoodworth.fastcraft.bukkit.world.*
import net.benwoodworth.fastcraft.platform.config.FcConfig
import net.benwoodworth.fastcraft.platform.gui.FcGui
import net.benwoodworth.fastcraft.platform.player.FcPlayer
import net.benwoodworth.fastcraft.platform.player.FcPlayerEvents
import net.benwoodworth.fastcraft.platform.player.FcSound
import net.benwoodworth.fastcraft.platform.recipe.FcRecipeProvider
import net.benwoodworth.fastcraft.platform.server.*
import net.benwoodworth.fastcraft.platform.text.FcText
import net.benwoodworth.fastcraft.platform.text.FcTextColor
import net.benwoodworth.fastcraft.platform.text.FcTextConverter
import net.benwoodworth.fastcraft.platform.world.FcItem
import net.benwoodworth.fastcraft.platform.world.FcItemOrderComparator
import net.benwoodworth.fastcraft.platform.world.FcItemStack
import org.bukkit.Server
import org.bukkit.inventory.ItemFactory
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginManager
import org.bukkit.scheduler.BukkitScheduler
import java.util.logging.Logger
import javax.inject.Provider

@Suppress("unused")
@Module
class FastCraftPaperModule(
    private val plugin: FastCraftPaper,
) {

    @Provides
    fun providePlugin(): Plugin {
        return plugin
    }

    @Provides
    fun provideServer(plugin: Plugin): Server {
        return plugin.server
    }

    @Provides
    fun provideFastCraftApi(instance: BukkitFastCraftApiImpl): FastCraftApi {
        return instance
    }

    @Provides
    fun provideBukkitFastCraftPreferences(instance: BukkitFastCraftPreferencesImpl): BukkitFastCraftPreferences {
        return instance
    }

    @Provides
    fun provideFastCraftPreferences(instance: BukkitFastCraftPreferences): FastCraftPreferences {
        return instance
    }

    @Provides
    fun provideItemFactory(server: Server): ItemFactory {
        return server.itemFactory
    }

    @Provides
    fun providePluginManager(server: Server): PluginManager {
        return server.pluginManager
    }

    @Provides
    fun provideBukkitScheduler(server: Server): BukkitScheduler {
        return server.scheduler
    }

    @Provides
    fun provideLogger(): Logger {
        return plugin.logger
    }

    @Provides
    fun provideFcLogger(instance: FcLogger_Bukkit_Paper): FcLogger {
        return instance
    }

    @Provides
    fun provideFcPluginData(instance: FcPluginData_Bukkit_Paper): FcPluginData {
        return instance
    }

    @Provides
    fun provideFcConfigFactory(instance: FcConfig_Bukkit_Paper.Factory): FcConfig.Factory {
        return instance
    }

    @Provides
    fun provideFcGuiFactory(instance: FcGui_Bukkit_Paper.Factory): FcGui.Factory {
        return instance
    }

    @Provides
    fun provideFcGuiButtonFactory(
        instance: Provider<FcGuiButton_Bukkit_Paper.Factory>,
    ): FcGuiButton_Bukkit.Factory = instance.get()

    @Provides
    fun provideFcGuiLayoutFactory(instance: FcGuiLayout_Bukkit_Paper.Factory): FcGuiLayout_Bukkit.Factory {
        return instance
    }

    @Provides
    fun provideFcItemStackFactory(
        instance: Provider<FcItemStack_Bukkit_Paper.Factory>,
    ): FcItemStack.Factory = instance.get()

    @Provides
    fun provideFcItemFactory(
        instance: Provider<FcItem_Bukkit_Paper.Factory>,
    ): FcItem.Factory = instance.get()

    @Provides
    fun provideFcMaterialComparator(
        instance: Provider<FcItemOrderComparator_Bukkit_Paper>,
    ): FcItemOrderComparator = instance.get()

    @Provides
    fun provideFcPlayerProvider(instance: FcPlayer_Bukkit_Paper.Provider): FcPlayer.Provider {
        return instance
    }

    @Provides
    fun provideFcCraftingRecipePreparedFactory(
        instance: Provider<FcCraftingRecipePrepared_Bukkit_Paper.Factory>,
    ): FcCraftingRecipePrepared_Bukkit.Factory = instance.get()

    @Provides
    fun provideFcRecipeProvider(
        instance: Provider<FcRecipeProvider_Bukkit_Paper>,
    ): FcRecipeProvider = instance.get()

    @Provides
    fun provideCraftingInventoryViewFactory(
        instance: Provider<CraftingInventoryViewFactory_Paper>,
    ): CraftingInventoryViewFactory = instance.get()

    @Provides
    fun provideFcCraftingRecipeFactory(
        instance: Provider<FcCraftingRecipe_Bukkit_Paper.Factory>,
    ): FcCraftingRecipe_Bukkit.Factory = instance.get()

    @Provides
    fun provideFcPlayerEvents(
        instance: Provider<FcPlayerEvents_Bukkit_Paper>,
    ): FcPlayerEvents = instance.get()

    @Provides
    fun provideFcTaskFactory(instance: FcTask_Bukkit_Paper.Factory): FcTask.Factory {
        return instance
    }

    @Provides
    fun provideFcTextFactory(instance: FcText_Bukkit_Paper.Factory): FcText.Factory {
        return instance
    }

    @Provides
    fun provideFcTextColorFactory(instance: FcTextColor_Bukkit_Paper.Factory): FcTextColor.Factory {
        return instance
    }

    @Provides
    fun provideFcTextConverter(instance: FcTextConverter_Bukkit_Paper): FcTextConverter {
        return instance
    }

    @Provides
    fun provideCommandSourceFactory(
        instance: FcCommandSource_Bukkit_Paper.Factory,
    ): FcCommandSource_Bukkit.Factory {
        return instance
    }

    @Provides
    fun provideSoundFactory(
        instance: Provider<FcSoundFactory_Paper.Factory>,
    ): FcSound.Factory = instance.get()

    @Provides
    fun provideFcServer(
        instance: FcServer_Bukkit_Paper,
    ): FcServer {
        return instance
    }

    @Provides
    fun provideFcPermissionFactory(
        instance: FcPermission_Bukkit_Paper.Factory,
    ): FcPermission.Factory {
        return instance
    }

    @Provides
    fun provideFcSoundOperations(
        instance: Provider<FcSoundOperations_Paper.Operations>,
    ): FcSound.Operations {
        return instance.get()
    }

    @Provides
    fun provideFcPlayerOperations(
        instance: Provider<FcPlayer_Bukkit_Paper.Operations>,
    ): FcPlayer.Operations {
        return instance.get()
    }

    @Provides
    fun provideFcItemOperations(
        instance: Provider<FcItem_Bukkit_Paper.Operations>,
    ): FcItem.Operations = instance.get()

    @Provides
    fun provideFcItemStackOperations(
        instance: Provider<FcItemStack_Bukkit_Paper.Operations>,
    ): FcItemStack.Operations = instance.get()

    @Provides
    fun provideFcTextColorOperations(
        instance: Provider<FcTextColor_Bukkit_Paper.Operations>,
    ): FcTextColor.Operations = instance.get()

}
