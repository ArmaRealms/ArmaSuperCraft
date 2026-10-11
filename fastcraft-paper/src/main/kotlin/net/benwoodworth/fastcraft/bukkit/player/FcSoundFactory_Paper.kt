package net.benwoodworth.fastcraft.bukkit.player

import net.benwoodworth.fastcraft.platform.player.FcSound
import org.bukkit.Sound
import javax.inject.Inject
import javax.inject.Singleton

object FcSoundFactory_Paper {
    @Singleton
    class Factory @Inject constructor() : FcSound_Bukkit.Factory {
        override val uiButtonClick: FcSound
            get() = fromSound(Sound.UI_BUTTON_CLICK)

        override fun fromSound(sound: Sound): FcSound = FcSound(sound)
    }
}
