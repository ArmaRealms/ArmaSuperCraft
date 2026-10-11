package net.benwoodworth.fastcraft.bukkit.player

import net.benwoodworth.fastcraft.platform.player.FcSound
import org.bukkit.Sound
import javax.inject.Inject
import javax.inject.Singleton

object FcSoundOperations_Paper {
    @Singleton
    class Operations @Inject constructor() : FcSound_Bukkit.Operations {
        override val FcSound.sound: Sound
            get() = value as Sound
    }
}
