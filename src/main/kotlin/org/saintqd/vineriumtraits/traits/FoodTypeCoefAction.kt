package org.saintqd.vineriumtraits.traits

import io.papermc.paper.datacomponent.DataComponentTypes
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitManager

@VinTraitType("food_type_coef")
class FoodTypeCoefAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    val anyValue : Pair<Double, Double>?
    var foodList = hashMapOf<Material, Pair<Double, Double>>()
    var keyList = hashMapOf<NamespacedKey, Pair<Double, Double>>()

    init {
        var possibleAnyValue : Pair<Double, Double>? = null
        try {
            config.getConfigurationSection("FoodList")?.getKeys(false)?.forEach { materialName ->
                val foodData = config.getString("FoodList.$materialName")?.split(",") ?: return@forEach
                if (materialName == "ANY") {
                    possibleAnyValue = Pair(foodData[0].toDouble(), foodData[1].toDouble())
                    return@forEach
                }
                val material = Material.valueOf(materialName.uppercase())
                val foodLevel = foodData[0].toDouble()
                val saturation = foodData[1].toDouble()
                foodList[material] = Pair(foodLevel, saturation)
            }
            config.getConfigurationSection("KeyList")?.getKeys(false)?.forEach { namespacedKeyString ->
                val foodData = config.getString("KeyList.$namespacedKeyString")?.split(",") ?: return@forEach
                val namespacedKey = NamespacedKey.fromString(namespacedKeyString)!!
                val foodLevel = foodData[0].toDouble()
                val saturation = foodData[1].toDouble()
                keyList[namespacedKey] = Pair(foodLevel, saturation)
            }
        }
        catch (e : NumberFormatException) {
            VineriumTraits.inst().logger.warning { "FoodTypeChangeAction: wrong food/saturation value! ${e.message}" }
        }
        anyValue = possibleAnyValue
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
        const val NAME = "food_type_coef"

        private val actionsMap = hashMapOf<String, FoodTypeCoefAction>()
        private val listener = object : Listener {
            @EventHandler
            fun onFoodLevelChange(event : FoodLevelChangeEvent) {
                event.item?.let { item ->
                    val traitOwner = TraitManager.instance.traitOwners[event.entity.uniqueId] ?: return
                    val commonTraits = traitOwner.traits intersect actionsMap.keys
                    if (commonTraits.isEmpty()) return
                    for (traitName in commonTraits) {
                        actionsMap[traitName]?.let { action ->
                            val foodList = action.foodList
                            var possiblePair = foodList[item.type]
                            if (possiblePair == null) {
                                for (key in item.persistentDataContainer.keys) {
                                    if (action.keyList.contains(key)) {
                                        possiblePair = action.keyList[key]
                                    }
                                }
                            }
                            if (possiblePair == null) {
                                possiblePair = action.anyValue
                            }
                            if (possiblePair == null)
                                continue
                            if (item.hasData(DataComponentTypes.FOOD)) {
                                val foodProperties = item.getData(DataComponentTypes.FOOD) ?: continue
                                var nutrition = foodProperties.nutrition()
                                var saturation = foodProperties.saturation()

                                var newFoodLevel: Int = event.entity.foodLevel - nutrition
                                var newSaturation: Float = event.entity.saturation - saturation
                                nutrition = (nutrition * possiblePair.first).toInt()
                                saturation = (saturation * possiblePair.second).toFloat()

                                newFoodLevel += nutrition
                                newSaturation += saturation
                                newFoodLevel = newFoodLevel.coerceIn(0,20)
                                newSaturation = newSaturation.coerceIn(0f,20f)

                                event.entity.foodLevel = newFoodLevel
                                event.entity.saturation = newSaturation
                            }
                        }
                    }
                }
            }
        }
    }
}