package org.saintqd.vineriumtraits.traits

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByBlockEvent
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitManager

@VinTraitType("damaged_by_block_coef")
class OnDamagedByBlockCoefAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    var blockList = hashMapOf<Material, Double>()

    init {
        config.getConfigurationSection("BlockList")?.getKeys(false)?.forEach { materialName ->
            val material = Material.valueOf(materialName.uppercase())
            val coef = config.getDouble("BlockList.$materialName",1.0)
            blockList[material] = coef
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
        const val NAME = "damaged_by_block_coef"

        private val actionsMap = hashMapOf<String, OnDamagedByBlockCoefAction>()
        private val listener = object : Listener {

            @EventHandler
            fun onEntityDamage(event : EntityDamageByBlockEvent) {
                event.damager?.let { block ->
                    val traitOwner = TraitManager.instance.traitOwners[event.entity.uniqueId] ?: return
                    val commonTraits = traitOwner.traits intersect actionsMap.keys
                    if (commonTraits.isEmpty()) return
                    for (traitName in commonTraits) {
                        actionsMap[traitName]?.let { action ->
                            val blockList = action.blockList
                            blockList[block.type]?.let { coef ->
                                val changedDamage = event.damage * coef
                                if (changedDamage <= 0)
                                    event.isCancelled = true
                                else
                                    event.damage = changedDamage
                            }
                        }
                    }
                }
            }
        }
    }
}