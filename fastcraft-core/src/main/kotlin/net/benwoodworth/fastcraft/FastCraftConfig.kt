package net.benwoodworth.fastcraft

import net.benwoodworth.fastcraft.platform.config.FcConfig
import net.benwoodworth.fastcraft.platform.config.FcConfigNode
import net.benwoodworth.fastcraft.platform.server.FcLogger
import net.benwoodworth.fastcraft.platform.server.FcPluginData
import net.benwoodworth.fastcraft.platform.world.FcItem
import net.benwoodworth.fastcraft.platform.world.FcItemStack
import net.benwoodworth.fastcraft.util.Expr
import java.nio.file.Files
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KMutableProperty0

@Singleton
class FastCraftConfig @Inject constructor(
    private val fcConfigFactory: FcConfig.Factory,
    private val fcPluginData: FcPluginData,
    private val fcItemFactory: FcItem.Factory,
    private val fcItemStackFactory: FcItemStack.Factory,
    private val fcLogger: FcLogger,
    fcItemOperations: FcItem.Operations,
    fcItemStackOperations: FcItemStack.Operations,
) : FcItem.Operations by fcItemOperations,
    FcItemStack.Operations by fcItemStackOperations {

    // Complete candidate settings are prepared before swapping the active snapshot.
    private data class Settings(
        val config: FcConfig,
        val language: String,
        val disableRecipes: DisableRecipes,
        val recipeCalculations: RecipeCalculations,
        val playerDefaults: PlayerDefaults,
        val layout: Layout,
    )

    private fun createSettings(config: FcConfig, language: String) = Settings(
        config = config,
        language = language,
        disableRecipes = DisableRecipes(config),
        recipeCalculations = RecipeCalculations(config),
        playerDefaults = PlayerDefaults(config),
        layout = Layout(config),
    )

    @Volatile
    private var active = createSettings(fcConfigFactory.create(), "pt_BR")
    private var modified: Boolean = false
    private var newFile: Boolean = false

    private val header: String = """
        https://github.com/BenWoodworth/FastCraft/wiki/Configuration
    """.trimIndent()

    /** Locale file name under plugins/FastCraft/lang (without .json). */
    val language: String get() = active.language

    val disableRecipes: DisableRecipes get() = active.disableRecipes

    val recipeCalculations: RecipeCalculations get() = active.recipeCalculations

    val playerDefaults: PlayerDefaults get() = active.playerDefaults

    val layout: Layout get() = active.layout

    inner class DisableRecipes(private val config: FcConfig) {
        private val node: FcConfigNode
            get() = config["disable-recipes"]

        private val none = Regex("(?" + "!)") // Matches nothing

        private var recipeIds: List<Expr> = emptyList()
            set(values) {
                field = values
                recipeIdsRegex = values
                    .takeUnless { it.isEmpty() }
                    ?.joinToString("|") { it.toRegex().toString() }
                    ?.let { Regex(it) }
                    ?: none
            }

        var recipeIdsRegex: Regex = none
            private set

        fun load() {
            node["recipe-ids"].loadExprList(::recipeIds)
        }
    }

    inner class RecipeCalculations(private val config: FcConfig) {
        private val node: FcConfigNode
            get() = config["recipe-calculations"]

        var maxTickUsage: Double = 0.1
            private set

        fun load() {
            node["max-tick-usage"].loadDouble(::maxTickUsage, min = 0.0)
        }
    }

    inner class PlayerDefaults(private val config: FcConfig) {
        private val node: FcConfigNode
            get() = config["player-defaults"]

        var enabled: Boolean = true
            private set

        fun load() {
            node["enabled"].loadBoolean(::enabled)
        }
    }

    inner class Layout(private val config: FcConfig) {
        private val node: FcConfigNode
            get() = config["layout"]

        var height: Int = 6
            private set

        val recipes = Recipes()

        inner class Recipes {
            private val node: FcConfigNode
                get() = this@Layout.node["recipes"]

            var row: Int = 0
                private set

            var column: Int = 0
                private set

            var width: Int = 7
                private set

            var height: Int = 6
                private set

            fun load() {
                node["row"].loadInt(::row, 0, this@Layout.height - 1)
                node["column"].loadInt(::column, 0, 8)
                node["width"].loadInt(::width, 1, 9 - column)
                node["height"].loadInt(::height, 1, this@Layout.height - row)
            }
        }

        val buttons = Buttons()

        inner class Buttons {
            private val node: FcConfigNode
                get() = this@Layout.node["buttons"]

            val craftingGrid = Button(
                getNode = { node["crafting-grid"] },
                enable = true,
                item = fcItemStackFactory.create(fcItemFactory.craftingTable),
                row = 0,
                column = 8,
            )

            val craftAmount = Button(
                getNode = { node["craft-amount"] },
                enable = true,
                item = fcItemStackFactory.create(fcItemFactory.anvil),
                row = 1,
                column = 8,
            )

            val refresh = Button(
                getNode = { node["refresh"] },
                enable = true,
                item = fcItemStackFactory.create(fcItemFactory.netherStar),
                row = 2,
                column = 8,
            )

            val page = Button(
                getNode = { node["page"] },
                enable = true,
                item = fcItemStackFactory.create(fcItemFactory.ironSword),
                row = 5,
                column = 8,
            )

            fun load() {
                craftingGrid.load()
                craftAmount.load()
                refresh.load()
                page.load()
            }
        }

        val customButtons = CustomButtons()

        inner class CustomButtons {
            private val node: FcConfigNode
                get() = this@Layout.node["items"]

            var buttons: Set<CustomButton> = emptySet()
                private set

            fun load() {
                buttons = node.getChildKeys()
                    .map {
                        CustomButton(
                            getNode = { node[it] },
                            enable = false,
                            item = fcItemStackFactory.create(fcItemFactory.air),
                            row = 0,
                            column = 0,
                        )
                    }
                    .let {
                        it.takeUnless { it.isEmpty() } ?: listOf(
                            CustomButton(
                                getNode = { node["custom-button-example"] },
                                enable = false,
                                item = fcItemStackFactory.create(fcItemFactory.stoneButton),
                                row = 3,
                                column = 8,
                                command = "say <player> pressed a custom button"
                            )
                        )
                    }
                    .onEach { it.load() }
                    .toSet()
            }
        }

        val background = EnableItem(
            getNode = { this@Layout.node["background"] },
            enable = false,
            item = fcItemStackFactory.create(fcItemFactory.lightGrayStainedGlassPane),
        )

        open inner class EnableItem(
            private val getNode: () -> FcConfigNode,
            enable: Boolean,
            item: FcItemStack,
        ) {
            val node: FcConfigNode
                get() = getNode()

            var enable: Boolean = enable
                private set

            var item: FcItemStack = item
                private set

            private var itemId: String = item.type.id

            open fun load() {
                node["enable"].loadBoolean(::enable)
                node["item"].loadItem(::item, ::itemId)
            }
        }

        open inner class Button(
            getNode: () -> FcConfigNode,
            enable: Boolean,
            item: FcItemStack,
            row: Int,
            column: Int,
        ) : EnableItem(
            getNode = getNode,
            enable = enable,
            item = item,
        ) {
            var row: Int = row
                private set

            var column: Int = column
                private set

            override fun load() {
                super.load()
                node["row"].loadInt(::row, 0, this@Layout.height - 1)
                node["column"].loadInt(::column, 0, 8)
            }
        }

        inner class CustomButton(
            getNode: () -> FcConfigNode,
            enable: Boolean,
            item: FcItemStack,
            row: Int,
            column: Int,
            width: Int = 1,
            height: Int = 1,
            command: String? = null,
        ) : Button(
            getNode = getNode,
            enable = enable,
            item = item,
            row = row,
            column = column,
        ) {
            var width: Int = width
                private set

            var height: Int = height
                private set

            var command: String? = command
                get() = field?.takeUnless { it.isBlank() }
                private set

            override fun load() {
                super.load()
                val requestedWidth = node["width"].getInt() ?: width
                val requestedHeight = node["height"].getInt() ?: height
                require(requestedWidth in 1..(9 - column)) {
                    "${node.path}.width must be between 1 and ${9 - column}"
                }
                require(requestedHeight in 1..(this@Layout.height - row)) {
                    "${node.path}.height must be between 1 and ${this@Layout.height - row}"
                }
                width = requestedWidth
                height = requestedHeight
                node["command"].loadString(::command, command)
            }
        }

        private fun validateOccupiedSlots() {
            // This must match the GUI's placement order: built-in buttons, custom
            // buttons, then the recipe area. Background is only drawn on free slots.
            val occupied = mutableMapOf<Int, String>()

            fun reserve(column: Int, row: Int, name: String) {
                val index = row * 9 + column
                val previous = occupied.putIfAbsent(index, name)
                require(previous == null) {
                    "layout: $name overlaps $previous at row $row, column $column"
                }
            }

            val standardButtons = listOf(
                "layout.buttons.crafting-grid" to buttons.craftingGrid,
                "layout.buttons.craft-amount" to buttons.craftAmount,
                "layout.buttons.refresh" to buttons.refresh,
                "layout.buttons.page" to buttons.page,
            )
            for ((name, button) in standardButtons) {
                if (button.enable) {
                    reserve(button.column, button.row, name)
                }
            }

            for (button in customButtons.buttons) {
                if (!button.enable) continue
                for (row in button.row until button.row + button.height) {
                    for (column in button.column until button.column + button.width) {
                        reserve(column, row, button.node.path)
                    }
                }
            }

            val hasRecipeSlot = (recipes.row until recipes.row + recipes.height).any { row ->
                (recipes.column until recipes.column + recipes.width).any { column ->
                    row * 9 + column !in occupied
                }
            }
            require(hasRecipeSlot) {
                "layout.recipes has no free slots after placing enabled buttons"
            }
        }

        fun load() {
            node["height"].loadInt(::height, 1, 6)
            recipes.load()
            buttons.load()
            customButtons.load()
            background.load()
            validateOccupiedSlots()
        }
    }

    fun load(): Boolean {
        val file = fcPluginData.configFile
        val exists = Files.exists(file)
        modified = !exists
        newFile = !exists

        return try {
            Files.createDirectories(file.parent)
            val candidateConfig = if (exists) fcConfigFactory.load(file) else fcConfigFactory.create()

            if (candidateConfig.headerComment != header) {
                candidateConfig.headerComment = header
                modified = true
            }

            // The configured server language is shared by all players.
            val languageNode = candidateConfig["language"]
            val configuredLanguage = languageNode.getString()?.trim()
            val candidateLanguage = configuredLanguage?.takeIf { it.isNotEmpty() } ?: "pt_BR"
            if (configuredLanguage.isNullOrEmpty()) {
                languageNode.modify(candidateLanguage)
            }

            // Validate all settings in isolation; exceptions must not change active settings.
            val candidate = createSettings(candidateConfig, candidateLanguage)
            candidate.disableRecipes.load()
            candidate.recipeCalculations.load()
            candidate.playerDefaults.load()
            candidate.layout.load()

            if (modified) {
                candidateConfig.save(file)
            }
            if (newFile) {
                fcLogger.info("Created ${file.fileName}")
            }

            // Load translations before exposing the completed configuration.
            Strings.load(fcPluginData.dataFolder, candidateLanguage, fcLogger)
            active = candidate
            true
        } catch (e: Exception) {
            fcLogger.error("Error loading ${file.fileName}: ${e.message}")
            fcLogger.info("Keeping current configuration")
            false
        } finally {
            modified = false
            newFile = false
        }
    }

    init {
        check(load()) { "FastCraft cannot start without a valid configuration" }
    }

    private inline fun <T> FcConfigNode.modify(
        newValue: T,
        message: (newValue: T) -> String = { "Set value to $it" },
    ): T {
        if (!newFile) {
            fcLogger.info("${fcPluginData.configFile.fileName} [$path]: ${message(newValue)}")
        }

        set(newValue)
        modified = true
        return newValue
    }

    private fun FcConfigNode.logErr(message: String) {
        fcLogger.error("${fcPluginData.configFile.fileName} [$path]: $message")
    }

    private fun FcConfigNode.loadInt(
        property: KMutableProperty0<Int>,
        min: Int = Int.MIN_VALUE,
        max: Int = Int.MAX_VALUE,
        default: Int = property.get(),
    ) {
        val safeDefault = default.coerceIn(min, max)
        when (val newValue = getInt()) {
            null -> property.set(modify(safeDefault))
            !in min..max -> {
                property.set(safeDefault)
                logErr("$newValue is not in $min..$max. Defaulting to ${safeDefault}.")
            }

            else -> property.set(newValue)
        }
    }

    private fun FcConfigNode.loadDouble(
        property: KMutableProperty0<Double>,
        min: Double = Double.NEGATIVE_INFINITY,
        max: Double = Double.POSITIVE_INFINITY,
        default: Double = property.get(),
    ) {
        when (val newValue = getDouble()) {
            null -> modify(default)
            !in min..max -> {
                property.set(default)
                logErr("$newValue is not in $min..$max. Defaulting to ${default}.")
            }

            else -> property.set(newValue)
        }
    }

    private fun FcConfigNode.loadBoolean(
        property: KMutableProperty0<Boolean>,
        default: Boolean = property.get(),
    ) {
        when (val newValue = getBoolean()) {
            null -> property.set(modify(default))
            else -> property.set(newValue)
        }
    }

    private fun FcConfigNode.loadString(
        property: KMutableProperty0<String?>,
        default: String? = property.get(),
    ) {
        when (val newValue = getString()) {
            null -> if (default != null) property.set(modify(default))
            else -> property.set(newValue)
        }
    }

    private fun FcConfigNode.loadItem(
        itemProperty: KMutableProperty0<FcItemStack>,
        itemIdProperty: KMutableProperty0<String>,
        default: String = itemIdProperty.get(),
    ) {
        when (val newItemId = getString()) {
            null -> modify(default)
            else -> when (val newItem = fcItemStackFactory.parseOrNull(newItemId)) {
                null -> logErr("Invalid item id: $newItemId. Defaulting to $default.")
                else -> {
                    itemIdProperty.set(newItemId)
                    itemProperty.set(newItem)
                }
            }
        }
    }

    private fun FcConfigNode.loadExprList(
        property: KMutableProperty0<List<Expr>>,
        default: List<Expr> = property.get(),
    ) {
        when (val newDisableRecipes = getStringList()) {
            null -> modify(default.map { it.expression })
            else -> {
                val nonNulls = newDisableRecipes.filterNotNull()
                if (nonNulls.size != newDisableRecipes.size) {
                    modify(nonNulls) { "Removed null entries" }
                }
                property.set(nonNulls.map { Expr(it) })
            }
        }
    }
}
