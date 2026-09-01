package org.saintqd.vineriumtraits.traits

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.EntityType
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.saintqd.vineriumlib.utils.VinUtils
import org.saintqd.vineriumtraits.VineriumTraits
import org.saintqd.vineriumtraits.annotations.VinTraitType
import org.saintqd.vineriumtraits.managers.TraitManager
import java.util.UUID

@VinTraitType("cancel_entity_target_event")
class CancelEntityTargetAction(name : String, config : ConfigurationSection) : TraitAction(name,config) {

    var entityList = hashSetOf<EntityType>()
    var rememberTime = config.getInt("RememberTime",1200)

    val attackers = hashMapOf<UUID, Long>()

    init {
        config.getStringList("Types").forEach { entityName ->
            val event = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENTITY_TYPE).getOrThrow(Key.key(entityName.lowercase()))
            entityList.add(event)
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
        const val NAME = "cancel_entity_target_event"

        private val actionsMap = hashMapOf<String, CancelEntityTargetAction>()

        private val listener = object : Listener {

            @EventHandler
            fun onEntityTarget(event: EntityTargetLivingEntityEvent) {
                event.target?.let { target ->
                    if (target is Player) {
                        val traitOwner = TraitManager.instance.traitOwners[target.uniqueId] ?: return
                        val commonTraits = traitOwner.traits intersect actionsMap.keys
                        if (commonTraits.isEmpty()) return
                        for (traitName in commonTraits) {
                            actionsMap[traitName]?.let { action ->
                                if (action.entityList.contains(event.entity.type) && TraitManager.instance.executeAction(traitName, traitOwner)) {
                                    if (action.rememberTime > 0 && action.attackers.containsKey(target.uniqueId)
                                        && VinUtils.getCurrentTick() < (action.attackers[target.uniqueId]!! + action.rememberTime)) return
                                    action.attackers.remove(target.uniqueId)
                                    if (event.entity is Mob) {
                                        val mob = event.entity as Mob
                                        mob.target = null
                                    }
                                    event.isCancelled = true
                                }
                            }
                        }
                    }
                }
            }

            @EventHandler
            fun onEntityDamage(event : EntityDamageByEntityEvent) {
                val damager = event.damageSource.causingEntity
                if (damager is Player) {
                    val traitOwner = TraitManager.instance.traitOwners[damager.uniqueId] ?: return
                    val commonTraits = traitOwner.traits intersect actionsMap.keys
                    if (commonTraits.isEmpty()) return
                    for (traitName in commonTraits) {
                        actionsMap[traitName]?.let { action ->
                            if (action.entityList.contains(event.entity.type) && action.rememberTime > 0) {
                                action.attackers[damager.uniqueId] = VinUtils.getCurrentTick()
                            }
                        }
                    }
                }
            }
        }
    }
}