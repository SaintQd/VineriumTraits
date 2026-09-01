package org.saintqd.vineriumtraits.traits

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.bukkit.BukkitAdapter
import org.bukkit.Bukkit
import org.bukkit.attribute.Attributable
import org.bukkit.attribute.Attribute
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Damageable
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.managers.TraitManager
import org.saintqd.vineriumlib.utils.MMAbilityData
import org.saintqd.vineriumtraits.annotations.VinTraitType
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

@VinTraitType("mm_skill_on_kill")
class MMSkillOnKillAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    private var skillName = config.getString("SkillName","vintrait_$name")!!
    val chance = config.getDouble("chance",1.0)

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
        const val NAME = "mm_skill_on_kill"
        private val actionsMap = hashMapOf<String, MMSkillOnKillAction>()
        private val listener = object : Listener {

            @EventHandler(priority = EventPriority.HIGH)
            fun onPlayerAttack(event: EntityDamageByEntityEvent) {
                if (event.isCancelled) return
                val damager = event.damageSource.causingEntity
                if (damager !is Player) return
                val traitOwner = TraitManager.instance.traitOwners[damager.uniqueId] ?: return
                val entity = event.entity
                if (entity !is Damageable) return

                val commonTraits = traitOwner.traits intersect actionsMap.keys
                if (commonTraits.isEmpty()) return
                if (event.finalDamage >= entity.health) {
                    val abstractEntity = BukkitAdapter.adapt(entity)
                    for (traitName in commonTraits) {
                        actionsMap[traitName]?.let { action ->
                            if (action.chance < 1.0 && action.chance > 0.0) {
                                if (ThreadLocalRandom.current().nextDouble() > action.chance)
                                    continue
                            }
                            TraitManager.instance.executeAction(traitName,traitOwner) Function@ { traitOwner ->
                                if (action.canExecute(traitOwner) && action.skillName.isNotEmpty()) {
                                    val skillMetadata = MMAbilityData.prepareMMSkillData(traitOwner.player)
                                    skillMetadata.setEntityTarget(abstractEntity)
                                    if (entity is Attributable)
                                        skillMetadata.variables.putDouble("entity_max_health",entity.getAttribute(Attribute.MAX_HEALTH)!!.value)
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