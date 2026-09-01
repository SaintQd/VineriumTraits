package org.saintqd.vineriumtraits.traits

import com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitManager

@VinTraitType("experience_multiplier")
class ExperienceMultiplierAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    var coef = config.getDouble("Coef",1.0)

    override fun register() {
        if (actionsMap.isEmpty())
            Bukkit.getServer().pluginManager.registerEvents(listener, VineriumTraits.inst())
        actionsMap[traitName] = this
    }

    override fun unregister() {
        actionsMap.remove(traitName)
        if (actionsMap.isEmpty())
            HandlerList.unregisterAll(listener)
    }

    companion object {

        private val actionsMap = hashMapOf<String, ExperienceMultiplierAction>()
        private val listener = object : Listener {

            @EventHandler
            fun onEntityExhaustion(event: PlayerPickupExperienceEvent) {
                val traitOwner = TraitManager.instance.traitOwners[event.player.uniqueId] ?: return
                val commonTraits = traitOwner.traits intersect actionsMap.keys
                if (commonTraits.isEmpty()) return
                for (traitName in commonTraits) {
                    actionsMap[traitName]?.let { action ->
                        var exp = event.experienceOrb.experience.toDouble()
                        exp *= action.coef
                        event.experienceOrb.experience = exp.toInt()
                    }
                }
            }
        }
    }
}