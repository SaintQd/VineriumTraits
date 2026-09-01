package org.saintqd.vineriumtraits.traits

import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitManager

@VinTraitType("snowball_damage")
class OnSnowballDamageCoefAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    var damageCoef = config.getDouble("Coef",0.0)

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
        const val NAME = "snowball_damage"

        private val actionsMap = hashMapOf<String, OnSnowballDamageCoefAction>()
        private val listener = object : Listener {

            @EventHandler
            fun onEntityDamage(event : EntityDamageByEntityEvent) {
                if (event.damager !is Snowball) return
                val shooter = event.damageSource.causingEntity
                if (shooter is Player) {
                    val traitOwner = TraitManager.instance.traitOwners[shooter.uniqueId] ?: return
                    val commonTraits = traitOwner.traits intersect actionsMap.keys
                    if (commonTraits.isEmpty()) return
                    for (traitName in commonTraits) {
                        actionsMap[traitName]?.let { action ->
                            if (event.damage <= 0.001)
                                event.damage = 1.0
                            val changedDamage = event.damage * action.damageCoef
                            if (changedDamage <= 0.0)
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