package org.saintqd.vineriumtraits.traits

import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityExhaustionEvent
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitManager

@VinTraitType("exhaustion_multiplier")
class ExhaustionMultiplierAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

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
        const val NAME = "exhaustion_multiplier"

        private val actionsMap = hashMapOf<String, ExhaustionMultiplierAction>()
        private val listener = object : Listener {

            @EventHandler
            fun onEntityExhaustion(event: EntityExhaustionEvent) {
                val traitOwner = TraitManager.instance.traitOwners[event.entity.uniqueId] ?: return
                val commonTraits = traitOwner.traits intersect actionsMap.keys
                if (commonTraits.isEmpty()) return
                for (traitName in commonTraits) {
                    actionsMap[traitName]?.let { action ->
                        if (action.reasons.isNotEmpty() && event.exhaustionReason.name !in action.reasons)
                            continue
                        if (action.coef <= 0.0)
                            event.isCancelled = true
                        event.exhaustion *= action.coef

                    }
                }
            }
        }
    }
}