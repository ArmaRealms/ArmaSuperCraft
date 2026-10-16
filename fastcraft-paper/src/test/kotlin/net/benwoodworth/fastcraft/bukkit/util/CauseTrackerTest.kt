package net.benwoodworth.fastcraft.bukkit.util

import io.mockk.every
import io.mockk.mockk
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitScheduler
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CauseTrackerTest {
    private val plugin = mockk<Plugin>()
    private val scheduler = mockk<BukkitScheduler>()
    private val tasks = mutableListOf<Runnable>()
    private val tracker = CauseTracker(scheduler, plugin)

    init {
        every { scheduler.scheduleSyncDelayedTask(plugin, any<Runnable>(), 0L) } answers {
            tasks += secondArg<Runnable>()
            tasks.size
        }
    }

    @Test
    fun consumesAnInteractionAtMostOncePerTick() {
        tracker.trackCause("player")
        assertTrue(tracker.consumeCause("player"))
        assertFalse(tracker.consumeCause("player"))

        tracker.trackCause("player") // The other hand cannot rearm it.
        assertFalse(tracker.consumeCause("player"))
        assertTrue(tasks.size == 1)
    }

    @Test
    fun expiresPendingCausesAndAllowsNextTick() {
        tracker.trackCause("player")
        tasks.single().run()
        assertFalse(tracker.consumeCause("player"))

        tracker.trackCause("player")
        assertTrue(tracker.consumeCause("player"))
        assertTrue(tasks.size == 2)
    }

    @Test
    fun keepsPlayersIndependentAndInvalidatesOnlyConsumedCause() {
        tracker.trackCause("alice")
        tracker.trackCause("bob")

        assertTrue(tracker.consumeCause("alice"))
        assertFalse(tracker.consumeCause("alice"))
        assertTrue(tracker.consumeCause("bob"))
    }
}
