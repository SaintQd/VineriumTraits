package org.saintqd.vineriumtraits.traits

import com.destroystokyo.paper.event.block.BeaconEffectEvent
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitManager

@VinTraitType("cancel_beacon_effect_event")
class CancelBeaconEffectEvent(name : String, config : ConfigurationSection) : TraitAction(name,config) {

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

        private val actionsMap = hashMapOf<String, CancelBeaconEffectEvent>()

        private val listener = object : Listener {

            @EventHandler
            fun onBeaconEffect(event : BeaconEffectEvent) {
                val traitOwner = TraitManager.instance.traitOwners[event.player.uniqueId] ?: return
                val commonTraits = traitOwner.traits intersect actionsMap.keys
                if (commonTraits.isNotEmpty())
                    event.isCancelled = true
            }
        }
    }
}