package net.benwoodworth.fastcraft.platform.recipe

/** A provider performs the entire craft transaction. FastCraft must not consume or give items. */
interface FcDelegatedCraftingRecipePrepared : FcCraftingRecipePrepared {
    fun commit(repetitions: Int, dropResults: Boolean): FcDelegatedCraftResult
}

enum class FcDelegatedCraftResult {
    SUCCESS,
    REJECTED,
    UNAVAILABLE,
}
