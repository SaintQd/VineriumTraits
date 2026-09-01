package org.saintqd.vineriumtraits.traits

import org.bukkit.OfflinePlayer
import org.bukkit.configuration.ConfigurationSection
import org.saintqd.vineriumlib.utils.VinUtils
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.managers.TraitManager
import org.saintqd.vineriumtraits.managers.TraitOwner
import placeholders.VinTraitsPlaceholders

abstract class TraitAction(val traitName: String, config : ConfigurationSection) {

    var checkIfPresent = true
    var cooldown = 0

    open var executeFunction : (TraitOwner) -> Boolean = Function@ { traitOwner ->
        return@Function canExecute(traitOwner)
    }

    init {
        checkIfPresent = config.getBoolean("CheckIfPresent",true)
        cooldown = config.getInt("Cooldown",0)
    }

    fun canExecute(owner: TraitOwner): Boolean {
        owner.cooldowns[traitName]?.let { cooldown ->
            if (VinUtils.getCurrentTick() < cooldown)
                return false
        }
        return !(checkIfPresent && !owner.traits.contains(traitName))
    }

    fun applyCooldown(traitOwner: TraitOwner) {
        if (cooldown > 0) {
            traitOwner.cooldowns[traitName] = VinUtils.getCurrentTick() + cooldown
        }
    }

    open fun register() {
    }

    open fun unregister() {

    }

    open fun onLoad(owner: TraitOwner) {

    }

    open fun onUnload(owner: TraitOwner) {

    }

    open fun onAdd(owner: TraitOwner) {

    }

    open fun onRemove(owner: TraitOwner) {

    }
}