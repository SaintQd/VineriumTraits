package org.saintqd.vineriumtraits.traits

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.bukkit.BukkitAdapter
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.managers.TraitManager
import org.saintqd.vineriumlib.utils.MMAbilityData
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitOwner
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

@VinTraitType("mm_skill_on_damaged")
class MMSkillOnDamagedAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    private var skillName = config.getString("SkillName","vintrait_$name")!!
    val shouldBeCritical = config.getBoolean("Critical",false)
    val chance = config.getDouble("Chance",1.0)

    override var executeFunction : (TraitOwner) -> Boolean = Function@ { traitOwner ->
        if (canExecute(traitOwner) && skillName.isNotEmpty()) {
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
        const val NAME = "mm_skill_on_damaged"
        private val actionsMap = hashMapOf<String, MMSkillOnDamagedAction>()
        val entityTargets = hashMapOf<UUID, AbstractEntity>()
        private val listener = object : Listener {

            @EventHandler
            fun onPlayerDamaged(event: EntityDamageByEntityEvent) {
                if (event.isCancelled) return
                val entity = event.entity
                val damager = event.damageSource.causingEntity

                if (entity is Player) {
                    val traitOwner = TraitManager.instance.traitOwners[entity.uniqueId] ?: return
                    val commonTraits = traitOwner.traits intersect actionsMap.keys
                    if (commonTraits.isEmpty()) return
                    for (traitName in commonTraits) {
                        val abstractEntity = if (damager != null) BukkitAdapter.adapt(damager) else null
                        actionsMap[traitName]?.let { action ->
                            if (action.chance < 1.0 && action.chance > 0.0) {
                                if (ThreadLocalRandom.current().nextDouble() > action.chance)
                                    continue
                            }
                            if (action.shouldBeCritical && !event.isCritical)
                                continue
                            TraitManager.instance.executeAction(traitName,traitOwner) Function@ { traitOwner ->
                                if (action.canExecute(traitOwner) && action.skillName.isNotEmpty()) {
                                    val skillMetadata = MMAbilityData.prepareMMSkillData(traitOwner.player)
                                    if (abstractEntity != null) {
                                        skillMetadata.setEntityTarget(abstractEntity)
                                    }
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