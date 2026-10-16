package net.benwoodworth.fastcraft.bukkit.util

import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitScheduler
import javax.inject.Inject
import javax.inject.Singleton

/** Correlates an interaction with at most one inventory opening in the same tick. */
@Singleton
class CauseTracker @Inject constructor(
    private val scheduler: BukkitScheduler,
    private val plugin: Plugin,
) {
    private var clearCausesTask = -1
    private val pending = mutableSetOf<Any>()
    private val seen = mutableSetOf<Any>()

    fun trackCause(cause: Any) {
        // A second hand event must not rearm an already consumed interaction.
        if (seen.add(cause)) {
            pending.add(cause)
        }
        if (clearCausesTask == -1) {
            clearCausesTask = scheduler.scheduleSyncDelayedTask(plugin, {
                pending.clear()
                seen.clear()
                clearCausesTask = -1
            }, 0L)
        }
    }

    /** An opening consumes the interaction even when the vanilla menu is kept. */
    fun consumeCause(cause: Any): Boolean = pending.remove(cause)
}
