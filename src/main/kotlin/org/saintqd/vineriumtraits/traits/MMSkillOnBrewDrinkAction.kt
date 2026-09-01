package org.saintqd.vineriumtraits.traits

import com.dre.brewery.api.BreweryApi
import io.lumine.mythic.bukkit.BukkitAdapter
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.managers.TraitManager
import org.saintqd.vineriumlib.utils.MMAbilityData
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitOwner

@VinTraitType("mm_skill_on_brew_drink")
class MMSkillOnBrewDrinkAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    private var skillName = config.getString("SkillName","vintrait_$name")!!

    override var executeFunction : (TraitOwner) -> Boolean = Function@ { traitOwner ->
        if (canExecute(traitOwner) && skillName.isNotEmpty()) {
            val skillMetadata = MMAbilityData.prepareMMSkillData(traitOwner.player)
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
        const val NAME = "mm_skill_on_brew_drink"
        private val actionsMap = hashMapOf<String, MMSkillOnBrewDrinkAction>()
        private val listener = object : Listener {

            @EventHandler
            fun onBrewDrink(event: PlayerItemConsumeEvent) {
                if (event.isCancelled) return
                if (!BreweryApi.isBrew(event.item)) return
                BreweryApi.getBrew(event.item)?.let { brew ->
                    val traitOwner = TraitManager.instance.traitOwners[event.player.uniqueId] ?: return
                    val commonTraits = traitOwner.traits intersect actionsMap.keys
                    if (commonTraits.isEmpty()) return
                    for (traitName in commonTraits) {
                        actionsMap[traitName]?.let { action ->
                            TraitManager.instance.executeAction(traitName,traitOwner) Function@ { traitOwner ->
                                if (action.canExecute(traitOwner) && action.skillName.isNotEmpty()) {
                                    val skillMetadata = MMAbilityData.prepareMMSkillData(traitOwner.player)
                                    skillMetadata.variables.putInt("brew_alcohol", brew.calcAlcohol())
                                    return@Function MMAbilityData.executeMMSkill(action.skillName, skillMetadata)
                                }
                                return@Function false
                            }
                        }
                    }
                }
            }
        }
    }
}