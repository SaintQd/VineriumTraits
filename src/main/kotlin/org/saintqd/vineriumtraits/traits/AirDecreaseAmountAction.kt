package org.saintqd.vineriumtraits.traits

import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityAirChangeEvent
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitManager

@VinTraitType("air_decrease_amount")
class AirDecreaseAmountAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    val amount = config.getInt("Amount",1)

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
        const val NAME = "air_decrease_amount"

        val lastAirAmountPerPlayer = hashMapOf<Player, Int>()

        private val actionsMap = hashMapOf<String, AirDecreaseAmountAction>()
        private val listener = object : Listener {

            @EventHandler
            fun onAirChange(event : EntityAirChangeEvent) {
                if (event.entity !is Player) return
                val player = event.entity as Player
                val traitOwner = TraitManager.instance.traitOwners[event.entity.uniqueId] ?: return
                val commonTraits = traitOwner.traits intersect actionsMap.keys
                if (commonTraits.isEmpty()) return
                for (traitName in commonTraits) {
                    actionsMap[traitName]?.let { action ->
                        if (TraitManager.instance.executeAction(traitName, traitOwner)) {
                            lastAirAmountPerPlayer[player]?.let { lastAmount ->
                                if (lastAmount >= event.amount) {
                                    event.amount -= action.amount
                                }
                            }
                            lastAirAmountPerPlayer[player] = event.amount
                        }
                    }
                }
            }
        }
    }
}