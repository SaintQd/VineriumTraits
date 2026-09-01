package org.saintqd.vineriumtraits.traits

import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityResurrectEvent
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitManager

@VinTraitType("cancel_entity_resurrect_event")
class CancelEntityResurrectEvent(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    val removeTotem = config.getBoolean("RemoveTotem",true)

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
        const val NAME = "cancel_entity_resurrect_event"

        private val actionsMap = hashMapOf<String, CancelEntityResurrectEvent>()

        private val listener = object : Listener {

            @EventHandler
            fun onEntityInteract(event: EntityResurrectEvent) {
                val entity = event.entity
                if (entity !is Player)
                    return
                val traitOwner = TraitManager.instance.traitOwners[event.entity.uniqueId] ?: return
                val commonTraits = traitOwner.traits intersect actionsMap.keys
                if (commonTraits.isEmpty()) return
                event.isCancelled = true
                event.hand?.let { slot ->
                    for (traitName in commonTraits) {
                        actionsMap[traitName]?.let { action ->
                            if (action.removeTotem) {
                                entity.equipment.setItem(slot,null)
                                break
                            }
                        }
                    }
                }
            }
        }
    }
}