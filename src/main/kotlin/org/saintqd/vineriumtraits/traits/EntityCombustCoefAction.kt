package org.saintqd.vineriumtraits.traits

import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityCombustEvent
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitManager

@VinTraitType("combust_coef")
class EntityCombustCoefAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    var coef = config.getDouble("Coef",1.0).toFloat()

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

        private val actionsMap = hashMapOf<String, EntityCombustCoefAction>()
        private val listener = object : Listener {

            @EventHandler
            fun onEntityExhaustion(event: EntityCombustEvent) {
                val traitOwner = TraitManager.instance.traitOwners[event.entity.uniqueId] ?: return
                val commonTraits = traitOwner.traits intersect actionsMap.keys
                if (commonTraits.isEmpty()) return
                for (traitName in commonTraits) {
                    actionsMap[traitName]?.let { action ->
                        event.duration *= action.coef
                    }
                }
            }
        }
    }
}