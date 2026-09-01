package org.saintqd.vineriumtraits.traits

import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitManager

@VinTraitType("damage_cause_coef")
class OnDamageCauseCoefAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    var damageTypes = hashMapOf<EntityDamageEvent.DamageCause, Double>()

    init {
        config.getConfigurationSection("DamageCauses")?.getKeys(false)?.forEach { causeName ->
            val cause = EntityDamageEvent.DamageCause.valueOf(causeName.uppercase())
            val coef = config.getDouble("DamageCauses.$causeName",1.0)
            damageTypes[cause] = coef
        }
    }

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
        const val NAME = "damage_cause_coef"

        private val actionsMap = hashMapOf<String, OnDamageCauseCoefAction>()
        private val listener = object : Listener {

            @EventHandler
            fun onEntityDamage(event : EntityDamageEvent) {
                val traitOwner = TraitManager.instance.traitOwners[event.entity.uniqueId] ?: return
                val commonTraits = traitOwner.traits intersect actionsMap.keys
                if (commonTraits.isEmpty()) return
                for (traitName in commonTraits) {
                    actionsMap[traitName]?.let { action ->
                        action.damageTypes[event.cause]?.let { coef ->
                            val changedDamage = event.damage * coef
                            if (changedDamage <= 0)
                                event.isCancelled = true
                            else
                                event.damage = changedDamage
                        }
                    }
                }
            }
        }
    }
}