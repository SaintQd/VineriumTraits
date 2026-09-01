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
import org.saintqd.vineriumlib.utils.MMAbilityData
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitOwner
import java.util.UUID
import kotlin.collections.set

@VinTraitType("mm_skill_on_food_consume")
class MMSkillOnFoodConsumeAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    private val items = config.getStringList("Items").map { foodName -> Material.valueOf(foodName.uppercase()) }
    private var skillName = config.getString("SkillName","vintrait_$name")!!

    override var executeFunction : (TraitOwner) -> Boolean = Function@ { traitOwner ->
        if (canExecute(traitOwner) && skillName.isNotEmpty()) {
            val skillMetadata = MMAbilityData.prepareMMSkillData(traitOwner.player)
            consumedItems[traitOwner.player.uniqueId]?.let { material ->
                skillMetadata.variables.putString("consumed_material", material.name)
            }
            return@Function MMAbilityData.executeMMSkill(skillName,skillMetadata)
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
        const val NAME = "mm_skill_on_food_consume"
        private val actionsMap = hashMapOf<String, MMSkillOnFoodConsumeAction>()
        private val consumedItems = hashMapOf<UUID, Material>()
        private val listener = object : Listener {

            @EventHandler
            fun onFoodLevelChange(event : FoodLevelChangeEvent) {
                event.item?.let {
                    val traitOwner = TraitManager.instance.traitOwners[event.entity.uniqueId] ?: return
                    val commonTraits = traitOwner.traits intersect actionsMap.keys
                    if (commonTraits.isEmpty()) return
                    for (traitName in commonTraits) {
                        actionsMap[traitName]?.let { action ->
                            event.item?.let { item ->
                                if (item.type in action.items && TraitManager.instance.executeAction(traitName, traitOwner)) {
                                    consumedItems[event.entity.uniqueId] = item.type
                                    TraitManager.instance.executeAction(traitName, traitOwner)
                                    consumedItems.remove(event.entity.uniqueId)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}