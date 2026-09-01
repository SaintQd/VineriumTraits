package org.saintqd.vineriumtraits.traits

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitManager

@VinTraitType("air_from_items")
class AirFromItemsAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    val items = hashMapOf<Material, Int>()

    init {
        config.getConfigurationSection("Items")?.getKeys(false)?.forEach { materialName ->
            val material = Material.valueOf(materialName.uppercase())
            val value = config.getInt("Items.$materialName")
            items[material] = value
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
        const val NAME = "air_from_items"

        private val actionsMap = hashMapOf<String, AirFromItemsAction>()
        private val listener = object : Listener {

            @EventHandler
            fun onItemConsume(event : PlayerItemConsumeEvent) {
                val traitOwner = TraitManager.instance.traitOwners[event.player.uniqueId] ?: return
                val commonTraits = traitOwner.traits intersect actionsMap.keys
                if (commonTraits.isEmpty()) return
                for (traitName in commonTraits) {
                    actionsMap[traitName]?.let { action ->
                        action.items[event.item.type]?.let { value ->
                            event.player.remainingAir = (event.player.remainingAir + value).coerceAtMost(event.player.maximumAir)
                        }
                    }
                }
            }
        }
    }
}