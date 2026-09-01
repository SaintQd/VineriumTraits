package org.saintqd.vineriumtraits.traits

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.damage.DamageType
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.managers.TraitManager
import org.saintqd.vineriumlib.utils.MMAbilityData
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitOwner

@VinTraitType("damage_type_coef")
class OnDamageTypeCoefAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    var damageTypes = hashMapOf<DamageType, Double>()
    val mmSkillOnTriggerName = config.getString("MMSkillOnTriggerName","")!!

    override var executeFunction : (TraitOwner) -> Boolean = Function@ { traitOwner ->
        if (canExecute(traitOwner)) {
            if (mmSkillOnTriggerName.isNotEmpty()) {
                val skillMetadata = MMAbilityData.prepareMMSkillData(traitOwner.player)
                MMAbilityData.executeMMSkill(mmSkillOnTriggerName,skillMetadata)
            }
            return@Function true
        }
        else
            return@Function false
    }

    init {
        config.getConfigurationSection("DamageTypes")?.getKeys(false)?.forEach { typeName ->
            val type = RegistryAccess.registryAccess().getRegistry(RegistryKey.DAMAGE_TYPE).getOrThrow(Key.key(typeName.lowercase()))
            val coef = config.getDouble("DamageTypes.$typeName",1.0)
            damageTypes[type] = coef
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
        const val NAME = "damage_type_coef"

        private val actionsMap = hashMapOf<String, OnDamageTypeCoefAction>()
        private val listener = object : Listener {

            @EventHandler
            fun onEntityDamage(event : EntityDamageEvent) {
                val traitOwner = TraitManager.instance.traitOwners[event.entity.uniqueId] ?: return
                val commonTraits = traitOwner.traits intersect actionsMap.keys
                if (commonTraits.isEmpty()) return
                for (traitName in commonTraits) {
                    actionsMap[traitName]?.let { action ->
                        action.damageTypes[event.damageSource.damageType]?.let { coef ->
                            if (TraitManager.instance.executeAction(traitName, traitOwner)) {
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