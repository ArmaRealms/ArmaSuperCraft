package net.benwoodworth.fastcraft

import net.benwoodworth.fastcraft.api.FastCraftPreferences
import net.benwoodworth.fastcraft.crafting.FastCraftGui
import net.benwoodworth.fastcraft.platform.command.FcCommandSource
import net.benwoodworth.fastcraft.platform.player.FcPlayer
import net.benwoodworth.fastcraft.platform.server.FcPermission
import net.benwoodworth.fastcraft.text.miniMessage
import javax.inject.Inject

class FastCraftCommand @Inject constructor(
    private val fcPlayerProvider: FcPlayer.Provider,
    private val fastCraftPreferences: FastCraftPreferences,
    private val fastCraftGuiFactory: FastCraftGui.Factory,
    private val permissions: Permissions,
    private val fastCraftConfig: FastCraftConfig,
    fcPlayerOperations: FcPlayer.Operations,
) : FcPlayer.Operations by fcPlayerOperations {
    private fun FcCommandSource.sendMissingPermissionMessage(permission: FcPermission) {
        sendMessage(
            miniMessage(
                Strings.commandErrorPermission(locale),
                mapOf("permission" to permission.name)
            )
        )
    }

    private fun FcCommandSource.sendMustBePlayerMessage() {
        sendMessage(miniMessage(Strings.commandErrorPlayerOnly(locale)))
    }

    private fun FcCommandSource.sendPlayerNotFoundMessage(player: String) {
        sendMessage(
            miniMessage(
                Strings.commandErrorPlayerUnknown(locale),
                mapOf("player" to player)
            )
        )
    }

    fun fcSetEnabled(source: FcCommandSource, enabled: Boolean = false, toggle: Boolean = false): Boolean {
        val targetPlayer = source.player

        if (targetPlayer == null) {
            source.sendMustBePlayerMessage()
            return false
        }

        if (!source.hasPermission(permissions.CRAFT_USE)) {
            source.sendMissingPermissionMessage(permissions.CRAFT_USE)
            return false
        }

        val newEnabled = if (toggle) {
            !fastCraftPreferences.getEnabledOrDefault(targetPlayer.uuid)
        } else {
            enabled
        }

        fastCraftPreferences.setEnabled(targetPlayer.uuid, newEnabled)
        source.sendMessage(
            miniMessage(
                if (newEnabled) {
                    Strings.commandSetEnabledTrue(source.locale)
                } else {
                    Strings.commandSetEnabledFalse(source.locale)
                }
            )
        )
        return true
    }

    fun fcSetEnabledAdmin(source: FcCommandSource, player: String, enabled: Boolean = false, toggle: Boolean = false): Boolean {
        if (!source.hasPermission(permissions.ADMIN)) {
            source.sendMissingPermissionMessage(permissions.ADMIN)
            return false
        }

        val targetPlayer = fcPlayerProvider
            .getOnlinePlayers()
            .firstOrNull { it.username.equals(player, true) }
        if (targetPlayer == null) {
            source.sendPlayerNotFoundMessage(player)
            return false
        }

        val newEnabled = if (toggle) {
            !fastCraftPreferences.getEnabledOrDefault(targetPlayer.uuid)
        } else {
            enabled
        }

        fastCraftPreferences.setEnabled(targetPlayer.uuid, newEnabled)
        source.sendMessage(
            miniMessage(
                if (newEnabled) {
                    Strings.commandSetEnabledTruePlayer(source.locale)
                } else {
                    Strings.commandSetEnabledFalsePlayer(source.locale)
                },
                mapOf("player" to targetPlayer.username),
            )
        )
        return true
    }

    fun fcCraft(source: FcCommandSource, type: String): Boolean {
        val targetPlayer = source.player

        if (targetPlayer == null) {
            source.sendMustBePlayerMessage()
            return false
        }

        if (!source.hasPermission(permissions.CRAFT_USE)) {
            source.sendMissingPermissionMessage(permissions.CRAFT_USE)
            return false
        }

        val hasUsePermission = targetPlayer.hasPermission(permissions.CRAFT_USE)
        val prefersFastCraft = fastCraftPreferences.getEnabledOrDefault(targetPlayer.uuid)
        val craftType = when (type) {
            "default" -> if (prefersFastCraft && hasUsePermission) "fastcraft" else "grid"
            else -> type
        }

        when (craftType) {
            "fastcraft" -> {
                if (!hasUsePermission) {
                    source.sendMissingPermissionMessage(permissions.CRAFT_USE)
                    return false
                }

                fastCraftGuiFactory.createFastCraftGui(targetPlayer).open()
            }

            "grid" -> targetPlayer.openCraftingTable()

            else -> throw IllegalStateException()
        }
        return true
    }

    fun fcCraftAdmin(source: FcCommandSource, type: String, player: String): Boolean {
        if (!source.hasPermission(permissions.ADMIN)) {
            source.sendMissingPermissionMessage(permissions.ADMIN)
            return false
        }

        val targetPlayer = fcPlayerProvider
            .getOnlinePlayers()
            .firstOrNull { it.username.equals(player, true) }
        if (targetPlayer == null) {
            source.sendPlayerNotFoundMessage(player)
            return false
        }

        val craftType = when (type) {
            "default" -> if (fastCraftPreferences.getEnabledOrDefault(targetPlayer.uuid) &&
                targetPlayer.hasPermission(permissions.CRAFT_USE)
            ) "fastcraft" else "grid"

            else -> type
        }

        when (craftType) {
            "fastcraft" -> {
                if (!targetPlayer.hasPermission(permissions.CRAFT_USE)) {
                    source.sendMissingPermissionMessage(permissions.CRAFT_USE)
                    return false
                }

                fastCraftGuiFactory
                    .createFastCraftGui(targetPlayer)
                    .open()
            }

            "grid" -> {
                targetPlayer.openCraftingTable()
            }

            else -> {
                throw IllegalStateException()
            }
        }
        return true
    }

    fun fcReload(source: FcCommandSource): Boolean {
        if (!source.hasPermission(permissions.ADMIN)) {
            source.sendMissingPermissionMessage(permissions.ADMIN)
            return false
        }

        if (!fastCraftConfig.load()) {
            source.sendMessage(miniMessage(Strings.commandReloadFailed(source.locale)))
            return false
        }

        source.sendMessage(miniMessage(Strings.commandReloadReloaded(source.locale)))
        return true
    }
}
