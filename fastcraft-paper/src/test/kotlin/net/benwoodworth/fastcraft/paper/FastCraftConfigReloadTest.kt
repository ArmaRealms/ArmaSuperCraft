package net.benwoodworth.fastcraft.paper

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.benwoodworth.fastcraft.FastCraftConfig
import net.benwoodworth.fastcraft.bukkit.config.FcConfig_Bukkit_Paper
import net.benwoodworth.fastcraft.crafting.view.FastCraftGuiView
import net.benwoodworth.fastcraft.crafting.view.buttons.*
import net.benwoodworth.fastcraft.platform.gui.FcGui
import net.benwoodworth.fastcraft.platform.gui.FcGuiButton
import net.benwoodworth.fastcraft.platform.gui.FcGuiLayout
import net.benwoodworth.fastcraft.platform.player.FcPlayer
import net.benwoodworth.fastcraft.platform.server.FcServer
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
import java.util.Locale
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
              buttons:
                craft-amount:
                  row: 1
                  column: 8
                refresh:
                  row: 0
                  column: 7
                page:
                  row: 1
                  column: 7
        """.trimIndent())

        assertTrue(settings.load())
        assertEquals("pt_BR", settings.language)
        assertTrue(settings.playerDefaults.enabled)
        assertEquals(2, settings.layout.height)
        assertEquals(0, settings.layout.recipes.row)
        assertEquals(2, settings.layout.recipes.height)
        assertEquals(1, settings.layout.buttons.page.row)
        assertEquals(0, settings.layout.buttons.refresh.row)
        assertEquals(7, settings.layout.buttons.page.column)
        assertEquals(14, settings.layout.recipes.width * settings.layout.recipes.height)
        assertGuiBuildsWithinInventory(settings)
        // Previously published settings are never mutated during preparation.
        assertEquals(4, originalLayout.height)
    }

    @Test
    fun invalidExplicitLayoutValuesStillUseBoundedDefaultsInMemory() {
        val file = dataFolder.resolve("config.yml")
        Files.writeString(file, """
            layout:
              height: 2
              recipes:
                row: 999
                height: 999
              buttons:
                craft-amount:
                  row: 1
                  column: 7
                refresh:
                  row: 0
                  column: 7
                page:
                  row: 999
        """.trimIndent())

        val settings = realSettings()
        assertEquals(2, settings.layout.height)
        assertEquals(0, settings.layout.recipes.row)
        assertEquals(2, settings.layout.recipes.height)
        assertEquals(1, settings.layout.buttons.page.row)
        assertGuiBuildsWithinInventory(settings)
    }

    @Test
    fun outOfRangeCustomButtonCoordinatesUseSafeDefaults() {
        val file = dataFolder.resolve("config.yml")
        Files.writeString(file, """
            layout:
              height: 2
              items:
                example:
                  enable: true
                  row: 999
                  column: -3
                  width: 1
                  height: 1
              buttons:
                craft-amount:
                  row: 1
                  column: 8
                refresh:
                  row: 0
                  column: 7
                page:
                  row: 1
                  column: 7
        """.trimIndent())

        val settings = realSettings()
        val custom = settings.layout.customButtons.buttons.single()
        assertEquals(0, custom.row)
        assertEquals(0, custom.column)
        assertGuiBuildsWithinInventory(settings)
    }

    @Test
    fun validCustomButtonFitsInsideSmallGui() {
        val file = dataFolder.resolve("config.yml")
        Files.writeString(file, """
            layout:
              height: 2
              items:
                example:
                  enable: true
                  row: 0
                  column: 0
                  width: 2
                  height: 2
              buttons:
                craft-amount:
                  row: 1
                  column: 8
                refresh:
                  row: 0
                  column: 7
                page:
                  row: 1
                  column: 7
        """.trimIndent())

        val settings = realSettings()
        val custom = settings.layout.customButtons.buttons.single()
        assertEquals(2, custom.width)
        assertEquals(2, custom.height)
        assertEquals(0, custom.column)
        assertEquals(0, custom.row)
        assertGuiBuildsWithinInventory(settings)
    }

    @Test
    fun invalidCustomButtonDimensionsRejectReloadAndKeepPreviousLayout() {
        val file = dataFolder.resolve("config.yml")
        Files.writeString(file, """
            layout:
              height: 4
              items:
                example:
                  enable: true
                  row: 0
                  column: 0
                  width: 2
                  height: 2
        """.trimIndent())

        val settings = realSettings()
        val original = settings.layout
        val cases = listOf(
            "row: 0\ncolumn: 0\nwidth: 0\nheight: 1",
            "row: 0\ncolumn: 0\nwidth: -1\nheight: 1",
            "row: 0\ncolumn: 0\nwidth: 10\nheight: 1",
            "row: 0\ncolumn: 0\nwidth: 1\nheight: 0",
            "row: 0\ncolumn: 0\nwidth: 1\nheight: -1",
            "row: 0\ncolumn: 0\nwidth: 1\nheight: 5",
            "row: 0\ncolumn: 8\nwidth: 2\nheight: 1",
            "row: 3\ncolumn: 0\nwidth: 1\nheight: 2",
        )

        for (fields in cases) {
            val invalid = "layout:\n  height: 4\n  items:\n    example:\n      enable: true\n" +
                fields.lines().joinToString("\n") { "      $it" } + "\n"
            Files.writeString(file, invalid)
            assertFalse(settings.load(), "Reload accepted invalid button: $fields")
            assertEquals(invalid, Files.readString(file))
            assertTrue(settings.layout === original)
            val current = settings.layout.customButtons.buttons.single()
            assertEquals(2, current.width)
            assertEquals(2, current.height)
        }
    }

    @Test
    fun malformedCustomButtonDimensionsPreventStartupWithoutOverwritingFile() {
        val file = dataFolder.resolve("config.yml")
        val invalid = """
            layout:
              height: 2
              items:
                example:
                  enable: true
                  row: 1
                  column: 8
                  width: 2
                  height: 1
        """.trimIndent()
        Files.writeString(file, invalid)

        assertFailsWith<IllegalStateException> { realSettings() }
        assertEquals(invalid, Files.readString(file))
    }

    private fun assertGuiBuildsWithinInventory(settings: FastCraftConfig) {
        val player = FcPlayer(Any())
        val playerOperations = mockk<FcPlayer.Operations>()
        with(playerOperations) {
            every { player.locale } returns Locale.US
        }

        val grid = mockk<FcGuiLayout.Grid>()
        val gui = mockk<FcGui<FcGuiLayout.Grid>>()
        val buttons = List(settings.layout.height * 9) { mockk<FcGuiButton>(relaxed = true) }
        every { gui.layout } returns grid
        every { grid.getButton(any(), any()) } answers {
            val column = firstArg<Int>()
            val row = secondArg<Int>()
            assertTrue(column in 0..8, "GUI requested invalid column: $column")
            assertTrue(row in 0 until settings.layout.height, "GUI requested invalid row: $row")
            buttons[row * 9 + column]
        }

        val factory = mockk<FcGui.Factory>()
        every { factory.createChestGui(player, any(), settings.layout.height) } returns gui

        val view = FastCraftGuiView(
            player = player,
            fcGuiFactory = factory,
            workbenchButtonFactory = mockk<WorkbenchButtonView.Factory>(relaxed = true),
            pageButtonFactory = mockk<PageButtonView.Factory>(relaxed = true),
            recipeButtonFactory = mockk<RecipeButtonView.Factory>(relaxed = true),
            craftAmountButtonFactory = mockk<CraftAmountButtonView.Factory>(relaxed = true),
            refreshButtonFactory = mockk<RefreshButtonView.Factory>(relaxed = true),
            customButtonViewFactory = mockk<CustomButtonView.Factory>(relaxed = true),
            fcTextFactory = mockk(relaxed = true),
            config = settings,
            fcPlayerOperations = playerOperations,
            fcServer = mockk<FcServer>(relaxed = true),
        )
        assertTrue(view.recipeButtons.isNotEmpty())
        assertEquals(settings.layout.buttons.craftingGrid.enable, view.craftingGridButton != null)
        assertEquals(settings.layout.buttons.craftAmount.enable, view.craftAmountButton != null)
        assertEquals(settings.layout.buttons.refresh.enable, view.refreshButton != null)
        assertEquals(settings.layout.buttons.page.enable, view.pageButton != null)
        assertEquals(
            settings.layout.customButtons.buttons
                .filter { it.enable }
                .sumOf { it.width * it.height },
            view.customButtons.size,
        )
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
