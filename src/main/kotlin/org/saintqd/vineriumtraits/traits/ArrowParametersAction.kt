package org.saintqd.vineriumtraits.traits

import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.util.Vector
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitManager

@VinTraitType("arrow_parameters")
class ArrowParametersAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    var divergence = config.getDouble("Divergence",0.0).toFloat()
    var forceCoef = config.getDouble("ForceCoef",1.0)

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
        const val NAME = "arrow_parameters"

        private val actionsMap = hashMapOf<String, ArrowParametersAction>()

        private val listener = object : Listener {

            @EventHandler
            fun onBowShoot(event : EntityShootBowEvent) {
                val traitOwner = TraitManager.instance.traitOwners[event.entity.uniqueId] ?: return
                val commonTraits = traitOwner.traits intersect actionsMap.keys
                if (commonTraits.isEmpty()) return
                for (traitName in commonTraits) {
                    actionsMap[traitName]?.let { action ->
                        if (TraitManager.instance.executeAction(traitName, traitOwner)) {
                            val projectile = event.projectile as Projectile
                            val offsetX = (Math.random() - 0.5) * action.divergence
                            val offsetY = (Math.random() - 0.5) * action.divergence
                            val offsetZ = (Math.random() - 0.5) * action.divergence
                            projectile.velocity = projectile.velocity.add(Vector(offsetX, offsetY, offsetZ))
                            if (action.forceCoef != 1.0) {
                                projectile.velocity = projectile.velocity.multiply(action.forceCoef)
                            }
                        }
                    }
                }
            }
        }
    }
}