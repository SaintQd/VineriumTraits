package org.saintqd.vineriumtraits.traits

import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.inventory.EquipmentSlot
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.managers.TraitManager

class OnInteractAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    private val interactionType = InteractionType.valueOf(config.getString("InteractionType","RIGHT_CLICK")!!.uppercase())
    private val requiresSneaking = config.getBoolean("Sneaking",false)
    private var interactableActionName = config.getString("Action","none")!!

    override var executeFunction : (TraitOwner) -> Boolean = Function@ { traitOwner ->
        if (canExecute(traitOwner)) {
            return@Function TraitManager.instance.executeAction(interactableActionName, traitOwner)
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
        const val NAME = "on_interact"
        private val actionsMap = hashMapOf<String, OnInteractAction>()
        private val listener = object : Listener {

            @EventHandler
            fun onPlayerSwapHands(event : PlayerSwapHandItemsEvent) {
                val traitOwner = TraitManager.instance.traitOwners[event.player.uniqueId] ?: return
                for (actionName in actionsMap.keys) {
                    val interactionTrait = actionsMap[actionName]
                    if (interactionTrait != null && interactionTrait.interactionType == InteractionType.SWAP_HANDS) {
                        if (interactionTrait.requiresSneaking && !event.player.isSneaking)
                            continue
                        event.isCancelled = true
                        TraitManager.instance.executeAction(actionName,traitOwner)
                    }
                }
            }

            @EventHandler
            fun onPlayerClick(event : PlayerInteractEvent) {
                if (event.hand != EquipmentSlot.HAND)
                    return
                val traitOwner = TraitManager.instance.traitOwners[event.player.uniqueId] ?: return
                for (actionName in actionsMap.keys) {
                    val interactionTrait = actionsMap[actionName]
                    if (interactionTrait != null) {
                        if (interactionTrait.requiresSneaking && !event.player.isSneaking)
                            continue
                        if (event.action.isLeftClick) {
                            if (interactionTrait.interactionType == InteractionType.LEFT_CLICK) {
                                event.isCancelled = true
                                TraitManager.instance.executeAction(actionName,traitOwner)
                            }
                        }
                        else if (event.action.isRightClick) {
                            if (interactionTrait.interactionType == InteractionType.RIGHT_CLICK) {
                                event.isCancelled = true
                                TraitManager.instance.executeAction(actionName,traitOwner)
                            }
                        }
                    }
                }
            }

            @EventHandler
            fun onPlayerItemDrop(event : PlayerDropItemEvent) {
                val traitOwner = TraitManager.instance.traitOwners[event.player.uniqueId] ?: return
                for (actionName in actionsMap.keys) {
                    val interactionTrait = actionsMap[actionName]
                    if (interactionTrait != null && interactionTrait.interactionType == InteractionType.DROP) {
                        if (interactionTrait.requiresSneaking && !event.player.isSneaking)
                            continue
                        event.isCancelled = true
                        TraitManager.instance.executeAction(actionName,traitOwner)
                    }
                }
            }
        }
    }

    enum class InteractionType{
        LEFT_CLICK,
        RIGHT_CLICK,
        DROP,
        SWAP_HANDS
    }

}