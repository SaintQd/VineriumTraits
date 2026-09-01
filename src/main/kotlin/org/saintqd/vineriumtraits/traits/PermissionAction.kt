package org.saintqd.vineriumtraits.traits

import org.bukkit.configuration.ConfigurationSection
import org.saintqd.vineriumlib.VineriumLib
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitOwner

@VinTraitType("permission")
class PermissionAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    private val permissions = config.getStringList("Permissions")

    override fun onAdd(owner: TraitOwner) {
        VineriumLib.inst().vaultManager?.permissionProvider?.let { permissionProvider ->
            permissions.forEach { permission ->
                permissionProvider.playerAdd(null,owner.player,permission)
            }
        }
    }

    override fun onRemove(owner: TraitOwner) {
        VineriumLib.inst().vaultManager?.permissionProvider?.let { permissionProvider ->
            permissions.forEach { permission ->
                permissionProvider.playerRemove(null,owner.player,permission)
            }
        }
    }

    companion object {
        const val NAME = "permission"
    }
}