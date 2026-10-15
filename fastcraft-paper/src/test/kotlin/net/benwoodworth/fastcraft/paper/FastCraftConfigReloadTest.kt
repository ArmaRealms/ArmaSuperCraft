package net.benwoodworth.fastcraft.paper

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.benwoodworth.fastcraft.FastCraftConfig
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FastCraftConfigReloadTest {
    @TempDir
    lateinit var dataFolder: Path

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
