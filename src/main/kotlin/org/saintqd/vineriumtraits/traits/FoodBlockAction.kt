package org.saintqd.vineriumtraits.traits

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitManager

@VinTraitType("food_block")
class FoodBlockAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    var foodList = if (config.getStringList("FoodList").isEmpty()) listOf(
        Material.BEEF,
        Material.COOKED_BEEF,
        Material.PORKCHOP,
        Material.COOKED_PORKCHOP,
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
    ) else config.getStringList("FoodList").map { foodName -> Material.valueOf(foodName.uppercase()) }

    var denyList = config.getBoolean("DenyList",false)

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
        const val NAME = "food_block"

        private val actionsMap = hashMapOf<String, FoodBlockAction>()
        private val listener = object : Listener {
            @EventHandler
            fun onFoodLevelChange(event : FoodLevelChangeEvent) {
                event.item?.let {
                    val traitOwner = TraitManager.instance.traitOwners[event.entity.uniqueId] ?: return
                    val commonTraits = traitOwner.traits intersect actionsMap.keys
                    if (commonTraits.isEmpty()) return
                    for (traitName in commonTraits) {
                        actionsMap[traitName]?.let { action ->
                            val foodList = action.foodList
                            if ((!foodList.contains(it.type) && !action.denyList) || (foodList.contains(it.type) && action.denyList)) {
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