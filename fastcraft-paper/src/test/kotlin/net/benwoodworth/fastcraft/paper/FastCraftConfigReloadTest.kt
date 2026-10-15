package net.benwoodworth.fastcraft.paper

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.benwoodworth.fastcraft.FastCraftConfig
import net.benwoodworth.fastcraft.bukkit.config.FcConfig_Bukkit_Paper
import net.benwoodworth.fastcraft.platform.config.FcConfig
import net.benwoodworth.fastcraft.platform.config.FcConfigNode
import net.benwoodworth.fastcraft.platform.server.FcLogger
import net.benwoodworth.fastcraft.platform.server.FcPluginData
import net.benwoodworth.fastcraft.platform.text.FcText
import net.benwoodworth.fastcraft.platform.world.FcItem
import net.benwoodworth.fastcraft.platform.world.FcItemStack
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import javax.inject.Provider
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class FastCraftConfigReloadTest {
    @TempDir
    lateinit var dataFolder: Path

    @Test
    fun malformedYamlPreventsStartupWithoutOverwritingFile() {
        val file = dataFolder.resolve("config.yml")
        val invalidYaml = "player-defaults: [this is not valid yaml"
        Files.writeString(file, invalidYaml)

        val failure = assertFailsWith<IllegalStateException> { realSettings() }

        assertTrue(failure.message.orEmpty().contains("cannot start"))
        assertEquals(invalidYaml, Files.readString(file))
    }

    @Test
    fun invalidRecipeRegexPreventsStartupWithoutOverwritingFile() {
        val file = dataFolder.resolve("config.yml")
        val invalidConfig = """
            player-defaults:
              enabled: false
            disable-recipes:
              recipe-ids:
                - "/[/"
        """.trimIndent()
        Files.writeString(file, invalidConfig)

        val failure = assertFailsWith<IllegalStateException> { realSettings() }

        assertTrue(failure.message.orEmpty().contains("cannot start"))
        assertEquals(invalidConfig, Files.readString(file))
    }

    @Test
    fun failedReadReturnsFalseWithoutDiscardingCurrentSettings() {
        val file = dataFolder.resolve("config.yml")
        Files.writeString(file, "# existing configuration")

        val factory = mockk<FcConfig.Factory>()
        val config = mockk<FcConfig>(relaxed = true)
        val node = mockk<FcConfigNode>(relaxed = true)
        val logger = mockk<FcLogger>(relaxed = true)

        every { factory.create() } returns config
        every { factory.load(file) } returns config
        every { config.get(any()) } returns node
        every { node.get(any()) } returns node
        every { node.getChildKeys() } returns emptySet()
        every { node.getString() } returns null
        every { node.getStringList() } returns emptyList()
        every { node.getInt() } returns null
        every { node.getDouble() } returns null
        every { node.getBoolean() } returns false

        val settings = FastCraftConfig(
            fcConfigFactory = factory,
            fcPluginData = object : FcPluginData {
                override val dataFolder = this@FastCraftConfigReloadTest.dataFolder
                override val configFile = file
            },
            fcItemFactory = itemFactory,
            fcItemStackFactory = itemStackFactory,
            fcLogger = logger,
            fcItemOperations = itemOperations,
            fcItemStackOperations = itemStackOperations,
        )

        assertTrue(settings.load())
        assertFalse(settings.playerDefaults.enabled)

        every { factory.load(file) } throws IllegalArgumentException("broken config")
        assertFalse(settings.load())
        assertFalse(settings.playerDefaults.enabled)
        verify(exactly = 1) { logger.error(match { it.contains("broken config") }) }
    }

    @Test
    fun malformedYamlDoesNotReplaceActiveSettingsOrOverwriteDisk() {
        val file = dataFolder.resolve("config.yml")
        Files.writeString(file, """
            language: en
            player-defaults:
              enabled: false
            layout:
              height: 4
        """.trimIndent())

        val settings = realSettings()
        assertEquals("en", settings.language)
        assertFalse(settings.playerDefaults.enabled)
        assertEquals(4, settings.layout.height)

        val invalidYaml = "player-defaults: [this is not valid yaml"
        Files.writeString(file, invalidYaml)

        assertFalse(settings.load())
        assertEquals(invalidYaml, Files.readString(file))
        assertEquals("en", settings.language)
        assertFalse(settings.playerDefaults.enabled)
        assertEquals(4, settings.layout.height)
    }

    @Test
    fun invalidRecipeRegexCannotPartiallyApplyUpdatedSettings() {
        val file = dataFolder.resolve("config.yml")
        Files.writeString(file, """
            language: en
            player-defaults:
              enabled: false
            recipe-calculations:
              max-tick-usage: 0.15
            layout:
              height: 4
            disable-recipes:
              recipe-ids:
                - minecraft:stone
        """.trimIndent())

        val settings = realSettings()
        assertEquals("en", settings.language)
        assertFalse(settings.playerDefaults.enabled)
        assertEquals(4, settings.layout.height)
        assertEquals(0.15, settings.recipeCalculations.maxTickUsage)
        assertTrue(settings.disableRecipes.recipeIdsRegex.matches("minecraft:stone"))

        val invalidRegex = """
            language: pt_BR
            player-defaults:
              enabled: true
            recipe-calculations:
              max-tick-usage: 0.3
            layout:
              height: 2
            disable-recipes:
              recipe-ids:
                - "/[/"
        """.trimIndent()
        Files.writeString(file, invalidRegex)

        assertFalse(settings.load())
        assertEquals(invalidRegex, Files.readString(file))
        assertEquals("en", settings.language)
        assertFalse(settings.playerDefaults.enabled)
        assertEquals(4, settings.layout.height)
        assertEquals(0.15, settings.recipeCalculations.maxTickUsage)
        assertTrue(settings.disableRecipes.recipeIdsRegex.matches("minecraft:stone"))
    }

    @Test
    fun validReloadPublishesAllNewSettingsTogether() {
        val file = dataFolder.resolve("config.yml")
        Files.writeString(file, """
            language: en
            player-defaults:
              enabled: false
            layout:
              height: 4
        """.trimIndent())
        val settings = realSettings()
        val originalLayout = settings.layout

        Files.writeString(file, """
            language: pt_BR
            player-defaults:
              enabled: true
            layout:
              height: 2
        """.trimIndent())

        assertTrue(settings.load())
        assertEquals("pt_BR", settings.language)
        assertTrue(settings.playerDefaults.enabled)
        assertEquals(2, settings.layout.height)
        // Previously published settings are never mutated during preparation.
        assertEquals(4, originalLayout.height)
    }

    private fun realSettings(): FastCraftConfig {
        lateinit var factory: FcConfig_Bukkit_Paper.Factory
        factory = FcConfig_Bukkit_Paper.Factory(Provider { factory })
        return FastCraftConfig(
            fcConfigFactory = factory,
            fcPluginData = object : FcPluginData {
                override val dataFolder: Path = this@FastCraftConfigReloadTest.dataFolder
                override val configFile: Path = dataFolder.resolve("config.yml")
            },
            fcItemFactory = itemFactory,
            fcItemStackFactory = itemStackFactory,
            fcLogger = mockk(relaxed = true),
            fcItemOperations = itemOperations,
            fcItemStackOperations = itemStackOperations,
        )
    }

    private val itemFactory = object : FcItem.Factory {
        override val air = FcItem("minecraft:air")
        override val ironSword = FcItem("minecraft:iron_sword")
        override val craftingTable = FcItem("minecraft:crafting_table")
        override val anvil = FcItem("minecraft:anvil")
        override val netherStar = FcItem("minecraft:nether_star")
        override val lightGrayStainedGlassPane = FcItem("minecraft:light_gray_stained_glass_pane")
        override val stoneButton = FcItem("minecraft:stone_button")
        override fun parseOrNull(id: String): FcItem? = FcItem(id)
    }

    private val itemStackFactory = object : FcItemStack.Factory {
        override fun create(item: FcItem, amount: Int): FcItemStack = FcItemStack(item.value)
        override fun parseOrNull(itemStr: String, amount: Int): FcItemStack? = FcItemStack(itemStr)
    }

    private val itemOperations = object : FcItem.Operations {
        override val FcItem.id: String get() = value.toString()
        override val FcItem.name: FcText get() = error("Unused by configuration")
        override val FcItem.maxAmount: Int get() = 64
        override val FcItem.craftingRemainingItem: FcItem? get() = null
    }

    private val itemStackOperations = object : FcItemStack.Operations {
        override var FcItemStack.type: FcItem
            get() = FcItem(value)
            set(value) {}

        override var FcItemStack.amount: Int
            get() = 1
            set(value) {}

        override val FcItemStack.name: FcText get() = error("Unused by configuration")
        override val FcItemStack.lore: List<FcText> get() = emptyList()
        override val FcItemStack.hasMetadata: Boolean get() = false
        override fun FcItemStack.copy(): FcItemStack = this
    }
}
