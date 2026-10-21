package net.benwoodworth.fastcraft

import net.benwoodworth.fastcraft.platform.server.FcPermission
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Permissions @Inject constructor(
    fcPermissionFactory: FcPermission.Factory,
) {
    val CRAFT_USE = fcPermissionFactory.create(
        name = "armamc.supercraft.craft.use",
        description = "Permission to use SuperCraft and personal '/craft' commands",
    )

    val ADMIN = fcPermissionFactory.create(
        name = "armamc.supercraft.admin",
        description = "Permission to use SuperCraft administrative commands",
    )
}
