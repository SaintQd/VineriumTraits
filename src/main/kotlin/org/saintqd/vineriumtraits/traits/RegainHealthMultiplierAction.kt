package org.saintqd.vineriumtraits.traits

import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitManager

@VinTraitType("regain_health_multiplier")
class RegainHealthMultiplierAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    var coef = config.getDouble("Coef",1.0).toFloat()
    val reasons = config.getStringList("Reasons")

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

        private val actionsMap = hashMapOf<String, RegainHealthMultiplierAction>()
        private val listener = object : Listener {

            @EventHandler
            fun onEntityExhaustion(event: EntityRegainHealthEvent) {
                val traitOwner = TraitManager.instance.traitOwners[event.entity.uniqueId] ?: return
                val commonTraits = traitOwner.traits intersect actionsMap.keys
                if (commonTraits.isEmpty()) return
                for (traitName in commonTraits) {
                    actionsMap[traitName]?.let { action ->
                        if (action.reasons.isNotEmpty() && event.regainReason.name !in action.reasons)
                            continue
                        if (action.coef <= 0.0)
                            event.isCancelled = true
                        event.amount *= action.coef
                    }
                }
            }
        }
    }
}