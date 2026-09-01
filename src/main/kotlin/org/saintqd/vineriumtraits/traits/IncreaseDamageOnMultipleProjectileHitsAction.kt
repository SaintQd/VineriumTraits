package org.saintqd.vineriumtraits.traits

import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitManager
import java.util.UUID

@VinTraitType("increase_damage_on_multiple_projectile_hits")
class IncreaseDamageOnMultipleProjectileHitsAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    val coefPerHit = config.getDouble("CoefPerHit",0.2)
    val maxCoef = config.getDouble("MaxCoef",1.0)
    val allowedProjectileEntityTypes = config.getStringList("EntityTypes")

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
        private val actionsMap = hashMapOf<String, IncreaseDamageOnMultipleProjectileHitsAction>()

        private val playerTargets = hashMapOf<UUID, UUID>()
        private val projectiles = hashMapOf<UUID, Double>()

        private val listener = object : Listener {

            @EventHandler
            fun onProjectileHit(event: ProjectileHitEvent) {
                if (event.hitBlock != null) {
                    projectiles.remove(event.entity.uniqueId)
                    return
                }
            }

            @EventHandler
            fun onProjectileHitEntity(event: EntityDamageByEntityEvent) {
                if (event.isCancelled) return
                val entity = event.entity

                if (event.damager !is Projectile) {
                    return
                }
                val projectile = event.damager as Projectile

                if (entity !is LivingEntity) {
                    projectiles.remove(event.entity.uniqueId)
                    return
                }
                val damager = event.damageSource.causingEntity
                if (damager !is Player) {
                    return
                }

                val traitOwner = TraitManager.instance.traitOwners[damager.uniqueId] ?: return
                val commonTraits = traitOwner.traits intersect actionsMap.keys
                if (commonTraits.isEmpty()) return
                for (traitName in commonTraits) {
                    actionsMap[traitName]?.let { action ->

                        if (TraitManager.instance.executeAction(traitName,traitOwner)) {

                            if (action.allowedProjectileEntityTypes.isNotEmpty() && projectile.type.name !in action.allowedProjectileEntityTypes)
                                continue

                            val previousTarget = playerTargets[damager.uniqueId]
                            playerTargets[damager.uniqueId] = entity.uniqueId

                            var coef = projectiles[damager.uniqueId] ?: 0.0

                            if (previousTarget != null && entity.uniqueId == previousTarget) {
                                coef = (coef + action.coefPerHit).coerceAtMost(action.maxCoef)
                                projectiles[damager.uniqueId] = coef

                                val damage = event.damage * (1 + coef)
                                event.damage = damage
                            }
                            else
                                projectiles.remove(damager.uniqueId)
                        }
                    }
                }
            }
        }
    }
}