package org.saintqd.vineriumtraits.traits

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.managers.TraitManager

class FoodVegetarianAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    init {
        if (config.contains("FoodList")) {
            allowedFoodList = config.getStringList("FoodList").map { foodName -> Material.valueOf(foodName.uppercase()) }
        }
    }

    override var executeFunction : (TraitOwner) -> Boolean = Function@ { _ ->
        return@Function true
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
        const val NAME = "food_vegetarian"

        var allowedFoodList = listOf(
            Material.BEEF,
            Material.COOKED_BEEF,
            Material.PORKCHOP,
            Material.COOKED_BEEF,
            Material.MUTTON,
            Material.COOKED_MUTTON,
            Material.CHICKEN,
            Material.COOKED_CHICKEN,
            Material.RABBIT,
            Material.COOKED_RABBIT,
            Material.COD,
            Material.COOKED_COD,
            Material.SALMON,
            Material.COOKED_SALMON,
            Material.TROPICAL_FISH,
            Material.PUFFERFISH,
            Material.ROTTEN_FLESH,
            Material.SPIDER_EYE,
            Material.RABBIT_STEW
        )

        private val actionsMap = hashMapOf<String, FoodVegetarianAction>()
        private val listener = object : Listener {
            @EventHandler
            fun onFoodLevelChange(event : FoodLevelChangeEvent) {
                event.item?.let {
                    if (!allowedFoodList.contains(it.type)) {
                        val traitOwner = TraitManager.instance.traitOwners[event.entity.uniqueId] ?: return
                        for (action in actionsMap.values) {
                            if (traitOwner.traits.contains(action.traitName)) {
                                event.isCancelled = true
                                return
                            }
                        }
                    }
                }
            }
        }
    }
}