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
import org.saintqd.vineriumlib.utils.VinUtils
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.managers.TraitManager
import org.saintqd.vineriumlib.utils.MMAbilityData
import org.saintqd.vineriumtraits.annotations.VinTraitType
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import kotlin.collections.set

@VinTraitType("mm_skill_on_attack")
class MMSkillOnAttackAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    private var skillName = config.getString("SkillName","vintrait_$name")!!
    val shouldBeCritical = config.getBoolean("Critical",false)
    val chance = config.getDouble("Chance",1.0)

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
        private val actionsMap = hashMapOf<String, MMSkillOnAttackAction>()
        val entityTargets = hashMapOf<UUID, AbstractEntity>()
        private val listener = object : Listener {

            @EventHandler
            fun onPlayerAttack(event: EntityDamageByEntityEvent) {
                if (event.isCancelled) return
                val entity = event.entity
                if (entity !is LivingEntity) return
                val damager = event.damageSource.causingEntity

                if (damager is Player) {
                    val traitOwner = TraitManager.instance.traitOwners[damager.uniqueId] ?: return
                    val commonTraits = traitOwner.traits intersect actionsMap.keys
                    if (commonTraits.isEmpty()) return
                    for (traitName in commonTraits) {
                        val abstractEntity = BukkitAdapter.adapt(entity)
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
                                    skillMetadata.setEntityTarget(abstractEntity)
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