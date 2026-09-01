package org.saintqd.vineriumtraits.traits

import com.destroystokyo.paper.event.entity.EntityPathfindEvent
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Fox
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitManager

@VinTraitType("fox_friend")
class FoxFriendAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

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
        const val NAME = "fox_friend"
        private val actionsMap = hashMapOf<String, FoxFriendAction>()

        private val listener = object : Listener {

            @EventHandler
            fun onEntityPathfind(event: EntityPathfindEvent) {
                if (event.entity is Fox) {
                    val fox = event.entity as Fox
                    if (fox.isLeashed) return
                    for (possiblePlayer in event.entity.getNearbyEntities(7.0, 7.0, 7.0)) {
                        if (possiblePlayer is Player) {
                            val traitOwner = TraitManager.instance.traitOwners[possiblePlayer.uniqueId] ?: return
                            val commonTraits = traitOwner.traits intersect actionsMap.keys
                            if (commonTraits.isNotEmpty())
                                event.isCancelled = true
                        }
                    }
                }
            }
        }
    }
}