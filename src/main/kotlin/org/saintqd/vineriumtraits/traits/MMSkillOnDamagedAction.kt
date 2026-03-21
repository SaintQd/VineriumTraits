package org.saintqd.vineriumtraits.traits

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.bukkit.BukkitAdapter
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.managers.TraitManager
import org.saintqd.vineriumtraits.utils.MMAbilityData
import java.util.UUID

class MMSkillOnDamagedAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    private var skillName = config.getString("Skill","none")!!

    override var executeFunction : (TraitOwner) -> Boolean = Function@ { traitOwner ->
        if (canExecute(traitOwner)) {
            val skillMetadata = MMAbilityData.prepareMMSkillData(traitOwner.player)
            entityTargets[traitOwner.player.uniqueId]?.let { target ->
                skillMetadata.setEntityTarget(target)
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
        const val NAME = "mm_skill_on_attack"
        private val actionsMap = hashMapOf<String, MMSkillOnDamagedAction>()
        val entityTargets = hashMapOf<UUID, AbstractEntity>()
        private val listener = object : Listener {

            @EventHandler
            fun onPlayerAttack(event: EntityDamageByEntityEvent) {
                if (event.isCancelled) return
                val traitOwner = TraitManager.instance.traitOwners[event.entity.uniqueId] ?: return
                val damager = event.damager
                if (damager !is LivingEntity) return
                for (actionName in actionsMap.keys) {
                    val trait = actionsMap[actionName]
                    if (trait != null) {
                        val abstractEntity = BukkitAdapter.adapt(damager)
                        entityTargets[traitOwner.player.uniqueId] = abstractEntity
                        TraitManager.instance.executeAction(actionName, traitOwner)
                        entityTargets.remove(traitOwner.player.uniqueId)
                    }
                }
            }
        }
    }
}