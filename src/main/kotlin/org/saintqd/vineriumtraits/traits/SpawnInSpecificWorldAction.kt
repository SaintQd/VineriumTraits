package org.saintqd.vineriumtraits.traits

import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerRespawnEvent
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitManager

@VinTraitType("spawn_in_specific_world")
class SpawnInSpecificWorldAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    val worldName = config.getString("WorldName","world")!!

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
        const val NAME = "spawn_in_specific_world"
        private val actionsMap = hashMapOf<String, SpawnInSpecificWorldAction>()

        private val listener = object : Listener {

            @EventHandler
            fun onPlayerRespawn(event: PlayerRespawnEvent) {
                if (event.isBedSpawn || event.isAnchorSpawn) return
                val traitOwner = TraitManager.instance.traitOwners[event.player.uniqueId] ?: return
                val commonTraits = traitOwner.traits intersect actionsMap.keys
                for (traitName in commonTraits) {
                    actionsMap[traitName]?.let { action ->
                        if (TraitManager.instance.executeAction(traitName, traitOwner)) {
                            val world = Bukkit.getWorld(action.worldName) ?: continue
                            event.respawnLocation = world.spawnLocation
                        }
                    }
                }
            }
        }
    }
}