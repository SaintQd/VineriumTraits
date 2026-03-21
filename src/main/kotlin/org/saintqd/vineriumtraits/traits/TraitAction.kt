package org.saintqd.vineriumtraits.traits

import org.bukkit.configuration.ConfigurationSection
import org.saintqd.vineriumlib.utils.VinUtils

abstract class TraitAction(val traitName: String, config : ConfigurationSection) {

    var checkIfPresent = true
    var cooldown = 0

    open var executeFunction : (TraitOwner) -> Boolean = Function@ {
        return@Function true
    }

    init {
        checkIfPresent = config.getBoolean("CheckIfPresent",true)
        cooldown = config.getInt("Cooldown",0)
    }

    fun canExecute(owner: TraitOwner): Boolean {
        owner.cooldowns[traitName]?.let { cooldown ->
            if (cooldown > VinUtils.getCurrentTick())
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
}