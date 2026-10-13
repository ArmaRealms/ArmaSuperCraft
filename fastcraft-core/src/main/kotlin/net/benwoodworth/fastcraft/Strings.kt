package net.benwoodworth.fastcraft

import net.benwoodworth.fastcraft.platform.server.FcLogger
import java.nio.file.Path
import java.util.*

object Strings {
    // region locale keys
    private const val LOCALIZATION_TRANSLATOR = "localization.translator"
    private const val LOCALIZATION_TRANSLATOR_FROM = "localization.translator.from"
    private const val GUI_TITLE = "gui.title"
    private const val GUI_RECIPE_INGREDIENTS = "gui.recipe.ingredients"
    private const val GUI_RECIPE_INGREDIENTS_ITEM = "gui.recipe.ingredients.item"
    private const val GUI_RECIPE_RESULTS = "gui.recipe.results"
    private const val GUI_RECIPE_RESULTS_ITEM = "gui.recipe.results.item"
    private const val GUI_RECIPE_ID = "gui.recipe.id"
    private const val GUI_BUTTON_PAGE_TITLE = "gui.button.page.title"
    private const val GUI_BUTTON_PAGE_DESCRIPTION_0 = "gui.button.page.description.0"
    private const val GUI_BUTTON_PAGE_DESCRIPTION_1 = "gui.button.page.description.1"
    private const val GUI_BUTTON_PAGE_DESCRIPTION_2 = "gui.button.page.description.2"
    private const val GUI_BUTTON_REFRESH_TITLE = "gui.button.refresh.title"
    private const val GUI_BUTTON_REFRESH_DESCRIPTION_0 = "gui.button.refresh.description.0"
    private const val GUI_BUTTON_CRAFT_AMOUNT_TITLE = "gui.button.craft-amount.title"
    private const val GUI_BUTTON_CRAFT_AMOUNT_DESCRIPTION_0 = "gui.button.craft-amount.description.0"
    private const val GUI_BUTTON_CRAFT_AMOUNT_DESCRIPTION_1 = "gui.button.craft-amount.description.1"
    private const val GUI_BUTTON_CRAFT_AMOUNT_DESCRIPTION_2 = "gui.button.craft-amount.description.2"
    private const val GUI_BUTTON_CRAFTING_GRID_TITLE = "gui.button.crafting-grid.title"
    private const val GUI_BUTTON_CRAFTING_GRID_DESCRIPTION_0 = "gui.button.crafting-grid.description.0"
    private const val GUI_BUTTON_CRAFTING_GRID_DESCRIPTION_1 = "gui.button.crafting-grid.description.1"
    private const val COMMAND_ERROR_USAGE = "command.error.usage"
    private const val COMMAND_ERROR_PERMISSION = "command.error.permission"
    private const val COMMAND_ERROR_CONSOLE_ONLY = "command.error.console.only"
    private const val COMMAND_ERROR_PLAYER_ONLY = "command.error.player.only"
    private const val COMMAND_ERROR_PLAYER_UNKNOWN = "command.error.player.unknown"
    private const val COMMAND_SET_ENABLED_TRUE = "command.set.enabled.true"
    private const val COMMAND_SET_ENABLED_FALSE = "command.set.enabled.false"
    private const val COMMAND_SET_ENABLED_TRUE_PLAYER = "command.set.enabled.true.player"
    private const val COMMAND_SET_ENABLED_FALSE_PLAYER = "command.set.enabled.false.player"
    private const val COMMAND_RELOAD_RELOADED = "command.reload.reloaded"
    // endregion

    /** Load disk-editable server translations; the configured language overrides player locale. */
    fun load(dataFolder: Path, language: String, logger: FcLogger) {
        FastCraftTranslations.load(dataFolder, language, logger)
    }

    fun localizationTranslator(locale: Locale): String =
        FastCraftTranslations.get(locale, LOCALIZATION_TRANSLATOR) ?: LOCALIZATION_TRANSLATOR

    fun localizationTranslatorFrom(locale: Locale): String =
        FastCraftTranslations.get(locale, LOCALIZATION_TRANSLATOR_FROM) ?: LOCALIZATION_TRANSLATOR_FROM

    fun guiTitle(locale: Locale): String = FastCraftTranslations.get(locale, GUI_TITLE) ?: GUI_TITLE

    fun guiRecipeIngredients(locale: Locale): String =
        FastCraftTranslations.get(locale, GUI_RECIPE_INGREDIENTS) ?: GUI_RECIPE_INGREDIENTS

    fun guiRecipeIngredientsItem(locale: Locale): String =
        FastCraftTranslations.get(locale, GUI_RECIPE_INGREDIENTS_ITEM) ?: GUI_RECIPE_INGREDIENTS_ITEM

    fun guiRecipeResults(locale: Locale): String =
        FastCraftTranslations.get(locale, GUI_RECIPE_RESULTS) ?: GUI_RECIPE_RESULTS

    fun guiRecipeResultsItem(locale: Locale): String =
        FastCraftTranslations.get(locale, GUI_RECIPE_RESULTS_ITEM) ?: GUI_RECIPE_RESULTS_ITEM

    fun guiRecipeId(locale: Locale): String = FastCraftTranslations.get(locale, GUI_RECIPE_ID) ?: GUI_RECIPE_ID

    fun guiButtonPageTitle(locale: Locale): String =
        FastCraftTranslations.get(locale, GUI_BUTTON_PAGE_TITLE) ?: GUI_BUTTON_PAGE_TITLE

