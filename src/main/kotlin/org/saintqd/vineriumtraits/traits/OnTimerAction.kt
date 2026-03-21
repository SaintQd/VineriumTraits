package org.saintqd.vineriumtraits.traits

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.managers.TraitManager

class OnTimerAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    private val period = config.getInt("Period",20)
    private var actionName = config.getString("Action","none")!!

    override var executeFunction : (TraitOwner) -> Boolean = Function@ { traitOwner ->
        if (canExecute(traitOwner)) {
            return@Function TraitManager.instance.executeAction(actionName, traitOwner)
        }
        return@Function false
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
        const val NAME = "on_timer"

        private val actionsMap = hashMapOf<String, OnTimerAction>()
        private val listener = object : Listener {
            @EventHandler
            fun onServerTickEnd(event : ServerTickEndEvent) {
                for (action in actionsMap.values) {
                    if (event.tickNumber % action.period == 0) {
                        val eligibleTraitOwners = TraitManager.instance.traitOwners.filter { entry -> entry.value.traits.contains(action.traitName) }
                        eligibleTraitOwners.forEach { entry ->
                            TraitManager.instance.executeAction(action.traitName,entry.value)
                        }
                    }
                }
            }
        }
    }
}