package org.saintqd.vineriumtraits.traits

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.events.TraitOwnerJoinEvent
import org.saintqd.vineriumtraits.managers.TraitManager
import org.saintqd.vineriumtraits.managers.TraitOwner

@VinTraitType("armor_limit")
class ArmorLimitAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    var armorList = if (config.getStringList("ItemList").isEmpty()) listOf(
        Material.DIAMOND_HELMET,
        Material.DIAMOND_CHESTPLATE,
        Material.DIAMOND_LEGGINGS,
        Material.DIAMOND_BOOTS,
    ) else config.getStringList("ItemList").map { materialName -> Material.valueOf(materialName.uppercase()) }

    override var executeFunction : (TraitOwner) -> Boolean = Function@ { traitOwner ->
        if (canExecute(traitOwner)) {
            for (slot in 36..39) {
                traitOwner.player.inventory.getItem(slot)?.let { item ->
                    if (item.type in armorList) {
                        traitOwner.player.location.world.playSound(traitOwner.player.location,
                            Sound.ENTITY_ARMOR_STAND_BREAK,1.0f,1.0f)
                        traitOwner.player.dropItem(slot)
                    }
                }
            }
            return@Function true
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
        const val NAME = "armor_limit"

        private val actionsMap = hashMapOf<String, ArmorLimitAction>()

        private val listener = object : Listener {

            @EventHandler
            fun onTraitOwnerJoin(event : TraitOwnerJoinEvent) {
                val traitOwner = TraitManager.instance.traitOwners[event.traitOwner.player.uniqueId] ?: return
                val commonTraits = traitOwner.traits intersect actionsMap.keys
                if (commonTraits.isNotEmpty()) {
                    for (traitName in commonTraits) {
                        actionsMap[traitName]?.let { action ->
                            TraitManager.instance.executeAction(traitName, traitOwner)
                        }
                    }
                }
            }

            @EventHandler
            fun onArmorEquip(event: PlayerArmorChangeEvent) {
                val traitOwner = TraitManager.instance.traitOwners[event.player.uniqueId] ?: return
                val commonTraits = traitOwner.traits intersect actionsMap.keys
                if (commonTraits.isNotEmpty()) {
                    for (traitName in commonTraits) {
                        actionsMap[traitName]?.let { action ->
                            TraitManager.instance.executeAction(traitName, traitOwner)
                        }
                    }
                }
            }
        }
    }
}