    fun guiButtonPageDescription0(locale: Locale): String =
        FastCraftTranslations.get(locale, GUI_BUTTON_PAGE_DESCRIPTION_0) ?: GUI_BUTTON_PAGE_DESCRIPTION_0

    fun guiButtonPageDescription1(locale: Locale): String =
        FastCraftTranslations.get(locale, GUI_BUTTON_PAGE_DESCRIPTION_1) ?: GUI_BUTTON_PAGE_DESCRIPTION_1

    fun guiButtonPageDescription2(locale: Locale): String =
        FastCraftTranslations.get(locale, GUI_BUTTON_PAGE_DESCRIPTION_2) ?: GUI_BUTTON_PAGE_DESCRIPTION_2

    fun guiButtonRefreshTitle(locale: Locale): String =
        FastCraftTranslations.get(locale, GUI_BUTTON_REFRESH_TITLE) ?: GUI_BUTTON_REFRESH_TITLE

    fun guiButtonRefreshDescription0(locale: Locale): String =
        FastCraftTranslations.get(locale, GUI_BUTTON_REFRESH_DESCRIPTION_0) ?: GUI_BUTTON_REFRESH_DESCRIPTION_0

    fun guiButtonCraftAmountTitle(locale: Locale): String =
        FastCraftTranslations.get(locale, GUI_BUTTON_CRAFT_AMOUNT_TITLE) ?: GUI_BUTTON_CRAFT_AMOUNT_TITLE

    fun guiButtonCraftAmountDescription0(locale: Locale): String =
        FastCraftTranslations.get(locale, GUI_BUTTON_CRAFT_AMOUNT_DESCRIPTION_0)
            ?: GUI_BUTTON_CRAFT_AMOUNT_DESCRIPTION_0

    fun guiButtonCraftAmountDescription1(locale: Locale): String =
        FastCraftTranslations.get(locale, GUI_BUTTON_CRAFT_AMOUNT_DESCRIPTION_1)
            ?: GUI_BUTTON_CRAFT_AMOUNT_DESCRIPTION_1

    fun guiButtonCraftAmountDescription2(locale: Locale): String =
        FastCraftTranslations.get(locale, GUI_BUTTON_CRAFT_AMOUNT_DESCRIPTION_2)
            ?: GUI_BUTTON_CRAFT_AMOUNT_DESCRIPTION_2

    fun guiButtonCraftingGridTitle(locale: Locale): String =
        FastCraftTranslations.get(locale, GUI_BUTTON_CRAFTING_GRID_TITLE) ?: GUI_BUTTON_CRAFTING_GRID_TITLE

    fun guiButtonCraftingGridDescription0(locale: Locale): String =
        FastCraftTranslations.get(locale, GUI_BUTTON_CRAFTING_GRID_DESCRIPTION_0)
            ?: GUI_BUTTON_CRAFTING_GRID_DESCRIPTION_0

    fun guiButtonCraftingGridDescription1(locale: Locale): String =
        FastCraftTranslations.get(locale, GUI_BUTTON_CRAFTING_GRID_DESCRIPTION_1)
            ?: GUI_BUTTON_CRAFTING_GRID_DESCRIPTION_1

    fun commandErrorUsage(locale: Locale): String =
        FastCraftTranslations.get(locale, COMMAND_ERROR_USAGE) ?: COMMAND_ERROR_USAGE

    fun commandErrorPermission(locale: Locale): String =
        FastCraftTranslations.get(locale, COMMAND_ERROR_PERMISSION) ?: COMMAND_ERROR_PERMISSION

    fun commandErrorConsoleOnly(locale: Locale): String =
        FastCraftTranslations.get(locale, COMMAND_ERROR_CONSOLE_ONLY) ?: COMMAND_ERROR_CONSOLE_ONLY

    fun commandErrorPlayerOnly(locale: Locale): String =
        FastCraftTranslations.get(locale, COMMAND_ERROR_PLAYER_ONLY) ?: COMMAND_ERROR_PLAYER_ONLY

    fun commandErrorPlayerUnknown(locale: Locale): String =
        FastCraftTranslations.get(locale, COMMAND_ERROR_PLAYER_UNKNOWN) ?: COMMAND_ERROR_PLAYER_UNKNOWN

    fun commandSetEnabledTrue(locale: Locale): String =
        FastCraftTranslations.get(locale, COMMAND_SET_ENABLED_TRUE) ?: COMMAND_SET_ENABLED_TRUE

    fun commandSetEnabledFalse(locale: Locale): String =
        FastCraftTranslations.get(locale, COMMAND_SET_ENABLED_FALSE) ?: COMMAND_SET_ENABLED_FALSE

    fun commandSetEnabledTruePlayer(locale: Locale): String =
        FastCraftTranslations.get(locale, COMMAND_SET_ENABLED_TRUE_PLAYER) ?: COMMAND_SET_ENABLED_TRUE_PLAYER

    fun commandSetEnabledFalsePlayer(locale: Locale): String =
        FastCraftTranslations.get(locale, COMMAND_SET_ENABLED_FALSE_PLAYER) ?: COMMAND_SET_ENABLED_FALSE_PLAYER

    fun commandReloadReloaded(locale: Locale): String =
        FastCraftTranslations.get(locale, COMMAND_RELOAD_RELOADED) ?: COMMAND_RELOAD_RELOADED
}
