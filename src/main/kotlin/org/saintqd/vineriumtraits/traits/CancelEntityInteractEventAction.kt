package org.saintqd.vineriumtraits.traits

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitManager

@VinTraitType("cancel_entity_interact_event")
class CancelEntityInteractEventAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    var entityList = hashSetOf<EntityType>()

    init {
        config.getStringList("Types").forEach { entityName ->
            val event = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENTITY_TYPE).getOrThrow(Key.key(entityName.lowercase()))
            entityList.add(event)
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
        const val NAME = "cancel_entity_interact_event"

        private val actionsMap = hashMapOf<String, CancelEntityInteractEventAction>()

        private val listener = object : Listener {

            @EventHandler
            fun onEntityInteract(event: PlayerInteractEntityEvent) {
                val traitOwner = TraitManager.instance.traitOwners[event.player.uniqueId] ?: return
                val commonTraits = traitOwner.traits intersect actionsMap.keys
                if (commonTraits.isEmpty()) return
                for (traitName in commonTraits) {
                    actionsMap[traitName]?.let { action ->
                        if (action.entityList.contains(event.rightClicked.type)) {
                            event.isCancelled = true
                        }
                    }
                }
            }
        }
    }
}