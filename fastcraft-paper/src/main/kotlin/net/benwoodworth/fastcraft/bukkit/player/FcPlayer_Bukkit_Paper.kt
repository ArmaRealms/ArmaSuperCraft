package net.benwoodworth.fastcraft.bukkit.player

import org.bukkit.inventory.MenuType
import net.benwoodworth.fastcraft.bukkit.server.permission
import net.benwoodworth.fastcraft.bukkit.text.toComponent
import net.benwoodworth.fastcraft.bukkit.world.FcItemStack_Bukkit
import net.benwoodworth.fastcraft.bukkit.world.bukkit
import net.benwoodworth.fastcraft.platform.player.FcPlayer
import net.benwoodworth.fastcraft.platform.player.FcPlayerInventory
import net.benwoodworth.fastcraft.platform.player.FcSound
import net.benwoodworth.fastcraft.platform.server.FcPermission
import net.benwoodworth.fastcraft.platform.text.FcText
import net.benwoodworth.fastcraft.platform.text.FcTextConverter
import net.benwoodworth.fastcraft.platform.world.FcItemStack
import org.bukkit.Server
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

object FcPlayer_Bukkit_Paper {
    @Singleton
    class Operations @Inject constructor(
        private val fcTextConverter: FcTextConverter,
        private val fcPlayerInventoryFactory: FcPlayerInventory_Bukkit_Paper.Factory,
        fcSoundOperations: FcSound.Operations,
        fcItemStackOperations: FcItemStack.Operations,
    ) : FcPlayer_Bukkit.Operations,
        FcSound_Bukkit.Operations by fcSoundOperations.bukkit,
        FcItemStack_Bukkit.Operations by fcItemStackOperations.bukkit {

        override val FcPlayer.player: Player
            get() = value as Player

        override val FcPlayer.username: String
            get() = player.name

        override val FcPlayer.uuid: UUID
            get() = player.uniqueId

        override val FcPlayer.locale: Locale
            get() = player.locale()

        override val FcPlayer.isOnline: Boolean
            get() = player.isOnline

        override val FcPlayer.inventory: FcPlayerInventory
            get() = fcPlayerInventoryFactory.create(player.inventory)

        override fun FcPlayer.sendMessage(message: FcText) {
            player.sendMessage(fcTextConverter.toComponent(message))
        }

        override fun FcPlayer.hasPermission(permission: FcPermission): Boolean {
            return player.hasPermission(permission.permission)
        }

        override fun FcPlayer.giveItems(items: List<FcItemStack>, dropAll: Boolean) {
            fun ItemStack.drop() {
                player.world.dropItemNaturally(player.location, this)
            }

            items.forEach { itemStack ->
                if (dropAll) {
                    itemStack.itemStack.clone().drop()
                } else {
                    val notAdded = player.inventory.addItem(itemStack.itemStack).values
                    notAdded.forEach { it.drop() }
                }
            }
        }

        override fun FcPlayer.openCraftingTable() {
            player.openInventory(MenuType.CRAFTING.create(player))
        }

        override fun FcPlayer.playSound(sound: FcSound, volume: Double, pitch: Double) {
            player.playSound(
                player.location,
                sound.sound,
                volume.toFloat(),
                pitch.toFloat(),
            )
        }

        override fun FcPlayer.executeCommand(command: String) {
            player.performCommand(command)
        }
    }

    @Singleton
    class Provider @Inject constructor(
        private val server: Server,
    ) : FcPlayer_Bukkit.Provider {
        override fun getOnlinePlayers(): List<FcPlayer> {
            return server.onlinePlayers.map { player ->
                getPlayer(player)
            }
        }

        override fun getPlayer(uuid: UUID): FcPlayer? {
            return server.getPlayer(uuid)?.let { player ->
                getPlayer(player)
            }
        }

        override fun getPlayer(player: Player): FcPlayer {
            return FcPlayer(player)
        }
    }
}
