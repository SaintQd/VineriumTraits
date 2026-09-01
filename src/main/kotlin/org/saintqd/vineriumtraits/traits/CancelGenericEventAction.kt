package org.saintqd.vineriumtraits.traits

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.GameEvent
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.world.GenericGameEvent
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitManager
import kotlin.collections.set

@VinTraitType("cancel_generic_event")
class CancelGenericEventAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    var eventList = hashSetOf<GameEvent>()

    init {
        config.getStringList("Events").forEach { eventName ->
            val event = RegistryAccess.registryAccess().getRegistry(RegistryKey.GAME_EVENT).getOrThrow(Key.key(eventName.lowercase()))
            eventList.add(event)
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
        const val NAME = "cancel_generic_event"

        private val actionsMap = hashMapOf<String, CancelGenericEventAction>()
        private val listener = object : Listener {

            @EventHandler
            fun onGenericEvent(event: GenericGameEvent) {
                event.entity?.let { entity ->
                    val traitOwner = TraitManager.instance.traitOwners[entity.uniqueId] ?: return
                    val commonTraits = traitOwner.traits intersect actionsMap.keys
                    if (commonTraits.isEmpty()) return
                    for (traitName in commonTraits) {
                        actionsMap[traitName]?.let { action ->
                            if (TraitManager.instance.executeAction(traitName, traitOwner) && action.eventList.contains(event.event)) {
                                event.isCancelled = true
                            }
                        }
                    }
                }
            }
        }
    }
}