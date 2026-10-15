package net.benwoodworth.fastcraft

import net.benwoodworth.fastcraft.api.FastCraftPreferences
import net.benwoodworth.fastcraft.crafting.FastCraftGui
import net.benwoodworth.fastcraft.platform.command.FcCommandSource
import net.benwoodworth.fastcraft.platform.player.FcPlayer
import net.benwoodworth.fastcraft.platform.server.FcPermission
import net.benwoodworth.fastcraft.platform.text.FcText
import javax.inject.Inject

class FastCraftCommand @Inject constructor(
    private val fcTextFactory: FcText.Factory,
    private val fcPlayerProvider: FcPlayer.Provider,
    private val fastCraftPreferences: FastCraftPreferences,
    private val fastCraftGuiFactory: FastCraftGui.Factory,
    private val permissions: Permissions,
    private val fastCraftConfig: FastCraftConfig,
    fcPlayerOperations: FcPlayer.Operations,
) : FcPlayer.Operations by fcPlayerOperations {
    private fun FcCommandSource.sendMissingPermissionMessage(permission: FcPermission) {
        sendMessage(
            fcTextFactory.createMiniMessage(
                Strings.commandErrorPermission(locale),
                mapOf("permission" to permission.name)
            )
        )
    }

    private fun FcCommandSource.sendMustBePlayerMessage() {
        sendMessage(fcTextFactory.createMiniMessage(Strings.commandErrorPlayerOnly(locale)))
    }

    private fun FcCommandSource.sendPlayerNotFoundMessage(player: String) {
        sendMessage(
            fcTextFactory.createMiniMessage(
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

        if (!source.hasPermission(permissions.FASTCRAFT_COMMAND_SET_ENABLED)) {
            source.sendMissingPermissionMessage(permissions.FASTCRAFT_COMMAND_SET_ENABLED)
            return false
        }

        val newEnabled = if (toggle) {
            !fastCraftPreferences.getEnabledOrDefault(targetPlayer.uuid)
        } else {
            enabled
        }

        fastCraftPreferences.setEnabled(targetPlayer.uuid, newEnabled)
        source.sendMessage(
            fcTextFactory.createMiniMessage(
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
        val targetPlayer = fcPlayerProvider
            .getOnlinePlayers()
            .firstOrNull { it.username.equals(player, true) }

        if (!source.hasPermission(permissions.FASTCRAFT_ADMIN_COMMAND_SET_ENABLED)) {
            source.sendMissingPermissionMessage(permissions.FASTCRAFT_ADMIN_COMMAND_SET_ENABLED)
            return false
        }

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
            fcTextFactory.createMiniMessage(
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

        val hasFcPerm = source.hasPermission(permissions.FASTCRAFT_COMMAND_CRAFT_FASTCRAFT)
        val hasGridPerm = source.hasPermission(permissions.FASTCRAFT_COMMAND_CRAFT_GRID)
        val hasUsePerm = targetPlayer.hasPermission(permissions.FASTCRAFT_USE)
        val prefersFc = fastCraftPreferences.getEnabledOrDefault(targetPlayer.uuid)

        // The default command must respect the player's preference. A player who can
        // run the FastCraft command may still use the vanilla grid after opting out.
        val craftType = when (type) {
            "default" -> if (prefersFc && hasFcPerm && hasUsePerm) "fastcraft" else "grid"
            else -> type
        }

        when (craftType) {
            "fastcraft" -> {
                if (!hasFcPerm) {
                    source.sendMissingPermissionMessage(permissions.FASTCRAFT_COMMAND_CRAFT_FASTCRAFT)
                    return false
                }
                if (!hasUsePerm) {
                    source.sendMissingPermissionMessage(permissions.FASTCRAFT_USE)
                    return false
                }

                fastCraftGuiFactory
                    .createFastCraftGui(targetPlayer)
                    .open()
            }

            "grid" -> {
                if (!hasGridPerm && !(type == "default" && hasFcPerm)) {
                    source.sendMissingPermissionMessage(permissions.FASTCRAFT_COMMAND_CRAFT_GRID)
                    return false
                }

                targetPlayer.openCraftingTable()
            }

            else -> {
                throw IllegalStateException()
            }
        }
        return true
    }

    fun fcCraftAdmin(source: FcCommandSource, type: String, player: String): Boolean {
        val targetPlayer = fcPlayerProvider
            .getOnlinePlayers()
            .firstOrNull { it.username.equals(player, true) }

        if (!source.hasPermission(permissions.FASTCRAFT_ADMIN_COMMAND_CRAFT)) {
            source.sendMissingPermissionMessage(permissions.FASTCRAFT_ADMIN_COMMAND_CRAFT)
            return false
        }

        if (targetPlayer == null) {
            source.sendPlayerNotFoundMessage(player)
            return false
        }

        val craftType = when (type) {
            "default" -> if (fastCraftPreferences.getEnabledOrDefault(targetPlayer.uuid) &&
                targetPlayer.hasPermission(permissions.FASTCRAFT_USE)
            ) "fastcraft" else "grid"

            else -> type
        }

        when (craftType) {
            "fastcraft" -> {
                if (!targetPlayer.hasPermission(permissions.FASTCRAFT_USE)) {
                    source.sendMissingPermissionMessage(permissions.FASTCRAFT_USE)
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
        if (!source.hasPermission(permissions.FASTCRAFT_ADMIN_COMMAND_RELOAD)) {
            source.sendMissingPermissionMessage(permissions.FASTCRAFT_ADMIN_COMMAND_RELOAD)
            return false
        }

        fastCraftConfig.load()
        source.sendMessage(fcTextFactory.createMiniMessage(Strings.commandReloadReloaded(source.locale)))
        return true
    }
}
