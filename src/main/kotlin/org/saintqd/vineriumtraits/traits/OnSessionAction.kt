package org.saintqd.vineriumtraits.traits

import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.events.TraitOwnerJoinEvent
import org.saintqd.vineriumtraits.events.TraitOwnerQuitEvent
import org.saintqd.vineriumtraits.managers.TraitManager
import org.saintqd.vineriumtraits.managers.TraitOwner

@VinTraitType("on_session")
class OnSessionAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    private val sessionActionType = SessionAction.valueOf(config.getString("SessionType","JOIN")!!.uppercase())
    private var triggeredActionName = config.getString("Action","none")!!

    override var executeFunction : (TraitOwner) -> Boolean = Function@ { traitOwner ->
        if (canExecute(traitOwner)) {
            return@Function TraitManager.instance.executeAction(triggeredActionName, traitOwner)
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
        const val NAME = "on_session"

        private val actionsMap = hashMapOf<String, OnSessionAction>()
        private val listener = object : Listener {
            @EventHandler
            fun onTraitOwnerJoin(event : TraitOwnerJoinEvent) {
                val traitOwner = TraitManager.instance.traitOwners[event.traitOwner.player.uniqueId] ?: return
                for (actionName in actionsMap.keys) {
                    actionsMap[actionName]?.let { action ->
                        if (action.sessionActionType == SessionAction.JOIN)
                            TraitManager.instance.executeAction(actionName,traitOwner)
                    }
                }
            }

            @EventHandler
            fun onTraitOwnerLeave(event : TraitOwnerQuitEvent) {
                val traitOwner = TraitManager.instance.traitOwners[event.traitOwner.player.uniqueId] ?: return
                for (actionName in actionsMap.keys) {
                    actionsMap[actionName]?.let { action ->
                        if (action.sessionActionType == SessionAction.QUIT)
                            TraitManager.instance.executeAction(actionName,traitOwner)
                    }
                }
            }
        }
    }

    enum class SessionAction {
        JOIN,
        QUIT
    }
}