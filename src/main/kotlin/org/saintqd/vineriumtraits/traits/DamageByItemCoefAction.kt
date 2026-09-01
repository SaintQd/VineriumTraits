package org.saintqd.vineriumtraits.traits

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitManager

@VinTraitType("damage_by_item_coef")
class DamageByItemCoefAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    var items = hashMapOf<Material, Double>()
    val preventItemDamage = config.getBoolean("PreventItemDamage",false)

    init {
        config.getConfigurationSection("Items")?.getKeys(false)?.forEach { materialName ->
            val type = Material.valueOf(materialName.uppercase())
            val coef = config.getDouble("Items.$materialName",1.0)
            items[type] = coef
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
        const val NAME = "damage_by_item_coef"

        private val actionsMap = hashMapOf<String, DamageByItemCoefAction>()
        private val listener = object : Listener {

            @EventHandler
            fun onEntityDamage(event : EntityDamageByEntityEvent) {
                if (event.damager !is Player) return
                val damager = event.damager as Player
                val traitOwner = TraitManager.instance.traitOwners[event.damager.uniqueId] ?: return
                val commonTraits = traitOwner.traits intersect actionsMap.keys
                if (commonTraits.isEmpty()) return
                for (traitName in commonTraits) {
                    actionsMap[traitName]?.let { action ->
                        action.items[damager.inventory.itemInMainHand.type]?.let { coef ->
                            if (TraitManager.instance.executeAction(traitName, traitOwner)) {
                                event.damage *= coef
                                if (event.damage <= 0)
                                    event.isCancelled = true
                            }
                        }
                    }
                }
            }

            @EventHandler
            fun onItemDamage(event : PlayerItemDamageEvent) {
                val traitOwner = TraitManager.instance.traitOwners[event.player.uniqueId] ?: return
                val commonTraits = traitOwner.traits intersect actionsMap.keys
                if (commonTraits.isEmpty()) return
                for (traitName in commonTraits) {
                    actionsMap[traitName]?.let { action ->
                        if (action.items.containsKey(event.item.type) && action.preventItemDamage)
                            event.isCancelled = true
                    }
                }
            }
        }
    }
}