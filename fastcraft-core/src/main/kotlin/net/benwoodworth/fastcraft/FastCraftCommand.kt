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

    fun fcSetEnabled(source: FcCommandSource, enabled: Boolean = false, toggle: Boolean = false) {
        val targetPlayer = source.player

        if (targetPlayer == null) {
            source.sendMustBePlayerMessage()
            return
        }

        if (!source.hasPermission(permissions.FASTCRAFT_COMMAND_SET_ENABLED)) {
            source.sendMissingPermissionMessage(permissions.FASTCRAFT_COMMAND_SET_ENABLED)
            return
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
    }

    fun fcSetEnabledAdmin(source: FcCommandSource, player: String, enabled: Boolean = false, toggle: Boolean = false) {
        val targetPlayer = fcPlayerProvider
            .getOnlinePlayers()
            .firstOrNull { it.username.equals(player, true) }

        if (!source.hasPermission(permissions.FASTCRAFT_ADMIN_COMMAND_SET_ENABLED)) {
            source.sendMissingPermissionMessage(permissions.FASTCRAFT_ADMIN_COMMAND_SET_ENABLED)
            return
        }

        if (targetPlayer == null) {
            source.sendPlayerNotFoundMessage(player)
            return
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
    }

    fun fcCraft(source: FcCommandSource, type: String) {
        val targetPlayer = source.player

        if (targetPlayer == null) {
            source.sendMustBePlayerMessage()
            return
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
                    return
                }
                if (!hasUsePerm) {
                    source.sendMissingPermissionMessage(permissions.FASTCRAFT_USE)
                    return
                }

                fastCraftGuiFactory
                    .createFastCraftGui(targetPlayer)
                    .open()
            }

            "grid" -> {
                if (!hasGridPerm && !(type == "default" && hasFcPerm)) {
                    source.sendMissingPermissionMessage(permissions.FASTCRAFT_COMMAND_CRAFT_GRID)
                    return
                }

                targetPlayer.openCraftingTable()
            }

            else -> {
                throw IllegalStateException()
            }
        }
    }

    fun fcCraftAdmin(source: FcCommandSource, type: String, player: String) {
        val targetPlayer = fcPlayerProvider
            .getOnlinePlayers()
            .firstOrNull { it.username.equals(player, true) }

        if (!source.hasPermission(permissions.FASTCRAFT_ADMIN_COMMAND_CRAFT)) {
            source.sendMissingPermissionMessage(permissions.FASTCRAFT_ADMIN_COMMAND_CRAFT)
            return
        }

        if (targetPlayer == null) {
            source.sendPlayerNotFoundMessage(player)
            return
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
                    return
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
    }

    fun fcReload(source: FcCommandSource) {
        if (!source.hasPermission(permissions.FASTCRAFT_ADMIN_COMMAND_RELOAD)) {
            source.sendMissingPermissionMessage(permissions.FASTCRAFT_ADMIN_COMMAND_RELOAD)
            return
        }

        fastCraftConfig.load()
        source.sendMessage(fcTextFactory.createMiniMessage(Strings.commandReloadReloaded(source.locale)))
    }
}
